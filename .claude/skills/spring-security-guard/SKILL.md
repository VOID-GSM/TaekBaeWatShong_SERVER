---
name: spring-security-guard
description: "Wire and verify authentication and authorization on the TaekBaeWatShong server — SecurityConfig matchers, @PreAuthorize role expressions, the ROLE_ prefix trap, owner-only checks, the JWT filter chain, the Google OAuth2 + one-time-code login flow, and CORS. Use this skill whenever adding a public endpoint, gating an endpoint by STUDENT/TEACHER/ADMIN role, enforcing that a user may only touch their own data, touching SecurityConfig / JwtTokenProvider / JwtAuthenticationFilter / anything under global/security/, debugging an unexpected 401 or 403, or reviewing whether a new endpoint is protected at all. Every new endpoint needs an access decision — use this skill to make and verify it."
---

# Security & Access Control

How authentication and authorization actually work on this server, and the specific ways they break. Read `spring-kotlin-conventions` first for style.

## The filter chain

`global/config/SecurityConfig.kt` — `@EnableWebSecurity @EnableMethodSecurity`:

- CSRF disabled, sessions `STATELESS`, CORS from `app.cors.allowed-origins` (comma-split SpEL into `allowedOriginPatterns`, `allowCredentials = true`).
- `JwtAuthenticationFilter` inserted before `UsernamePasswordAuthenticationFilter`. It is constructed manually inside `filterChain`, not registered as a bean.
- `BCryptPasswordEncoder` bean for the email/password path.
- permitAll list: `/`, `/auth/login`, `/auth/admin/login`, `/auth/token`, `/auth/email/signup`, `/auth/email/login`, `/auth/email/admin/signup`, `/auth/email/admin/login`, `/oauth2/**`, `/login/**`.
- **`anyRequest().authenticated()`** — everything else needs a token.

That default is the most important fact here: a new controller is protected automatically, and a new *public* endpoint will return 401 until its path is added to the permitAll list. Adding a public endpoint is therefore always a two-file change.

## Rule 1 — never write the `ROLE_` prefix in `@PreAuthorize`

Authorities are built as `ROLE_${user.role}` in two places: `UserPrincipal.getAuthorities()` and `JwtAuthenticationFilter`. So a `TEACHER` carries the authority `ROLE_TEACHER`.

`hasRole` and `hasAnyRole` **add the `ROLE_` prefix themselves**. Writing it again produces `ROLE_ROLE_TEACHER`, which matches nobody:

```kotlin
@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")   // correct — matches ROLE_ADMIN / ROLE_TEACHER
@PreAuthorize("hasRole('ROLE_ADMIN')")            // WRONG — denies everyone, silently
@PreAuthorize("hasAuthority('ROLE_ADMIN')")       // also correct, but not the house style
```

This fails closed, so it never shows up as a security hole — it shows up as "admins get 403" long after the change. Use `hasAnyRole('ADMIN', 'TEACHER')`, matching `ParcelController`.

Roles are exactly `STUDENT`, `TEACHER`, `ADMIN` (`domain/user/entity/Role.kt`). There is no hierarchy configured: `ADMIN` does **not** implicitly satisfy a `hasRole('TEACHER')` check. Every role that should pass must be listed explicitly, which is why the existing annotations name both `ADMIN` and `TEACHER`.

## Rule 2 — role checks in the controller, ownership checks in the service

They are not interchangeable, because a role check needs only the caller while an ownership check needs the resource loaded.

**Role check** — declarative, on the controller method:

```kotlin
@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
@GetMapping
fun getAllParcels(...)
```

**Ownership check** — imperative, in the service after loading:

```kotlin
val parcel = parcelRepository.findByInvoiceNumber(request.invoiceNumber)
    ?: throw ParcelException.NotFound("해당 운송장 번호의 택배를 찾을 수 없습니다.")

if (parcel.owner.id != user.id) {
    throw AccessDeniedException("본인의 택배만 회수 처리할 수 있습니다.")
}
```

Use Spring's `org.springframework.security.access.AccessDeniedException` — do not invent an aggregate-specific forbidden exception. `@EnableMethodSecurity` is on, so it resolves to 403.

