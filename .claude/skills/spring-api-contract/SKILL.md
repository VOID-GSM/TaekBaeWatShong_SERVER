---
name: spring-api-contract
description: "Design and write the HTTP contract for a TaekBaeWatShong server endpoint before any implementation — path, verb, request/response DTO shape, status codes, role matrix, and the exact exception each failure path throws. Use this skill whenever adding, changing, or reviewing an API endpoint on this Spring Boot server, whenever a frontend asks 'what does this endpoint return', whenever writing a controller or a DTO class, and whenever an endpoint's error behavior needs to be pinned down. This project has no OpenAPI/Swagger, so this contract document is the only API specification that exists — produce it before writing the controller, not after."
---

# API Contract Design

Contract-first design for this server's endpoints. Because there is no springdoc, no OpenAPI file, and no `@RestControllerAdvice`, the contract produced here is the only artifact describing an endpoint's behavior — and error behavior in particular is invisible to any tooling. Getting it written down first prevents the two failures this codebase is most exposed to: a controller and a client disagreeing about response shape, and an unhandled failure path returning a 500 that nobody designed.

Read `spring-kotlin-conventions` first for naming, DTO, and exception style.

## Contract document format

Write one contract per endpoint into `_workspace/01_api-designer_contract.md`. Use this exact structure:

```markdown
### <VERB> <path> — <one-line purpose>

**Access:** <public | authenticated | hasAnyRole('ADMIN','TEACHER') | owner-only>
**Controller:** `domain/<agg>/controller/<Agg>Controller.kt#<methodName>`
**Service:** `<Agg>Service.<methodName>(...)`

**Request**
- Path: `<param>: <Type>`
- Query: `<param>: <Type>?` (required=false)
- Body: `<Action>Request(field: Type, optional: Type? = null)`

**Success**
- Status: `200 OK` | `201 CREATED`
- Body: `<Action>Response` — full field list with nullability

**Failures**
| Condition | Exception thrown | Status | Korean message |
|-----------|------------------|--------|----------------|
| ... | `ParcelException.NotFound` | 404 | "..." |

**Notes**
- side effects, events published, idempotency, ordering
```

Every row of the failure table must name a real exception class that exists or that you are specifying into existence. "Returns 400" without an exception class is not a contract — someone has to throw something, and in this project the thrown type *is* the status.

## Path and verb rules

Base paths are singular and unprefixed: `/parcel`, `/auth`. No `/api`, no `/v1`. `UserController`'s `/api/users/me` is the outlier — do not use it as a model.

| Intent | Verb | Path shape |
|--------|------|-----------|
| Create a resource | `POST` | `/parcel` |
| Read a collection | `GET` | `/parcel`, `/parcel/me` |
| Read one resource | `GET` | `/parcel/{parcelId}` |
| Mutate existing state | `PATCH` | `/parcel/claim`, `/parcel/{parcelId}/zone` |

`PUT` and `DELETE` are unused in this codebase. If a new endpoint genuinely needs full replacement or deletion semantics, introduce it deliberately and say so in the contract notes — do not reach for `PATCH` to avoid the conversation.

Note the two `PATCH` shapes already in use: action-on-collection (`/parcel/complete`, `/parcel/claim`, where the target is identified by a field in the body) and action-on-resource (`/parcel/{parcelId}/zone`). Prefer the path-parameter form for new endpoints — it makes the target explicit and lets `@PreAuthorize` and logging see it. Use the body form only when the natural identifier is a business key that is awkward in a URL, as `invoiceNumber` is.

## Status code selection

| Status | When | How it is produced |
|--------|------|--------------------|
| 200 | Successful read or mutation | `ResponseEntity.ok(...)` |
| 201 | New resource created | `ResponseEntity.status(HttpStatus.CREATED).body(...)` |
| 400 | Malformed or business-invalid input | `<Agg>Exception.Invalid*` with `@ResponseStatus(BAD_REQUEST)` |
| 401 | Missing or invalid token | Spring Security, automatic |
| 403 | Authenticated but not permitted | `@PreAuthorize` denial, or `AccessDeniedException` for ownership |
| 404 | Target does not exist | `<Agg>Exception.NotFound` |
| 409 | Conflicts with existing state | `<Agg>Exception.Conflict` |

**403 has two distinct sources and they are not interchangeable.** Role checks belong in `@PreAuthorize` on the controller, because they depend only on the caller. Ownership checks belong in the service, because they require loading the resource first — see `ParcelService.claimParcel`, which throws `AccessDeniedException("본인의 택배만 회수 처리할 수 있습니다.")` after comparing `parcel.owner.id` to `user.id`. Specify which one applies; an ownership rule written as `@PreAuthorize` cannot work, and a role rule pushed into the service loses the declarative guarantee.

Distinguish 400 from 409 carefully. In `ParcelService.registerParcel`, a duplicate invoice number throws `Conflict` (409) because the state already exists, while an unverifiable invoice number throws `InvalidInvoice` (400) because the input is wrong. `ParcelException` also defines a `DuplicateInvoice` subclass mapped to 400 that no code currently throws — if you need duplicate semantics, use `Conflict` and note the dead subclass rather than reviving an inconsistent one.

## DTO shape design

**Requests.** One `data class` per action in `dto/request/`, named `<Action>Request`. Optional fields are nullable with a `= null` default. Since there is no bean validation, every constraint you write in the contract must have a corresponding `if` check in the service — list them in the failure table so the service engineer implements them.

**Responses.** One `data class` per action in `dto/response/`, named `<Action>Response`, with a `companion object { fun from(entity) }` factory.

Three rules that prevent the boundary bugs this codebase is prone to:

1. **Collections are always wrapped.** `ParcelListResponse(val parcels: List<ParcelResponse>)`, never a bare `List<ParcelResponse>`. A bare array cannot gain a `totalCount` later without breaking every client, and a client written against `[...]` crashes when it becomes `{...}`.
2. **Nullability in the DTO must match reality.** `ParcelResponse.arrivedAt` is `LocalDateTime? = null` because a `PENDING` parcel has no arrival time. Marking it non-null would make the field a lie the moment the first pending parcel is serialized. Walk each field against the entity's state machine and mark it nullable if *any* reachable state leaves it unset.
3. **Name fields exactly as the client will read them.** Jackson serializes Kotlin property names verbatim — `invoiceNumber` in Kotlin is `invoiceNumber` in JSON. There is no naming-strategy configuration, so there is no snake_case conversion anywhere. Do not assume one.

When a response needs a shape that is not a straight entity projection, nest it inside the response class rather than creating a top-level DTO: `ParcelZoneGroupResponse.ZoneGroupDetail`. This keeps the `dto/response/` directory readable as a list of endpoints.

## Access control matrix

Every contract states access in one of four forms:

| Form | Written as | Example |
|------|-----------|---------|
| Public | listed in `SecurityConfig` permitAll | `/auth/login` |
| Authenticated | nothing — the default is `authenticated()` | `POST /parcel` |
| Role-gated | `@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")` | `GET /parcel` |
| Owner-only | service-side id comparison + `AccessDeniedException` | `PATCH /parcel/claim` |

