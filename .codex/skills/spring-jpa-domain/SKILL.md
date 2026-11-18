---
name: spring-jpa-domain
description: "Implement the persistence and business-logic layer of the TaekBaeWatShong server — JPA entities, state-transition methods, Spring Data repository queries, transaction boundaries, dirty-checking mutations, N+1 avoidance, and domain events. Use this skill whenever writing or changing an @Entity, a JpaRepository interface, a derived query name, a @Query, or a @Service method that reads or mutates domain state; whenever a list endpoint is slow or issues too many queries; and whenever adding a new aggregate to domain/. Covers the ddl-auto:update schema constraints that make some column changes unsafe on this project."
---

# JPA Domain & Service Implementation

How to build the entity → repository → service stack on this server. Read `spring-kotlin-conventions` first for style; this skill covers behavior and query design.

## Schema changes under `ddl-auto: update`

`application.yml` sets `spring.jpa.hibernate.ddl-auto: update`, and there is no Flyway or Liquibase. Hibernate reconciles the schema at startup against a live MySQL database. This is the single most dangerous constraint in the project, because `update` is **additive only**:

| Change | What Hibernate does |
|--------|--------------------|
| Add a nullable column | Adds it. Safe. |
| Add a `nullable = false` column to a table with rows | Fails at startup, or adds an implicit default silently, depending on MySQL mode. **Not safe.** |
| Rename a property | Adds a new column, leaves the old one populated and orphaned. Data appears lost. |
| Change a column type or widen a constraint | Usually ignored. The schema and the mapping silently diverge. |
| Remove a property | Leaves the column in place, still `NOT NULL` if it was. Inserts then fail. |
| Add a `unique = true` constraint | Fails if existing rows violate it. |

Therefore:

- **New non-null columns get a default and are added as nullable first,** or the change needs a manual migration the user runs. Say which you are doing.
- **Renames, type changes, and removals require the user's involvement.** Do not perform one silently and report success — the build passing tells you nothing, because `contextLoads()` may not even run against the production schema. State plainly what manual SQL is needed.
- Adding a whole new `@Entity` is safe: Hibernate creates the table.

When a change is not safely expressible under `ddl-auto: update`, flag it in your output file and to the QA inspector rather than working around it.

## Entity behavior methods

State lives in the entity, and transitions are methods on it. The reason is visible in `Parcel`:

```kotlin
fun markAsArrived(assignedZone: Zone? = null) {
    this.status = ParcelStatus.ARRIVED
    this.arrivedAt = LocalDateTime.now()
    assignedZone?.let { this.zone = it }
}

fun markAsClaimed() {
    this.status = ParcelStatus.CLAIMED
    this.claimedAt = LocalDateTime.now()
}
```

Each method sets a status *and* the timestamp that must accompany it. If a service assigned `parcel.status = ARRIVED` directly, `arrivedAt` would stay null and `unclaimedDays` — which reads both — would silently return 0 forever. Bundling them makes the invalid intermediate state unreachable.

When adding a transition:

1. Name it for the business event (`markAsArrived`), not the field (`setStatus`).
2. Set every field the new state implies, in one method.
3. Decide whether it should reject an invalid source state. `Parcel`'s methods currently do not — `markAsClaimed()` works on a `PENDING` parcel. If the contract says that should fail, add the guard in the **service** (so it can throw a typed `ParcelException`) rather than in the entity (which would have to throw an untyped error). Note the choice.
4. Derived reads stay computed properties: `unclaimedDays` recalculates from `arrivedAt` and today. Never store a derived value.

## Repository query design

Prefer derived query names. They are verified against the entity metamodel at startup, so a typo fails fast; a JPQL string typo fails at first call.

Existing names are long on purpose:

```kotlin
fun findAllByOwnerAndStatusAndClaimedAtGreaterThanEqualOrderByCreatedAtDesc(
    owner: User, status: ParcelStatus, claimedAt: LocalDateTime
): List<Parcel>
```

Drop to `@Query` when the derived name would exceed roughly that length or when the query needs a construct derived names cannot express (joins with fetch, aggregates, subqueries). Use triple-quoted JPQL and `@Param`:

```kotlin
@Query("""
    SELECT p FROM Parcel p
    WHERE p.status = :status
      AND p.claimedAt >= :threeDaysAgo
    ORDER BY p.createdAt DESC
""")
fun findClaimedParcelsWithinThreeDays(
    @Param("status") status: ParcelStatus = ParcelStatus.CLAIMED,
    @Param("threeDaysAgo") threeDaysAgo: LocalDateTime
): List<Parcel>
```

**Kotlin default values do not work as query parameter defaults through the Spring Data proxy.** In the call above, `ParcelService` passes `threeDaysAgo` by name and relies on the `status` default — that works only because Kotlin resolves the default at the *call site*, not in the proxy. It is fragile: a Java caller or a reflective invocation gets null. Prefer passing every `@Param` explicitly from the service and dropping the default.

Every list query ends with an explicit `OrderBy` / `ORDER BY`. Without one, MySQL's row order is unspecified and a list endpoint returns rows in a different order between calls. `createdAt DESC` is the house default.

## N+1 queries

This is the most likely performance defect here, because every list response maps through a `from()` factory that touches a lazy association:

```kotlin
ownerName = parcel.owner.name    // ParcelResponse.from — one extra SELECT per parcel
```

`Parcel.owner` is `FetchType.LAZY` (correct), so `findAllByOrderByCreatedAtDesc()` returns N parcels and the mapping then issues N more queries for users. With `show-sql: true` in `application.yml` this is directly observable — run the endpoint and count the `select` lines.

