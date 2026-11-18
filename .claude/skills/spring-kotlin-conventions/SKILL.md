---
name: spring-kotlin-conventions
description: "Baseline coding conventions for the TaekBaeWatShong Spring Boot 3.2.5 / Kotlin 1.9.23 / Java 21 server — package layout, entity style, DTO factories, sealed exception hierarchies, transaction boundaries, naming, and Korean-facing message rules. Load this skill BEFORE writing or reviewing ANY Kotlin file in this repository: controllers, services, entities, repositories, DTOs, config, or security classes. Every harness agent reads this first so all generated code looks like it was written by the same person. Also use it when reviewing a diff for style drift, when adding a new domain package, or when asked 'how do we do X in this codebase'."
---

# TaekBaeWatShong Server Conventions

Shared baseline for every agent that touches Kotlin code in this repository. Read it before writing code, not after.

The rules below are extracted from the existing `parcel` and `user` domains. The goal is that a reviewer cannot tell which file is new. Consistency matters more here than any individual improvement, because this project has no global exception handler and no validation layer — the patterns *are* the contract.

## Stack facts

| Item | Value |
|------|-------|
| Kotlin | 1.9.23, `-Xjsr305=strict` |
| Spring Boot | 3.2.5 |
| Java toolchain | 21 |
| Persistence | Spring Data JPA + MySQL, `ddl-auto: update` |
| Security | Spring Security + OAuth2 client + JJWT 0.12.6 |
| Build | Gradle Kotlin DSL, wrapper at `./gradlew` |
| Tests | JUnit 5, `spring-boot-starter-test`, `spring-security-test`, `kotlin-test-junit5` |

`allOpen` is configured for `@Entity`, `@MappedSuperclass`, `@Embeddable`. That is why entities are plain `class` (not `data class`) and still work with JPA proxies. Do not add `open` modifiers by hand, and do not turn an entity into a `data class`.

**Not on the classpath — do not use:** bean validation (`jakarta.validation`, `@Valid`, `@NotBlank`), springdoc/Swagger, H2, Testcontainers, ktlint/detekt/spotless. If you think one is needed, propose it to the user rather than adding a half-wired dependency.

## Package layout

```
com.example.taekbaewatshongserver
├── domain/<aggregate>/
│   ├── controller/     <Aggregate>Controller.kt
│   ├── dto/
│   │   ├── request/    <Action>Request.kt
│   │   └── response/   <Action>Response.kt
│   ├── entity/         <Aggregate>.kt, status/other enums
│   ├── repository/     <Aggregate>Repository.kt
│   └── service/        <Aggregate>Service.kt, external clients, events
└── global/
    ├── config/         SecurityConfig.kt
    ├── exception/      <Aggregate>Exception.kt
    └── security/       UserPrincipal.kt, jwt/, oauth/, email/
```

Two aggregates exist: `parcel` and `user`. A new aggregate gets the same five subpackages. Cross-aggregate code goes in `global/`.

Two known inconsistencies — follow `parcel`, do not retrofit `user` unless asked:

- `parcel` splits DTOs into `dto/request/` and `dto/response/`; `user` keeps them flat in `dto/`. **Use the split layout for new aggregates.**
- Email/password auth lives in `global/security/email/` rather than under `domain/user/`. Leave it there; moving it is a separate, user-approved refactor.

## Entities

Mutable classes with constructor properties and behavior methods. Reference `domain/parcel/entity/Parcel.kt`.

```kotlin
@Entity
@Table(name = "parcels")           // plural, snake_case table name
class Parcel(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @Column(nullable = false, unique = true)
    val invoiceNumber: String,     // val for identity / immutable facts

    var alias: String,             // var for genuinely mutable state

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val owner: User,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: ParcelStatus = ParcelStatus.PENDING,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
```

- **`val` vs `var` carries meaning.** `val` for anything that must never change after creation (id, invoice number, owner, createdAt). This is the cheapest available substitute for the invariants a validation layer would otherwise enforce.
- **Every `@ManyToOne` is `FetchType.LAZY`.** JPA defaults `@ManyToOne` to EAGER, which silently produces N+1 queries the moment a list endpoint is added.
- **Enums persist as `EnumType.STRING`,** never ordinal — ordinals break when someone reorders an enum constant.
- **State changes go through named methods on the entity,** not setters called from a service. `Parcel` exposes `markAsArrived()`, `updateZone()`, `markAsClaimed()`; each sets the status *and* its paired timestamp together so the two can never drift. Add new transitions the same way instead of assigning `parcel.status = ...` from a service.
- **Derived values are computed properties,** not stored columns — see `Parcel.unclaimedDays`. Stored duplicates of derivable state go stale.
- Id conventions differ: `Parcel.id` is `Long = 0L`, `User.id` is `Long? = null`. Match the aggregate you are extending, and remember the difference when comparing ids across aggregates (`parcel.owner.id != user.id` compares two `Long?`).
- There is no auditing base class and no `@CreatedDate`. Timestamps are plain properties defaulted with `LocalDateTime.now()`.