Roles are `STUDENT`, `TEACHER`, `ADMIN`. In `@PreAuthorize`, write `hasAnyRole('ADMIN', 'TEACHER')` without the `ROLE_` prefix — `UserPrincipal.getAuthorities()` and `JwtAuthenticationFilter` both emit `ROLE_${user.role}`, and `hasRole`/`hasAnyRole` add the prefix themselves. Writing `hasRole('ROLE_ADMIN')` silently matches nothing and denies everyone.

Anything not in `SecurityConfig`'s permitAll list requires a token. Adding a public endpoint therefore means editing `SecurityConfig` too — say so in the contract notes so the security engineer picks it up, because a new controller path alone will 401.

## Specifying the principal

State in the contract which principal type the controller declares, and verify it against the authentication path:

- Normal JWT-authenticated calls arrive with the raw **`User`** entity as principal (`JwtAuthenticationFilter`).
- OAuth2 login handlers see **`UserPrincipal`**.

`ParcelController` declares `UserPrincipal` and reads `.user`; `UserController` declares `User`. Spring injects `null` on a type mismatch instead of failing, so the wrong choice becomes an NPE in the service rather than a 401. Write the intended type into the contract explicitly and flag the inconsistency if the endpoint's auth path contradicts the aggregate's existing style.

## Worked example

```markdown
### PATCH /parcel/claim — 수령 완료 처리

**Access:** authenticated + owner-only
**Controller:** `domain/parcel/controller/ParcelController.kt#claimParcel`
**Service:** `ParcelService.claimParcel(user, request)`

**Request**
- Body: `ParcelClaimRequest(invoiceNumber: String)`

**Success**
- Status: `200 OK`
- Body: `ParcelClaimResponse` — id, invoiceNumber, alias, status(=CLAIMED), claimedAt

**Failures**
| Condition | Exception thrown | Status | Korean message |
|-----------|------------------|--------|----------------|
| invoiceNumber에 해당하는 택배 없음 | `ParcelException.NotFound` | 404 | "해당 운송장 번호의 택배를 찾을 수 없습니다." |
| 본인 택배가 아님 | `AccessDeniedException` | 403 | "본인의 택배만 회수 처리할 수 있습니다." |

**Notes**
- `markAsClaimed()` sets status and claimedAt together; no explicit save (dirty checking).
- Not idempotent by design: claiming twice overwrites claimedAt. Flag to the user if that matters.
- Already-CLAIMED and not-yet-ARRIVED parcels are currently accepted. If the contract should reject them, that is a new failure row and a new service check.
```

That last note is the point of writing contracts here: the gap only becomes visible when the failure table is filled in deliberately.

## Handoff

The finished contract is the input for three other agents — the web engineer builds the controller and DTOs from it, the domain engineer reads the persistence implications, and the QA inspector uses the failure table as its checklist. So a contract with a vague failure row produces an unverifiable endpoint. If a failure condition's correct behavior is genuinely a product decision (should double-claiming 409?), write both options into the notes and raise it rather than picking silently.

## Language rule

**Skill and agent definition files are written in English. Everything produced for a human is written in Korean** — every response to the user, workspace notes and reports meant for the team, commit messages (after the English type prefix), PR bodies, review comments, and thread replies. Code identifiers, package names, and log output stay English; user-facing exception messages stay Korean.