Fix it at the repository, not the mapper, with a fetch join:

```kotlin
@Query("SELECT p FROM Parcel p JOIN FETCH p.owner ORDER BY p.createdAt DESC")
fun findAllWithOwnerOrderByCreatedAtDesc(): List<Parcel>
```

Rules for fetch joins:

- Only for **collection-returning queries whose DTO reads the association.** A single-entity lookup does not need one.
- `JOIN FETCH` on a `@ManyToOne` is always safe. `JOIN FETCH` on a to-many collection plus pagination makes Hibernate load everything into memory and paginate in Java — avoid that combination.
- Never "fix" N+1 by changing the association to `FetchType.EAGER`. That trades one N+1 for an unconditional join on every single-entity read, including ones that never touch the association.

Before adding a fetch join, confirm the DTO actually reads the association — `ParcelZoneGroupResponse.ParcelSimpleDetail` reads `parcel.owner.name`, so `findAllByStatusAndZoneIn` needs one; a query whose DTO never dereferences `owner` does not.

## Service layer

```kotlin
@Service
@Transactional(readOnly = true)
class ParcelService(
    private val parcelRepository: ParcelRepository,
    private val apickTrackingService: ApickTrackingService,
    private val eventPublisher: ApplicationEventPublisher
) {
    @Transactional
    fun registerParcel(user: User, request: ParcelRegisterRequest): ParcelResponse {
        if (parcelRepository.existsByInvoiceNumber(request.invoiceNumber)) {
            throw ParcelException.Conflict("이미 등록된 운송장 번호입니다.")
        }
        ...
        return ParcelResponse.from(parcelRepository.save(parcel))
    }
}
```

Method structure, in order:

1. **Guard clauses first.** Existence, uniqueness, external validation, ownership — each throwing a typed exception before any state is touched. A guard placed after a mutation inside a transaction still rolls back, but it makes the method's behavior much harder to read.
2. **Load** via repository, converting "not found" immediately: `?: throw ParcelException.NotFound(...)` for nullable returns, `.orElseThrow { ... }` for `findById`.
3. **Mutate** through entity methods.
4. **Publish** any domain event.
5. **Return** `<Response>.from(entity)`.

### Transactions

- Class-level `@Transactional(readOnly = true)`; `@Transactional` on each write method. A write method missing the override runs read-only and its changes are silently discarded at commit — no exception, no log. This is the single easiest bug to introduce in this layer.
- Import from `org.springframework.transaction.annotation`.
- **Self-invocation does not open a transaction.** Spring proxies the bean, so a `@Transactional` method called from another method of the same class runs with the caller's transaction settings. Do not extract a helper and annotate it.
- **Do not call external HTTP inside a write transaction if you can avoid it.** `registerParcel` calls `apickTrackingService.validateInvoice(...)` inside `@Transactional`, holding a database connection for the full duration of a remote call with no configured timeout. It works, but it is a known weak point: if the Apick API hangs, connections pile up. Prefer validating before the transactional boundary in new code, and mention this when touching `registerParcel`.

### Mutations and `save()`

Inside a transaction, a loaded entity is managed and flushes at commit:

```kotlin
val parcel = parcelRepository.findByInvoiceNumber(...) ?: throw ...
parcel.markAsArrived(request.zone)     // no save() — dirty checking persists it
```

Call `save()` only for entities you constructed. Adding a redundant `save()` on a managed entity is harmless but is not the house style.

## Domain events

```kotlin
eventPublisher.publishEvent(ParcelArrivedEvent(parcel))
```

Event classes live in the same `service/` package as the publisher. Listeners go in `<Aggregate>EventListener.kt`.

Default `@EventListener` is **synchronous and joins the publishing transaction** — a listener that throws rolls back the publisher's write. That is usually not what an event is for. If the listener does something that must not fail the main operation (notification, logging, an outbound call), use `@TransactionalEventListener(phase = AFTER_COMMIT)`. Say which semantics you chose and why.

Do not add `@EnableAsync`/`@Async` to work around a slow listener without raising it — that changes threading and transaction propagation project-wide.

## Adding a new aggregate

1. `domain/<name>/entity/<Name>.kt` — plus status/other enums in the same package.
2. `domain/<name>/repository/<Name>Repository.kt` — `JpaRepository<Name, Long>`.
3. `global/exception/<Name>Exception.kt` — sealed, nested subclasses, `@ResponseStatus` + matching `status`.
4. `domain/<name>/service/<Name>Service.kt` — `@Transactional(readOnly = true)` at class level.
5. DTOs under `dto/request/` and `dto/response/` (the `parcel` layout, not `user`'s flat one).
6. Controller last, from the contract.

A new `@Entity` creates its table safely under `ddl-auto: update`, so a new aggregate needs no migration — unlike a change to an existing one.

## Verification

```bash
./gradlew compileKotlin     # catches derived-query-name and mapping errors only at startup, not here
./gradlew test              # boots the context; this is what validates repository method names
```

Derived query names are validated when Spring Data builds the repository proxy at **context startup**, not at compile time. So `compileKotlin` succeeding proves nothing about a query name — only a context-loading test does, and that test needs a live MySQL and the full env var set. If you cannot boot the context, verify derived names by hand against the entity's property names and say that verification was manual.

## Language rule

**Skill and agent definition files are written in English. Everything produced for a human is written in Korean** — every response to the user, workspace notes and reports meant for the team, commit messages (after the English type prefix), PR bodies, review comments, and thread replies. Code identifiers, package names, and log output stay English; user-facing exception messages stay Korean.