## Enums

Domain enums live in `entity/`. Give them a `description` when the value is shown to a human:

```kotlin
enum class Zone(val description: String) {
    A("1번 선반 - 1단 (상단)"),
    ...
}
```

Plain state enums carry a trailing Korean comment instead (`ParcelStatus`: `PENDING`, `ARRIVED`, `CLAIMED`). Pick by whether the label needs to reach the client.

Existing enums: `ParcelStatus`, `Zone` (A–F), `Role` (`STUDENT`, `TEACHER`, `ADMIN`), `AuthProvider` (`GOOGLE`, `LOCAL`).

## DTOs

Requests are plain `data class` holders with defaults for optional fields:

```kotlin
data class ParcelRegisterRequest(
    val deliveryCompany: String,
    val invoiceNumber: String,
    val alias: String? = null
)
```

Responses are `data class` with a `companion object { fun from(entity): Response }` factory.

- **Entities never cross the controller boundary.** Every response is a DTO built by `from()`. This also keeps lazy associations from being serialized outside a transaction, which would throw `LazyInitializationException`.
- **Mapping logic lives in `from()`,** not in the service. Services call `ParcelResponse.from(parcel)` and return; they do not hand-assemble DTOs.
- **List responses are wrapped,** never bare arrays: `ParcelListResponse(val parcels: List<ParcelResponse>)`. A wrapped object can gain fields (counts, paging) without breaking clients.
- Nested detail shapes go inside the response as nested classes — see `ParcelZoneGroupResponse.ZoneGroupDetail` and `.ParcelSimpleDetail`.
- All DTO properties are `val`. No annotations on request DTOs.
- Enums are serialized by name; `UserResponse` maps `role` to `role.name` (a `String`) while `ParcelResponse` exposes the `ParcelStatus`/`Zone` enum directly. Prefer exposing the enum — the client gets the same JSON and Kotlin keeps the type.

## Controllers

```kotlin
@RestController
@RequestMapping("/parcel")          // singular, no /api prefix, no version segment
class ParcelController(
    private val parcelService: ParcelService
) {
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PatchMapping("/{parcelId}/zone")
    fun assignZone(
        @PathVariable parcelId: Long,
        @RequestBody request: ParcelZoneAssignRequest
    ): ResponseEntity<ParcelZoneAssignResponse> {
        val response = parcelService.assignZone(parcelId, request)
        return ResponseEntity.ok(response)
    }
}
```

- Constructor injection only. No `@Autowired` fields, no `lateinit var`.
- Controllers are **thin**: call one service method, wrap in `ResponseEntity`, return. No business logic, no repository access, no entity handling.
- Return `ResponseEntity<T>` with a concrete DTO type — never `ResponseEntity<Any>` or a raw `Map`.
- `ResponseEntity.status(HttpStatus.CREATED).body(...)` for creation; `ResponseEntity.ok(...)` otherwise.
- Base paths are **singular and unprefixed**: `/parcel`, `/auth`. Keep it that way for consistency. (`UserController` uses `/api/users/me`, which is the odd one out — do not copy it.)
- Optional filters: `@RequestParam(required = false) status: ParcelStatus?`. Spring binds the enum by name and returns 400 on an unknown value.
- HTTP verbs in use: `POST` creates, `GET` reads, `PATCH` mutates existing state (`/complete`, `/claim`, `/{id}/zone`). `PUT` and `DELETE` are not used anywhere.

### The `@AuthenticationPrincipal` type trap

**The principal type depends on how the caller authenticated, and the two paths disagree:**

| Path | Principal object |
|------|------------------|
| `JwtAuthenticationFilter` (every normal API call) | the raw `User` entity |
| OAuth2 login handlers | `UserPrincipal` (wraps `User`) |

`ParcelController` declares `@AuthenticationPrincipal userPrincipal: UserPrincipal` and reads `userPrincipal.user`; `UserController` declares `@AuthenticationPrincipal user: User`. Both compile, and Spring injects `null` rather than failing when the runtime type does not match — which surfaces as a `NullPointerException` deep in the service, not as a 401.

**For a normal JWT-authenticated endpoint, declare `@AuthenticationPrincipal user: User`.** If you follow `ParcelController` and declare `UserPrincipal`, verify against `JwtAuthenticationFilter` that the principal really is a `UserPrincipal` on that path. Flag this mismatch when you encounter it rather than silently picking one — it is a live inconsistency, and `spring-qa-verify` checks for it.

## Services

```kotlin
@Service
@Transactional(readOnly = true)     // class-level default
class ParcelService(
    private val parcelRepository: ParcelRepository,
    private val apickTrackingService: ApickTrackingService,
    private val eventPublisher: ApplicationEventPublisher
) {
    @Transactional                  // override on writes only
    fun registerParcel(user: User, request: ParcelRegisterRequest): ParcelResponse { ... }
}
```

