# Implementation Status — spec vs. code

Feature-by-feature map of [PRODUCT.md](PRODUCT.md) against the current `feat/parcel` branch. Verified against source on 2026-09-10.

Legend: **DONE** works as specified · **PARTIAL** exists but incomplete or diverging · **MISSING** no code

## Existing API surface

Everything currently implemented:

| Verb | Path | Access | Purpose |
|------|------|--------|---------|
| POST | `/parcel` | authenticated | register a parcel |
| GET | `/parcel` | ADMIN, TEACHER | all parcels, optional `?status=` |
| GET | `/parcel/me` | authenticated | my parcels, optional `?status=` |
| PATCH | `/parcel/complete` | ADMIN, TEACHER | arrival scan by invoice number |
| GET | `/parcel/zone` | ADMIN, TEACHER | parcels grouped by zone, optional `?zone=` |
| PATCH | `/parcel/{parcelId}/zone` | ADMIN, TEACHER | reassign zone |
| PATCH | `/parcel/claim` | authenticated + owner | pickup scan by invoice number |
| GET | `/api/users/me` | authenticated | current user |
| GET | `/auth/login?role=` | public | start Google OAuth (client) |
| GET | `/auth/admin/login` | public | start Google OAuth (admin) |
| GET | `/auth/token?code=` | public | exchange one-time code for JWT |
| POST | `/auth/email/{signup,login}` | public | email/password auth |
| POST | `/auth/email/admin/{signup,login}` | public | email/password auth (admin) |

## Client

| Feature | Status | Notes |
|---------|--------|-------|
| Google OAuth login | **DONE** | separate client/admin redirect URIs already configured |
| Signup: student vs. teacher choice | **PARTIAL** | `role` is passed on `/auth/login?role=`, but no name/number collection step |
| Signup: student number (학번) | **MISSING** | `User` has no `studentNumber` field |
| Register parcel (invoice + carrier + optional alias) | **DONE** | `POST /parcel`; alias falls back to `user.name` |
| My parcel list | **DONE** | `GET /parcel/me` |
| — invoice number | **DONE** | |
| — arrival state + `arrivedAt` | **DONE** | count-up is rendered client-side from `arrivedAt` |
| — product name | **DONE** | `alias` |
| — **delivery progress stage** | **MISSING** | `ParcelStatus` has only PENDING/ARRIVED/CLAIMED; carrier stages are a separate axis |
| Zone map with per-zone counts | **PARTIAL** | `GET /parcel/zone` returns counts and parcels; `ParcelZoneMapResponse` (image + labels) exists but **is never used by any controller or service** |
| Unclaimed TOP 3 | **MISSING** | `unclaimedDays` exists, but no ranking endpoint |
| Pickup by barcode scan | **DONE** | `PATCH /parcel/claim` by invoice number, with ownership check |
| Lost report | **MISSING** | no aggregate |
| 1:1 chat | **MISSING** | no aggregate, no transport configured |
| Notifications | **PARTIAL** | `ParcelArrivedEvent` + `ParcelEventListener` exist with correct async/after-commit semantics, but the handler only calls `println`. No other trigger, no delivery transport, no persistence |

## Admin

| Feature | Status | Notes |
|---------|--------|-------|
| Google OAuth login, admin allowlist | **DONE** | gated by `app.admin.emails` |
| All-parcel list split by state | **DONE** | `GET /parcel?status=` |
| — arrived: invoice, orderer, arrival time | **DONE** | `ParcelResponse` carries `ownerName` |
| — not arrived: invoice, orderer | **DONE** | |
| — picked up: invoice, orderer | **DONE** | |
| 3-day retention for picked-up parcels | **DONE** | `findClaimedParcelsWithinThreeDays`; filtered, not deleted |
| Parcels by zone | **DONE** | `GET /parcel/zone` |
| Zone assignment | **DONE** | `PATCH /parcel/{id}/zone`, and optionally during arrival scan |
| Unclaimed TOP 3 | **MISSING** | |
| Lost-report list | **MISSING** | |
| 1:1 chat (both directions) | **MISSING** | |
| Driver scan page (physical scanner) | **DONE** | `PATCH /parcel/complete` — a hardware scanner just types the invoice number |
| Arrival notification on scan | **PARTIAL** | event fires, handler prints |
| Driver QR generation | **MISSING** | |
| Driver QR recognition → driver's parcel list | **MISSING** | no driver entity, no parcel→driver link |
| Bulk arrival from the driver list | **MISSING** | `/parcel/complete` handles one invoice per call |

