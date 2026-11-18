# Domain Model

The aggregates this server needs, what exists today, and the state machine that drives most of the product's behavior. Read [PRODUCT.md](PRODUCT.md) first for the feature context.

## Aggregates

| Aggregate | Package | Status |
|-----------|---------|--------|
| `parcel` | `domain/parcel/` | **exists** — the core of the product |
| `user` | `domain/user/` | **exists** — missing the student number |
| `report` (분실 신고) | — | **not built** |
| `chat` (1:1 채팅) | — | **not built** |
| `notification` (알림) | — | **not built** |
| `driver` (택배 기사 + QR) | — | **not built** |

Each new aggregate follows the `parcel` layout (`controller/`, `dto/request/`, `dto/response/`, `entity/`, `repository/`, `service/`) and gets a sealed exception class in `global/exception/`.

## Parcel — the central entity

`domain/parcel/entity/Parcel.kt`, table `parcels`.

| Field | Type | Notes |
|-------|------|-------|
| `id` | `Long = 0L` | identity |
| `deliveryCompany` | `String` | carrier, `val` |
| `invoiceNumber` | `String` | **unique**, `val` — the business key used by every scan path |
| `alias` | `String` | product name; defaults to the owner's name when the user omits it |
| `owner` | `User` | `@ManyToOne(LAZY)`, `val` |
| `zone` | `Zone?` | null until assigned |
| `status` | `ParcelStatus` | see state machine |
| `arrivedAt` | `LocalDateTime?` | set on arrival; drives the client count-up timer |
| `claimedAt` | `LocalDateTime?` | set on pickup; drives the 3-day retention filter |
| `createdAt` | `LocalDateTime` | `updatable = false` |
| `unclaimedDays` | computed `Int` | derived, not stored |

`invoiceNumber` being unique is what makes the whole scanning flow work: both the driver scanner and the client pickup scanner look a parcel up by invoice number alone, with no other identifier.

### State machine

```
        register              arrival scan            pickup scan
  ─────────────────▶ PENDING ──────────────▶ ARRIVED ─────────────▶ CLAIMED
                                   │                                   │
                            sets arrivedAt                      sets claimedAt
                            (+ optional zone)                   (3-day retention starts)
```

Transitions are entity methods, each setting the status and its paired timestamp together:

| Method | Effect |
|--------|--------|
| `markAsArrived(zone?)` | `status = ARRIVED`, `arrivedAt = now()`, optionally assigns the zone |
| `updateZone(zone)` | reassigns the zone without touching status |
| `markAsClaimed()` | `status = CLAIMED`, `claimedAt = now()` |

**No transition currently validates its source state.** `markAsClaimed()` succeeds on a `PENDING` parcel, and `markAsArrived()` succeeds on an already-`CLAIMED` one, overwriting `arrivedAt`. Whether that is acceptable is open product question #3 in PRODUCT.md. If guards are wanted, they belong in the service (which can throw a typed `ParcelException`), not the entity.

The spec's **delivery progress stage** (배송 진행단계) is a *second, finer* axis that this enum does not carry. `ParcelStatus` tracks the parcel's lifecycle **at the school**; the progress stage tracks it **in the carrier's network** (집화 → 간선상차 → 배송출발 …). They are not the same thing and should not be merged into one enum — a parcel can be "out for delivery" in the carrier's system and still `PENDING` here.

### Zone

`enum class Zone(val description: String)` — `A`–`F`, each carrying a Korean shelf-position label matching the physical layout in PRODUCT.md. Nullable on the parcel until an admin assigns it.

## User

`domain/user/entity/User.kt`, table `users`.

| Field | Type | Notes |
|-------|------|-------|
| `id` | `Long?` | **nullable**, unlike `Parcel.id` |
| `email` | `String` | unique |
| `name` | `String` | |
| `provider` | `AuthProvider` | `GOOGLE` or `LOCAL` |
| `providerId` | `String?` | Google `sub` |
| `password` | `String?` | null for Google users |
| `role` | `Role` | `STUDENT`, `TEACHER`, `ADMIN` |

**Missing for the spec: `studentNumber` (학번).** Required at student signup and displayed on every lost-report row. It must be nullable, because teachers and admins have none — the lost-report list renders "선생님" in its place. Adding it as a nullable column is safe under `ddl-auto: update`; adding it as non-null is not.