- **`@Transactional(readOnly = true)` at class level, `@Transactional` on write methods.** Read-only transactions let Hibernate skip dirty checking; forgetting the override on a write means the change is silently not flushed.
- Import `org.springframework.transaction.annotation.Transactional` (Spring), not `jakarta.transaction`.
- **Mutations rely on dirty checking inside `@Transactional`.** `completeParcelScan` calls `parcel.markAsArrived(...)` and never calls `save()` — the managed entity flushes at commit. Only call `save()` for genuinely new entities.
- Services return **DTOs**, not entities.
- Validate preconditions at the top of the method, before touching state, and throw a typed exception.
- Domain events go through `ApplicationEventPublisher.publishEvent(...)` with an event class in the same `service/` package (`ParcelArrivedEvent`).
- External integrations get their own `@Service` in `service/` (`ApickTrackingService`) so the domain service stays testable.

## Exceptions

Each aggregate owns one sealed class in `global/exception/`:

```kotlin
sealed class ParcelException(
    val status: HttpStatus,
    message: String
) : RuntimeException(message) {

    @ResponseStatus(HttpStatus.NOT_FOUND)
    class NotFound(
        message: String = "해당 택배를 찾을 수 없습니다."
    ) : ParcelException(HttpStatus.NOT_FOUND, message)
}
```

- Subclasses are nested inside the sealed parent, each with a `@ResponseStatus` **and** a matching `status` constructor argument. Both are needed today: `@ResponseStatus` is what actually produces the HTTP status, because **there is no `@RestControllerAdvice` in this project**. The `status` property exists for a future handler and must be kept in sync with the annotation — if they disagree, the annotation wins and the property silently lies.
- Every subclass has a Korean default message and accepts an override so call sites add context: `ParcelException.NotFound("해당 운송장 번호의 택배를 찾을 수 없습니다. (${request.invoiceNumber})")`.
- Use Spring's `AccessDeniedException` for authorization failures (403), as `claimParcel` does — do not invent an aggregate-specific forbidden type.
- `ParcelException` is currently the **only** custom exception class. Auth and email code throws `ResponseStatusException` inline. A new aggregate gets its own `<Aggregate>Exception.kt` in the sealed style; prefer that over `ResponseStatusException` for new domain code.
- Never throw bare `RuntimeException`, `IllegalArgumentException`, or `IllegalStateException` from a service.

**Adding a `@RestControllerAdvice` changes the behavior of every existing exception,** so treat it as a cross-cutting change: confirm with the user first, and make the handler read the `status` property rather than duplicating the mapping.

## Repositories

Interfaces extending `JpaRepository<Entity, Long>` in `repository/`. Prefer derived query names; drop to `@Query` (JPQL, triple-quoted) only when the derived name becomes unreadable.

Derived names here are long by design — `findAllByOwnerAndStatusAndClaimedAtGreaterThanEqualOrderByCreatedAtDesc`. That is accepted: an explicit name is compiler-checked, a hand-written string is not. See the `spring-jpa-domain` skill for query design and N+1 handling.

## Language and git

- **Identifiers, package names, log output: English.**
- **User-facing exception messages and enum descriptions: Korean.** They reach a Korean-language client.
- **Comments: Korean, and rare.** The codebase carries almost none, so a comment signals something genuinely non-obvious.
- **Commit messages: English type + Korean description, single line, no scope, no body.** Types in use: `feat`, `fix`, `refactor`, `chore`. Examples: `feat: 중복 운송장 등록 예외 처리 추가`, `refactor: 불필요한 도착 알림 이벤트 로직 삭제`.
- **Branches:** `feat/<kebab-topic>` or `fix/<kebab-topic>`, merged to `main` via PR in the `VOID-GSM` org. A `develop` branch also exists on origin.
- A Korean PR template lives at `.github/PULL_REQUEST_TEMPLATE.md` with a test checklist (단위 테스트 / API 동작 / 예외·권한 검증). Fill it out when opening a PR.

## Build and verification

```bash
./gradlew compileKotlin   # fast syntax + type check
./gradlew test            # tests only
./gradlew build           # compile + test
```

Run from the repository root. Use `./gradlew` (the shell script) from the Bash tool even on Windows.

**A green build here proves less than usual.** The only test is `TaekBaeWatShongServerApplicationTests.contextLoads()`, and it boots the full Spring context, so it needs a reachable MySQL plus `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` (≥32 bytes), `GOOGLE_CLIENT_ID`, and `GOOGLE_CLIENT_SECRET`. If the build fails on missing configuration rather than on your code, say so explicitly instead of reporting it as a code failure — and never report `compileKotlin` success as "tests pass".

## Language rule

**Skill and agent definition files are written in English. Everything produced for a human is written in Korean** — every response to the user, workspace notes and reports meant for the team, commit messages (after the English type prefix), PR bodies, review comments, and thread replies. Code identifiers, package names, and log output stay English; user-facing exception messages stay Korean.