## What blocks what

Rough dependency order for the missing work:

1. **`User.studentNumber`** — blocks the signup flow and the lost-report list. Smallest change; nullable column, safe under `ddl-auto: update`.
2. **Unclaimed TOP 3** — blocks nothing, needed by both apps, and buildable today from existing data. The cheapest visible win.
3. **`report` aggregate** — blocks the admin lost-report page. Depends on (1) for the student number.
4. **`notification` aggregate** — blocks all five notification triggers. Needs a persistence model and a scheduler (`@EnableScheduling` is not enabled), plus a transport decision.
5. **`driver` aggregate + QR** — blocks two admin pages. Needs open question #5 answered first (is a driver a `User` or its own entity?).
6. **Bulk arrival** — depends on (5), since the driver list is what it operates on.
7. **`chat` aggregate** — largest and most independent; needs a transport decision that affects `SecurityConfig`.
8. **Delivery progress stages** — depends on open question #1 and on what the Apick API actually returns.

## Defects and risks found in the current code

Concrete problems, not style. Each is worth a decision before building on top of it.

### 1. Apick tracking silently accepts every invoice number

`ApickTrackingService` short-circuits to `return true` when `apick.api-key` is blank or `dummy-key` — and `@Value` defaults it to exactly `dummy-key`. Any environment missing `APICK_API_KEY` therefore treats **every** invoice number as valid, defeating the validation in `registerParcel`. The failure is silent: registration succeeds normally.

The same class also calls `println` instead of a logger, creates its `RestClient` as a field with **no timeouts**, and is invoked **inside** the `@Transactional` write in `registerParcel` — a hanging carrier API holds a database connection for the duration.

Its response DTO uses camelCase field names (`invoiceNo`, `receiverName`) with no `@JsonProperty`, which will not bind if the API returns snake_case.

### 2. `@AuthenticationPrincipal` type mismatch

`JwtAuthenticationFilter` sets the raw `User` entity as principal; OAuth2 handlers set `UserPrincipal`. `ParcelController` declares `UserPrincipal`, `UserController` declares `User`. Spring injects `null` on mismatch rather than failing, so the wrong declaration becomes an NPE in the service instead of a 401.

Every new endpoint has to make this choice, so it should be settled before six more are written.

### 3. No global exception handler

There is no `@RestControllerAdvice` anywhere. HTTP statuses come from `@ResponseStatus` on `ParcelException` subclasses, and everything outside the parcel domain throws `ResponseStatusException` inline. Consequences: no consistent error body shape for either frontend, and `ParcelException`'s `status` property is dead weight that can silently disagree with its annotation.

Both clients will need a predictable error shape. Adding an advice changes the behavior of every existing endpoint, so it is best done now rather than after the frontends have coded around the current behavior.

### 4. No bean validation

`spring-boot-starter-validation` is not a dependency. Request DTOs carry no constraints, and validation is hand-rolled `if` checks — thorough in `EmailAuthController`, absent in the parcel flow. `ParcelRegisterRequest` accepts a blank invoice number.

### 5. N+1 on every parcel list

`ParcelResponse.from` reads `parcel.owner.name`, and `Parcel.owner` is `LAZY` (correctly). Every list endpoint therefore issues one extra query per parcel. With `show-sql: true` already on, this is directly observable. Fix at the repository with `JOIN FETCH p.owner`, not by switching to EAGER.

### 6. `ParcelZoneMapResponse` is dead code

Defined with `imageUrl` and zone labels, referenced by nothing. Either wire it to a zone-map endpoint or delete it — an unused DTO reads as an existing feature.

### 7. OAuth flow will not survive a second instance

`OneTimeAuthCodeStore` is an in-memory map with a 30-second TTL, and the login flow depends on an HTTP session while `SessionCreationPolicy.STATELESS` is configured. Running two instances breaks login. Fine for now; a deployment blocker later.

`OAuth2AuthenticationFailureHandler` also puts `exception.localizedMessage` directly into the redirect URL, exposing internal messages to the browser.

### 8. Effectively no tests

One test exists: `contextLoads()`. It boots the full Spring context, so it needs a live MySQL plus `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`. A green `./gradlew build` therefore proves only that the context loads — no endpoint, role, or query is exercised. `spring-security-test` is on the classpath and unused.

### 9. No CI

`.github/` contains only a PR template. No workflow runs the build on a pull request.