Two details in that comparison to get right:

- **Order matters.** Load-then-check returns 404 for a non-existent resource and 403 for someone else's. Checking first would leak nothing but cannot be written, since ownership is unknown before the load. Keep this order.
- **`parcel.owner.id` and `user.id` are both `Long?`** (`User.id` is nullable). `!=` on two nulls is `false`, so a pair of unsaved entities would compare as "same owner". In practice both are persisted so both are non-null, but do not replicate this pattern on a path where either side might be transient.

A third pattern exists for endpoints that are scoped rather than checked: `GET /parcel/me` passes `userPrincipal.user` into a repository method that filters by owner, so there is nothing to deny. Prefer this shape when the endpoint returns a collection — filtering at the query is both safer and faster than loading everything and checking each row.

## Rule 3 — know which principal type arrives

**The two authentication paths inject different principal objects, and Spring gives you `null` instead of an error when the declared type is wrong.**

| Path | Principal set | Set by |
|------|--------------|--------|
| Bearer token on a normal API call | the raw `User` entity | `JwtAuthenticationFilter` |
| Google OAuth2 login handlers | `UserPrincipal` (wraps `User`) | `CustomOAuth2UserService` |

`JwtAuthenticationFilter` reads `Authorization: Bearer <token>`, loads the `User` by `claims.subject.toLong()`, and sets `UsernamePasswordAuthenticationToken(user, null, listOf(SimpleGrantedAuthority("ROLE_${user.role}")))` — the principal is the entity.

So on a JWT-authenticated endpoint:

```kotlin
@AuthenticationPrincipal user: User            // resolves — matches UserController
@AuthenticationPrincipal p: UserPrincipal      // resolves to null on this path
```

`ParcelController` declares `UserPrincipal` and dereferences `.user`. Under a plain Bearer-token call that is an NPE, not a 401. `UserController` declares `User`. **This is a live inconsistency in the codebase, not a style choice.**

When writing a new endpoint: declare `User` for anything reached with a Bearer token. When touching an existing `UserPrincipal` endpoint, do not silently "fix" it — the change alters runtime behavior for every caller. Report it and let the user decide whether to unify on `User` or make the filter emit `UserPrincipal`. `spring-qa-verify` flags this on every review.

Note also that the filter hits the database on **every request** to load the user. That is a known cost, not a bug to fix in passing.

## Rule 4 — JWT specifics

`global/security/jwt/JwtTokenProvider.kt`, jjwt 0.12.6:

- HS256 via `Keys.hmacShaKeyFor`. The `init` block requires the secret to be **at least 32 bytes** — a shorter `JWT_SECRET` fails at startup, which is the correct behavior and should not be relaxed.
- `createToken(userId, email)` puts the user id in `sub` and email in an `email` claim. Validity is `jwt.access-token-validity-ms`, default 1 hour.
- There is **no refresh token** and no revocation list. A token is valid until it expires.
- `getClaims` returns `null` for expired, malformed, and illegal-argument cases alike, so the filter cannot distinguish "expired" from "forged" — both produce an unauthenticated request and then a generic 401. If a caller needs "token expired" specifically, that is a change to `JwtTokenProvider`, not something a controller can infer.

Do not add claims casually: anything you put in the token is readable by the client (JWTs are signed, not encrypted) and cannot be revoked before expiry.

## Rule 5 — the OAuth2 login flow

`global/security/oauth/`, Google only:

1. `GET /auth/login?role=STUDENT|TEACHER` or `GET /auth/admin/login` (`AuthController`) stores `login_type` (and `signup_role`) in the **HTTP session**, then redirects to `/oauth2/authorization/google`.
2. `CustomOAuth2UserService` reads that session via `RequestContextHolder`, rejects a missing `login_type`, and for admin logins requires the email to be in `app.admin.emails` — otherwise `OAuth2AuthenticationException("not_admin")`. It creates or updates the `User` and returns a `UserPrincipal`.
3. `OAuth2AuthenticationSuccessHandler` issues a JWT, stores it in `OneTimeAuthCodeStore`, and redirects to `app.oauth2.redirect-uri` (or `admin-redirect-uri` for ADMIN) with `?code=`.
4. `GET /auth/token?code=` exchanges the code for a `TokenResponse`.

