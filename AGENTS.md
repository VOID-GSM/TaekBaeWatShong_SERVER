# AGENTS.md

Shared instructions for any AI coding agent working in this repository — Codex, Claude Code, or anything else that reads `AGENTS.md`.

This file is **tool-agnostic and self-contained**: everything needed for ordinary work is here. Deeper references are listed at the bottom and can be read on demand. Claude Code additionally loads `CLAUDE.md`, which wires up its agent-team harness; that harness is optional and nothing here depends on it.

## Language rule

**Instruction files under `.claude/` are written in English. Everything produced for a human is written in Korean.**

That covers every response to the user, commit messages (Korean subject after the English type prefix), PR titles and bodies, code review comments, review thread replies, and user-facing exception messages. Code identifiers, package names, and log output stay English.

## Project

**TaekBaeWatShong (택배왔숑)** — a school parcel management service built to prevent parcels from being left unattended or lost. Students and teachers order parcels to the school; this server tracks each parcel's owner, arrival, storage zone, and pickup, and applies pressure to unclaimed ones.

This repository is the **backend** for two separate frontends:

- **Client** — every teacher and student who orders parcels.
- **Admin** — one 학생생활안전부 teacher, 학생회 학생생활안전부 students, the VOID team, and delivery drivers.

Built by **VOID** (`VOID-GSM`).

**A large part of the product is not built yet.** Before planning any feature, read `docs/IMPLEMENTATION-STATUS.md` — it maps every spec item to its current state and lists nine known defects. Building on an assumed-existing feature is the most common way to waste a session here.

## Stack

| Item | Value |
|------|-------|
| Kotlin | 1.9.23, `-Xjsr305=strict` |
| Spring Boot | 3.2.5 |
| Java toolchain | 21 |
| Persistence | Spring Data JPA + MySQL, `ddl-auto: update` |
| Security | Spring Security + OAuth2 client (Google) + JJWT 0.12.6 |
| Build | Gradle Kotlin DSL, `./gradlew` |

**Not on the classpath — do not use:** bean validation (`jakarta.validation`, `@Valid`, `@NotBlank`), springdoc/Swagger, H2, Testcontainers, ktlint/detekt/spotless. Propose adding one rather than writing code that silently does nothing.

## Verification

```bash
./gradlew compileKotlin   # fast type check
./gradlew test            # boots the Spring context
./gradlew build           # compile + test
```

**A green build proves very little here.** The only test is `TaekBaeWatShongServerApplicationTests.contextLoads()`, and it boots the full context, so it needs a reachable MySQL plus `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` (≥32 bytes), `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`.

- If those are missing, the build fails for reasons unrelated to your change. Report that as **"could not verify"** — never as a code failure, never as a pass.
- Never report `compileKotlin` success as "tests pass".
- Derived repository query names are validated at **context startup**, not compile time. If you cannot boot the context, verify them by hand against entity property names and say the check was manual.

## Package layout

```
com.example.taekbaewatshongserver
├── domain/<aggregate>/
│   ├── controller/     <Aggregate>Controller.kt
│   ├── dto/request/    <Action>Request.kt
│   ├── dto/response/   <Action>Response.kt
│   ├── entity/         <Aggregate>.kt, enums
│   ├── repository/     <Aggregate>Repository.kt
│   └── service/        <Aggregate>Service.kt, external clients, events
└── global/
    ├── config/         SecurityConfig.kt
    ├── exception/      <Aggregate>Exception.kt
    └── security/       UserPrincipal.kt, jwt/, oauth/, email/
```

Aggregates today: `parcel`, `user`. **Follow `parcel`** — it splits DTOs into `request/` and `response/`, while `user` keeps them flat and uses an `/api/users/me` path that nothing else uses. Do not copy `user`'s layout or its path style.

## Conventions

### Entities

Plain `class` (not `data class` — `allOpen` handles JPA), mutable, with behavior methods.

- **`val` for immutable facts** (id, invoice number, owner, createdAt), `var` only for genuinely mutable state. With no validation layer, this is the main invariant mechanism.
- **Every `@ManyToOne` is `FetchType.LAZY`.** JPA defaults to EAGER, which silently causes N+1.
- **Enums persist as `EnumType.STRING`,** never ordinal.
- **State changes go through named entity methods** — `markAsArrived()`, `markAsClaimed()` — each setting the status *and* its paired timestamp together. Never assign `entity.status = ...` from a service; that is how a status and its timestamp drift apart with nothing to catch it.
- **Derived values are computed properties,** never stored columns.

### DTOs

- Requests: `data class` in `dto/request/`, optional fields nullable with `= null`.
- Responses: `data class` in `dto/response/` with `companion object { fun from(entity) }`.
- **Entities never cross the controller boundary.** All mapping lives in `from()`.
- **Collections are wrapped**, never bare arrays: `ParcelListResponse(val parcels: List<ParcelResponse>)`.
- **Nullability must match reality** — a field is nullable if any reachable entity state leaves it unset.
- Jackson serializes Kotlin property names verbatim. There is no naming strategy configured, so no snake_case conversion anywhere.

