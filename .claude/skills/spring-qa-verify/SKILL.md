---
name: spring-qa-verify
description: "Cross-boundary integration verification for the TaekBaeWatShong server — compare controller signatures against service signatures, DTO factories against entity nullability, thrown exceptions against the declared contract, @PreAuthorize against SecurityConfig, derived query names against entity properties, and principal types against the filter that sets them. Use this skill after implementing or changing any endpoint, before opening a PR, when reviewing a diff on this server, when an endpoint returns 500/null/403 unexpectedly, or whenever asked to verify, audit, QA, or sanity-check backend changes. Verifies connections between layers, not the existence of files — a green ./gradlew build proves almost nothing on this project."
---

# Integration Coherence Verification

Verification for a layered Spring server. The defects that actually reach production here are not inside a class — they are at the seams between two classes that are each individually correct.

## Why the build is not the check

`./gradlew build` runs exactly one test: `TaekBaeWatShongServerApplicationTests.contextLoads()`. And it boots the full Spring context, so it requires a reachable MySQL plus `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` (≥32 bytes), `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`. Two consequences:

- If those are unavailable, the build fails for reasons unrelated to the change. **Report that as "could not verify", never as a code failure and never as a pass.**
- If they are available and it passes, all that was proven is that the context loads. No endpoint was called, no role was checked, no query was executed against real data.

Kotlin's compiler catches more than a dynamically typed stack would — a controller calling a service method with the wrong argument types will not compile. What it cannot catch is everything below.

## Run these first

```bash
./gradlew compileKotlin                              # type errors across layers
./gradlew test                                       # context load — repository proxies, bean wiring
git diff --stat main...HEAD                          # scope of the change
```

`compileKotlin` is fast and worth running before any manual analysis. `test` is what validates **derived repository query names**, because Spring Data builds repository proxies at context startup — a typo like `findAllByOnwer` compiles fine and fails only there.

## The seven cross-checks

Each one requires reading **both sides at once**. Reading one side and reasoning about what the other probably does is how these bugs survive review.

### 1. Controller ↔ Service signature and return type

| Left (caller) | Right (callee) |
|---|---|
| `<Agg>Controller` method body | `<Agg>Service` method |

Check: the controller passes what the service expects; the service's return type is the DTO the controller declares in `ResponseEntity<T>`; the controller does no business logic and touches no repository or entity.

Kotlin catches type mismatches. What it does not catch: a controller declaring `ResponseEntity<ParcelResponse>` while the contract promised `ParcelListResponse`, or a controller that quietly grew an `if` branch.

### 2. Response DTO ↔ entity nullability

| Left | Right |
|---|---|
| `<Action>Response` property types | the entity's reachable states |

For each property, ask whether **any reachable state** leaves the source field unset. `ParcelResponse.arrivedAt` is `LocalDateTime?` because a `PENDING` parcel has none. A non-null DTO field fed from a nullable entity field does not compile — but a non-null field fed from a *computed* property that returns a default (like `unclaimedDays` returning `0` for non-arrived parcels) compiles and quietly ships a misleading value.

Also verify the `from()` factory maps **every** constructor parameter, and that no field was added to the DTO without being added to `from()`.

### 3. Thrown exceptions ↔ contract failure table

| Left | Right |
|---|---|
| every `throw` on the code path | the contract's failure table |

Because there is no `@RestControllerAdvice`, **the thrown type is the HTTP status.** Verify for each throw:

- The exception class carries a `@ResponseStatus`, or it will surface as 500. A bare `RuntimeException`, `IllegalArgumentException`, or `IllegalStateException` from a service is a 500 — flag it.
- The `@ResponseStatus` value and the `status` constructor argument **agree**. `ParcelException` passes the status twice; if they disagree the annotation wins and the property lies. This cannot fail any test.
- Every failure row in the contract has a matching throw, and every throw has a matching row. A throw with no row is undocumented behavior; a row with no throw is a promise the code does not keep.

Watch for exceptions escaping from a layer that has no mapping: `apickTrackingService` swallows its failures and returns `false`, but an uncaught `RestClient` exception elsewhere becomes a 500.

### 4. `@PreAuthorize` ↔ `SecurityConfig` ↔ roles

| Left | Right |
|---|---|
| controller mapping paths and `@PreAuthorize` | `SecurityConfig` permitAll list, `Role` enum |

- No `ROLE_` prefix inside `hasRole` / `hasAnyRole` — `ROLE_ROLE_ADMIN` matches nobody and fails closed, so it will not be noticed for weeks.
- Every role name is a real `Role` constant (`STUDENT`, `TEACHER`, `ADMIN`). A typo like `hasRole('TEACHOR')` compiles — it is a string.
- There is **no role hierarchy**: `ADMIN` does not satisfy `hasRole('TEACHER')`. Every permitted role must be listed.
- Any endpoint intended to be public appears verbatim in the permitAll list. Any endpoint *not* intended to be public is not accidentally covered by the `/login/**` or `/oauth2/**` wildcards.
- Endpoints handling one user's data have an ownership check in the service, or a repository query scoped by owner.