Note the id asymmetry: `User.id` is `Long?` while `Parcel.id` is `Long`. Ownership comparisons (`parcel.owner.id != user.id`) therefore compare two nullable Longs — two nulls compare equal, which would read as "same owner". Persisted entities are never null here, but do not replicate the pattern where either side might be transient.

## Aggregates still to build

### report — 분실 신고

Fields implied by the admin list: reporter (→ `User`), the parcel (→ `Parcel`, giving invoice and product name), expected time of loss, description, created time. The reporter's student number is read through `User`, not copied.

Only the parcel's owner may file a report on it — the same ownership rule as pickup.

Open: whether a report has a lifecycle status (received / investigating / resolved) or is a flat list. The brief shows a flat list.

### chat — 1:1 채팅

Both directions: a client can request a chat with an admin, and an admin can start one from a lost report. Needs a conversation entity plus messages, and a transport decision — polling, SSE, or WebSocket. **None of the three is configured in the current stack**, and the choice affects `SecurityConfig` (a WebSocket handshake authenticates differently from a Bearer-token REST call).

### notification — 알림

Triggered on: registration, arrival, zone assignment, pickup, and a recurring unclaimed reminder carrying the elapsed day count.

The recurring reminder is the part that needs real state. A notification record must remember what was already sent, or the reminder fires again on every scheduler tick. It also needs a scheduler — `@EnableScheduling` is **not** currently on the application class (only `@EnableAsync` is).

The event mechanism already exists in miniature: `ParcelArrivedEvent` is published by `ParcelService.completeParcelScan`, and `ParcelEventListener` handles it with `@Async @TransactionalEventListener(AFTER_COMMIT)` — correct semantics, since a failed notification must not roll back the arrival. It currently only calls `println`. That listener is the right seam to build real notifications onto.

### driver — 택배 기사 + QR

The brief lists drivers as Admin-app users, but they never log in with Google — they are identified by a generated QR code. So a driver is most likely **not** a `User` with a new role, but its own entity holding a name and a QR token, with parcels linked to the driver that delivered them.

This matters for security: the QR-recognition page shows a driver's full parcel list including orderer names. Whoever opens that page must be authenticated as an admin — the QR identifies the *driver*, it must not authenticate the *viewer*.

Open product question #5 in PRODUCT.md.

## Rules encoded in queries, not in the model

Two business rules live in `ParcelRepository` rather than in an entity, which makes them easy to miss:

**3-day pickup retention.** `findClaimedParcelsWithinThreeDays` and `findAllByOwnerAndStatusAndClaimedAtGreaterThanEqualOrderByCreatedAtDesc` filter `claimedAt >= now - 3 days`. Claimed parcels are **filtered, never deleted** — the row remains for audit. The 3-day window is computed in `ParcelService` (`LocalDateTime.now().minusDays(3)`) and passed in, so it appears in two service methods and is not a named constant. Changing the retention period means editing both.

**Neglect measurement.** `Parcel.unclaimedDays` uses `ChronoUnit.DAYS.between(arrivedAt.toLocalDate(), LocalDate.now())` — a **calendar-day** difference, not elapsed hours. A parcel arriving at 23:00 crosses to `1` an hour later. Under the spec's "more than 1 day" threshold, the real elapsed time before someone is marked a 방치자 therefore varies between roughly 24 and 48 hours depending on arrival time of day. This is open product question #2, and the answer determines whether this property stays calendar-based or moves to `ChronoUnit.HOURS`.

`unclaimedDays` also returns `0` for any parcel not in `ARRIVED` status — including `CLAIMED` ones. So it means "days neglected so far", not "days it took to collect". Do not reuse it for the latter.

## What the TOP 3 ranking needs

The unclaimed TOP 3 appears in both apps and does not exist yet. It is a **ranking**, not a filter: order `ARRIVED` parcels by `arrivedAt` ascending, take three, and expose rank, owner name, and elapsed time. Doing it in Kotlin after loading every arrived parcel works at current scale but sorts in memory; a repository-side `ORDER BY p.arrivedAt ASC` with a limit is the shape to write.

Because the response includes `owner.name`, the query needs `JOIN FETCH p.owner` or it issues an extra select per row.