`OneTimeAuthCodeStore` is an in-memory `ConcurrentHashMap` with UUID codes, a **30-second TTL**, single use, purged on issue.

Three consequences to keep in mind rather than to fix silently:

- **The flow depends on an HTTP session while `SessionCreationPolicy.STATELESS` is configured.** It works because Spring Security's OAuth2 authorization-request repository still creates a session for the redirect, but it is fragile — any change to session handling can break login without touching the OAuth code.
- **Neither the session nor the code store is distributed.** Running more than one instance breaks login, since the callback may land on a different node than the one holding the session and the code.
- **`OAuth2AuthenticationFailureHandler` puts `exception.localizedMessage` into the redirect URL,** exposing internal messages to the browser. Worth reporting; changing it alters what the frontend receives, so confirm before editing.

The Google user's identity is read from `sub`/`name`/`email` (`GoogleOAuth2UserInfo`).

## Rule 6 — email/password auth

`global/security/email/` (note: under `global`, not `domain/user`). `EmailAuthController` exposes `/auth/email/{signup,login,admin/signup,admin/login}`, all returning `TokenResponse`.

Validation is hand-rolled — a top-level `EMAIL_REGEX`, a password length bound of 8–72 bytes, a blank-name check — throwing `ResponseStatusException`. The 72-byte upper bound is not arbitrary: **BCrypt silently truncates input beyond 72 bytes**, so rejecting longer passwords prevents two different passwords from authenticating the same account. Keep that bound.

`EmailAuthService` gates admin signup on the `app.admin.emails` allowlist, verifies the account's provider is `LOCAL` before checking the password, and maps `DataIntegrityViolationException` to 409. The provider check matters: without it, a Google-created account with a `null` password could be attacked through the password path.

## Access decision checklist

For every endpoint being added or changed:

- [ ] Is it public? If yes, its exact path is added to the permitAll list in `SecurityConfig` — a controller mapping alone yields 401.
- [ ] Is it role-gated? `@PreAuthorize("hasAnyRole(...)")` on the controller method, no `ROLE_` prefix, every permitted role listed (no hierarchy exists).
- [ ] Does it touch data belonging to one user? Ownership check in the **service**, after loading, throwing `AccessDeniedException`. Or scope the repository query by owner and avoid the check entirely.
- [ ] Does the declared `@AuthenticationPrincipal` type match the authentication path that actually reaches it?
- [ ] Does a new public path need a CORS origin? `app.cors.allowed-origins` defaults to `localhost:3000,3001`.
- [ ] Does the endpoint leak existence through its status codes (404 vs 403) in a way the contract did not intend?

## Verification

There is no security test suite — `spring-security-test` is on the classpath but unused, and the only test is `contextLoads()`. So a security change cannot be validated by running the build.

Verify by reading, and say that is what you did:

1. Grep every `@PreAuthorize` for a `ROLE_` prefix inside `hasRole`/`hasAnyRole`.
2. Cross-check each controller mapping path against `SecurityConfig`'s permitAll list — a public endpoint missing from it, and a sensitive endpoint accidentally matched by `/login/**` or `/oauth2/**`, are both invisible until runtime.
3. Cross-check every `@AuthenticationPrincipal` declaration against the filter that authenticates that path.
4. Confirm each ownership check runs after the load and throws `AccessDeniedException`.

If the change warrants real coverage, a `@WebMvcTest` with `spring-security-test`'s `@WithMockUser(roles = ["TEACHER"])` is the cheapest way to prove a role gate works — it needs no database. Propose it rather than assuming the user wants new test infrastructure.

## Language rule

**Skill and agent definition files are written in English. Everything produced for a human is written in Korean** — every response to the user, workspace notes and reports meant for the team, commit messages (after the English type prefix), PR bodies, review comments, and thread replies. Code identifiers, package names, and log output stay English; user-facing exception messages stay Korean.
