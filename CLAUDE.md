# CLAUDE.md

## Project

**TaekBaeWatShong (택배왔숑)** — a school parcel management service built to prevent parcels from being left unattended or lost. Students and teachers order parcels to the school; this server tracks each parcel's owner, arrival, storage zone, and pickup, and applies pressure to unclaimed ones.

This repository is the **backend** for two separate frontends:

- **Client** — every teacher and student who orders parcels. Register a parcel, see its status and storage zone, pick it up by barcode scan, report it lost, chat with an admin.
- **Admin** — one 학생생활안전부 teacher, 학생회 학생생활안전부 students, the VOID team, and delivery drivers. See all parcels, assign zones, review lost reports, scan arrivals, manage driver QR codes.

Stack: Spring Boot 3.2.5, Kotlin 1.9.23, Java 21, JPA + MySQL, Spring Security with Google OAuth2 + JWT. Built by **VOID** (`VOID-GSM`).

**A large part of the product is not built yet.** Before planning any feature, check [docs/IMPLEMENTATION-STATUS.md](docs/IMPLEMENTATION-STATUS.md) — it maps every spec item to its current state and lists nine known defects in the existing code.

## Language rule

**Skill and agent definition files under `.claude/` are written in English. Everything else produced for a human is written in Korean.**

That includes: every response to the user, commit messages (Korean subject after the English type prefix), PR titles and bodies, code review comments, review thread replies, and user-facing exception messages. Code identifiers, package names, and log output stay English.

## Documentation

| Document | Contents |
|----------|----------|
| [docs/PRODUCT.md](docs/PRODUCT.md) | Product specification — both apps, all features, business rules, open product questions |
| [docs/DOMAIN-MODEL.md](docs/DOMAIN-MODEL.md) | Aggregates, the parcel state machine, rules hidden in queries, aggregates still to build |
| [docs/IMPLEMENTATION-STATUS.md](docs/IMPLEMENTATION-STATUS.md) | Spec ↔ code gap map, existing API surface, dependency order, known defects |

Keep these current. When an implementation contradicts PRODUCT.md, that is a finding to report, not something to quietly reconcile.

## Harness: TaekBaeWatShong backend

**Goal:** Deliver backend features through a five-member agent team, so that layer-boundary defects are caught by cross-verification rather than in production.

**Trigger:** For any non-trivial backend work here — adding or changing an endpoint, adding a domain aggregate, changing an entity or query, wiring auth or roles, or auditing existing code — use the `parcel-server-orchestrator` skill. Follow-up requests ("rerun QA", "fix what QA found", "only redo the security part", "extend what we built") go through the same skill; it reuses the existing `_workspace/`. Simple questions and single-line edits need no orchestration.

**Always applies:** Read the `spring-kotlin-conventions` skill before writing or reviewing any Kotlin file here, even outside an orchestrated run.

## Delivery workflow

Commits, PRs, and review handling are owned by the `git-workflow-engineer` agent. Use its skills directly for standalone delivery work, or let the orchestrator reach its delivery phase:

| Task | Skill |
|------|-------|
| 커밋 | `git-commit-convention` — `<type>: <한국어 요약>`, one concern per commit |
| PR 생성 | `github-pr-author` — `.github/PULL_REQUEST_TEMPLATE.md`, Korean |
| PR 리뷰 / 코멘트 작성 | `github-review-commenter` |
| 리뷰 코멘트 반영·답글·완료 처리 | `github-review-responder` |

Commit types: `feat`, `fix`, `chore`, `refactor`, `docs`, `test`, `init`.

**Confirmation gates:** committing is not implied by "make this change". `git push` and `gh pr create` are confirmed with the user first. `gh pr review --approve` / `--request-changes` only on explicit request. Never force-push a branch with an open PR, never commit on `main`.

**Review threads** end in one of two states — applied, with the commit hash cited in a Korean reply and the thread resolved; or declined, with a clear reason and the thread left **open** for the reviewer.

**Change log:**

| Date | Change | Target | Reason |
|------|--------|--------|--------|
| 2026-09-10 | Initial build — 5 agents, 5 skills + orchestrator | all | — |
| 2026-09-10 | Added product/domain/status docs and project context | `CLAUDE.md`, `docs/` | Agents were designing against code alone with no product spec; the spec ↔ code gap was invisible |
| 2026-09-10 | Added delivery workflow — `git-workflow-engineer` + 4 skills, orchestrator Phase 5 | `agents/`, `skills/`, orchestrator | Work stopped at "code done"; commit granularity, PR template, and review-comment handling were unspecified |
| 2026-09-10 | Documented the English-skills / Korean-output language rule | `CLAUDE.md`, all skills | Output language was ambiguous once skill files were written in English |