### Controllers

Thin: call one service method, wrap in `ResponseEntity`, return. No business logic, no repository access, no entity handling.

- Constructor injection only.
- `ResponseEntity.status(HttpStatus.CREATED).body(...)` for creation, `ResponseEntity.ok(...)` otherwise. Never `ResponseEntity<Any>` or a raw `Map`.
- Base paths are **singular and unprefixed**: `/parcel`, `/auth`. No `/api`, no `/v1`.
- Verbs: `POST` creates, `GET` reads, `PATCH` mutates. `PUT` and `DELETE` are unused.

### Services

```kotlin
@Service
@Transactional(readOnly = true)     // class level
class ParcelService(...) {
    @Transactional                  // override on writes
    fun registerParcel(...): ParcelResponse { ... }
}
```

- Import `org.springframework.transaction.annotation.Transactional`.
- Method order: guard clauses → load → mutate → publish event → return DTO.
- **Mutations rely on dirty checking.** Inside a transaction a loaded entity flushes at commit; call `save()` only for new entities.
- Services return DTOs, never entities.

### Exceptions

One sealed class per aggregate in `global/exception/`, subclasses nested, each with a `@ResponseStatus` **and** a matching `status` argument.

**There is no `@RestControllerAdvice` in this project, so the thrown type *is* the HTTP status.** A bare `RuntimeException`, `IllegalArgumentException`, or `IllegalStateException` from a service is a 500. Use `AccessDeniedException` for ownership failures (403).

### Repositories

Prefer derived query names, even long ones — they are compiler-adjacent and validated at startup. Drop to `@Query` (triple-quoted JPQL, `@Param`) only when a name becomes unreadable. **Every collection query needs an explicit `OrderBy`**, or MySQL row order is unspecified.

## Traps specific to this codebase

These cause real runtime failures and are invisible to the build. Check them on every change.

### 1. `@AuthenticationPrincipal` type mismatch

`JwtAuthenticationFilter` sets the raw **`User`** entity as principal. OAuth2 handlers set **`UserPrincipal`**. Spring injects **`null`** on a type mismatch instead of failing — so the wrong declaration becomes a `NullPointerException` deep in a service, not a 401.

`ParcelController` declares `UserPrincipal`; `UserController` declares `User`. **This is a live inconsistency, not a style choice.** Declare `User` for Bearer-token endpoints. Do not silently "fix" existing endpoints — it changes behavior for live callers; report it instead.

### 2. Missing `@Transactional` on a write

The class default is `readOnly = true`. A write method without the override runs read-only and its changes are **silently discarded at commit** — no exception, no log.

### 3. The `ROLE_` prefix

Authorities are already `ROLE_${user.role}`, and `hasRole` / `hasAnyRole` add the prefix themselves. `hasRole('ROLE_ADMIN')` becomes `ROLE_ROLE_ADMIN` and matches nobody. It fails closed, so it surfaces weeks later as "admins get 403".

Write `hasAnyRole('ADMIN', 'TEACHER')`. Roles are `STUDENT`, `TEACHER`, `ADMIN`, and **there is no hierarchy** — `ADMIN` does not satisfy `hasRole('TEACHER')`. List every permitted role.

### 4. `ddl-auto: update` is additive only

There is no Flyway or Liquibase. Hibernate reconciles the schema at startup:

| Change | Result |
|--------|--------|
| Add a nullable column | Safe |
| Add a non-null column to a populated table | Fails, or takes a silent default |
| Rename a property | Adds a new column, orphans the old one — data appears lost |
| Change a type, or remove a property | Silently ignored / inserts then fail |

**Never report a rename or type change as "done".** State the manual SQL required and involve the user. Adding a whole new `@Entity` is safe.

### 5. N+1 on list endpoints

`ParcelResponse.from` reads `parcel.owner.name`, and `owner` is `LAZY` (correct). Every list endpoint therefore issues one extra query per row. Fix at the repository with `JOIN FETCH p.owner` — **never** by switching the association to EAGER. `show-sql: true` is already on, so it is directly observable.

### 6. Public endpoints need two changes

`SecurityConfig` ends with `anyRequest().authenticated()`. A new controller mapping alone returns 401 — the exact path must also be added to the permitAll list. Conversely, check that a new sensitive path is not accidentally matched by the `/oauth2/**` or `/login/**` wildcards.

### 7. Role checks vs. ownership checks

Role checks are declarative on the controller (`@PreAuthorize`). Ownership checks are imperative in the service, **after** loading, throwing `AccessDeniedException`. They are not interchangeable — an ownership rule cannot be written as `@PreAuthorize` because the resource is not loaded yet.

## Delivery workflow

### Commits

Format: `<type>: <한국어 요약>` — one line, no scope, no body.

| Type | Meaning |
|------|---------|
| `feat` | 새로운 기능 |
| `fix` | 버그 수정 |
| `chore` | 자잘한 작업 |
| `refactor` | 코드 리팩토링 |
| `docs` | 문서 수정 |
| `test` | 테스트 코드 수정 |
| `init` | 프로젝트 초기화 |