### 5. `@AuthenticationPrincipal` type ↔ the filter that authenticates the path

| Left | Right |
|---|---|
| `@AuthenticationPrincipal x: T` in the controller | `JwtAuthenticationFilter` / `CustomOAuth2UserService` |

**This is the highest-value check in this codebase.** `JwtAuthenticationFilter` sets the raw `User` entity as principal; OAuth2 handlers set `UserPrincipal`. Spring injects **`null`** on a type mismatch rather than failing, so the wrong declaration becomes a `NullPointerException` inside the service — a 500 with a confusing stack trace, not a 401.

`ParcelController` declares `UserPrincipal`; `UserController` declares `User`. Report this mismatch on every review that touches either. Do not unify it as a drive-by fix — it changes runtime behavior for existing callers and needs the user's decision.

### 6. Repository query names ↔ entity properties

| Left | Right |
|---|---|
| derived method names in `<Agg>Repository` | property names in the entity |

Decompose each derived name into its property path — `findAllByOwnerAndStatusOrderByCreatedAtDesc` → `owner`, `status`, `createdAt` — and confirm each exists on the entity with a compatible type. These fail at **context startup**, not compile time, so `compileKotlin` says nothing about them.

Also verify:

- Every collection-returning query has an explicit `OrderBy` / `ORDER BY`; without one MySQL row order is unspecified and the endpoint returns different orderings between calls.
- `@Query` JPQL references entity and property names (`Parcel p ... p.claimedAt`), not table or column names.
- `@Param` names match the JPQL placeholders exactly.

### 7. Lazy associations ↔ DTO field access (N+1)

| Left | Right |
|---|---|
| the repository method feeding a list endpoint | the `from()` factory's field reads |

If the DTO factory dereferences a `FetchType.LAZY` association — `parcel.owner.name` in `ParcelResponse.from` — and the query returns a collection, that is one extra SELECT per row. Confirm the query uses `JOIN FETCH`, or flag it.

With `show-sql: true` already set in `application.yml`, this is directly observable if the app can be run: call the endpoint and count `select` lines. If it cannot be run, the static read of both sides is sufficient to flag it.

## Report format

Write to `_workspace/04_qa_report.md`:

```markdown
# QA Report — <feature>

## Verified
| # | Check | Result | Evidence |
|---|-------|--------|----------|
| 1 | Controller ↔ Service | PASS | ParcelController.kt:71 → ParcelService.kt:124, both ParcelClaimResponse |

## Findings
### [BLOCKER] <one-line defect>
- **Where:** `path/File.kt:LINE` ↔ `path/Other.kt:LINE`
- **Failure:** concrete input → wrong output/crash
- **Fix:** specific change, addressed to the owning agent

## Not verified
- <check> — <why: no DB, cannot boot context, etc.>
```

Severity: **BLOCKER** (wrong behavior at runtime — mismatched principal, unmapped exception, broken role gate, missing permitAll), **MAJOR** (correct but wrong under load or edge cases — N+1, missing OrderBy, unhandled null), **MINOR** (style, naming, dead code).

**The "Not verified" section is mandatory and must not be empty when the environment blocked something.** Silence about an unrun check reads as a pass. On this project, at minimum, anything requiring a live database goes there unless the context genuinely booted.

## Reporting discipline

- Cite `file:line` on **both** sides of a boundary finding. A finding citing one side has not actually been cross-checked.
- Give a concrete failure scenario — "a STUDENT calling PATCH /parcel/claim on another student's parcel gets 500 instead of 403 because `userPrincipal` is null" — not "principal type may be wrong".
- Send boundary findings to **both** owning agents, since either side could be the one to change.
- Never report a passing check you did not run. If `./gradlew test` could not run, the build row is "not verified", not "PASS".

## Incremental use

Run these checks **as each piece lands**, not once at the end. Check 1 and 3 as soon as a service and controller pair exists; checks 4 and 5 the moment an endpoint is annotated; checks 6 and 7 when the repository method is written. A principal-type mismatch found at the first endpoint costs one line; found after five endpoints copied the same pattern, it costs five and a behavior discussion.

## Language rule

**Skill and agent definition files are written in English. Everything produced for a human is written in Korean** — every response to the user, workspace notes and reports meant for the team, commit messages (after the English type prefix), PR bodies, review comments, and thread replies. Code identifiers, package names, and log output stay English; user-facing exception messages stay Korean.