**Default to one file per commit, one concern per commit.** Split whenever describing the commit needs the word "및". The unit is one *concern*, which usually is one file — a rename across five files is still one commit.

Commit in dependency order so each commit compiles on its own: entity → repository → exception → service → DTO → controller → docs/tests/chores.

- Stage one unit at a time. **Never `git add -A` or `git add .`.**
- Read `git diff --staged` before committing.
- Never commit on `main`; work on `feat/<kebab>` or `fix/<kebab>` or `chore/<kebab>`.
- Never amend a pushed commit, never force-push, never `--no-verify`.
- Leave pre-existing unrelated modifications (currently `gradle/wrapper/*`, `gradlew*`) out of your commits, and mention them.

### Pull requests

Follow `.github/PULL_REQUEST_TEMPLATE.md`, written in Korean. Fill every section.

- **The template hardcodes `Closes #3`** — a leftover. Replace it with the real issue number or write `- 없음`, or merging will close an unrelated issue.
- **Only check a test checklist box if it is actually true.** Given the single `contextLoads()` test, "단위 테스트 작성 및 통과" is almost never true. An unchecked box with a stated reason is useful; a checked box that is false gets a defect merged.
- Put schema impact, unverified checks, and assumed answers to open product questions in 기타 참고사항.
- One PR per branch — update it, never open a second.

### Review comments

Each thread ends in exactly one of two states:

- **Applied** → commit, push, reply in Korean citing the short commit hash, then resolve the thread.
- **Declined** → reply with a concrete reason (out of scope / factually wrong / blocked by a project constraint / needs a team decision) and **leave the thread open** for the reviewer. Resolution means "handled"; a declined comment is not handled until the reviewer agrees.

Never apply a change you believe is wrong just to close a thread. Review comments are written by people — judge them, do not obey them blindly.

### Confirmation gates

| Action | Gate |
|--------|------|
| Commit | Only when asked — it is not implied by "make this change" |
| `git push`, `gh pr create` | Confirm with the user first |
| `gh pr review --approve` / `--request-changes` | Explicit request only |
| Force push, amend pushed commits, commit on `main` | Never |

## Skill files — keep both copies in sync

The same ten skill files exist twice, because Claude Code and Codex scan different directories:

```
.claude/skills/<name>/SKILL.md   ← Claude Code reads this
.codex/skills/<name>/SKILL.md    ← Codex auto-discovers this (verified, codex-cli 0.154.0)
```

Codex does **not** read `.claude/`, and Claude Code does not read `.codex/`. There is no shared location that both scan.

**When you change a skill, change both copies in the same commit.** If they drift, the two tools silently follow different rules — the worst possible failure mode, because nothing errors and the difference only shows up as inconsistent behavior weeks later.

To check they match:

```bash
diff -r .claude/skills .codex/skills && echo "동일함"
```

`.claude/agents/` has no `.codex/` counterpart: Codex does not load agent definitions from the repo, so copying them there would leave dead files. Agent definitions are Claude Code only.

## Reference files

Read these on demand; they are plain markdown. Skill paths are shown under `.claude/`; the identical copy under `.codex/` works the same.

| File | Contents |
|------|----------|
| `docs/PRODUCT.md` | Full product spec — both apps, business rules, 8 open product questions |
| `docs/DOMAIN-MODEL.md` | Aggregates, parcel state machine, rules hidden in queries, aggregates still to build |
| `docs/IMPLEMENTATION-STATUS.md` | Spec ↔ code gap map, current API surface, dependency order, 9 known defects |
| `.claude/skills/spring-kotlin-conventions/SKILL.md` | Full convention reference |
| `.claude/skills/spring-api-contract/SKILL.md` | Endpoint contract design and format |
| `.claude/skills/spring-jpa-domain/SKILL.md` | Entity behavior, query design, transactions, schema constraints |
| `.claude/skills/spring-security-guard/SKILL.md` | Filter chain, JWT, OAuth2 flow, access control |
| `.claude/skills/spring-qa-verify/SKILL.md` | Seven cross-boundary verification checks |
| `.claude/skills/git-commit-convention/SKILL.md` | Commit granularity in detail |
| `.claude/skills/github-pr-author/SKILL.md` | PR authoring in detail |
| `.claude/skills/github-review-responder/SKILL.md` | Review comment handling, `gh` commands for replying and resolving |
| `.claude/skills/github-review-commenter/SKILL.md` | What is worth commenting on in a review |

Codex auto-discovers the `.codex/skills/` copies as native skills, so it normally does not need to open these paths manually. Any agent can still read them directly — they are ordinary markdown.

## Working expectations

- When an implementation contradicts `docs/PRODUCT.md`, that is a finding to report, not something to quietly reconcile.
- When something could not be verified, say so explicitly rather than implying it passed.
- When a change needs a decision that is not yours — auth behavior, a schema rename, a new dependency — raise it instead of choosing silently.
