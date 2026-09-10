# CLAUDE.md

**Read [AGENTS.md](AGENTS.md) first.** It holds the shared, tool-agnostic instructions for this repository — project overview, the language rule, coding conventions, the seven codebase-specific traps, verification commands, and the commit / PR / review workflow. Everything there applies to Claude Code too and is not repeated here.

This file adds only what is specific to Claude Code: the agent-team harness.

## Harness: TaekBaeWatShong backend

**Goal:** Deliver backend features through a six-member agent team, so that layer-boundary defects are caught by cross-verification rather than in production.

**Trigger:** For any non-trivial backend work here — adding or changing an endpoint, adding a domain aggregate, changing an entity or query, wiring auth or roles, or auditing existing code — use the `parcel-server-orchestrator` skill. Delivery requests ("커밋하고 PR 올려줘", "리뷰 반영해줘") and follow-ups ("rerun QA", "fix what QA found", "only redo the security part") go through the same skill; it reuses the existing `_workspace/`. Simple questions and single-line edits need no orchestration.

**Always applies:** Read the `spring-kotlin-conventions` skill before writing or reviewing any Kotlin file here, even outside an orchestrated run.

### Team

| Agent | Role | Skills |
|-------|------|--------|
| `spring-api-designer` | Endpoint contract, DTO shapes, failure table | `spring-api-contract` |
| `spring-domain-engineer` | Entity, repository, service, exceptions | `spring-jpa-domain` |
| `spring-web-engineer` | Controller, request/response DTOs | `spring-api-contract` |
| `spring-security-engineer` | SecurityConfig, role gates, ownership | `spring-security-guard` |
| `spring-qa-inspector` | Seven cross-boundary checks | `spring-qa-verify` |
| `git-workflow-engineer` | Commits, PR, review comments and replies | `git-commit-convention`, `github-pr-author`, `github-review-commenter`, `github-review-responder` |

All six read `spring-kotlin-conventions`. Every agent runs on `model: "opus"`. `git-workflow-engineer` joins only at the delivery phase, so it cannot commit half-finished work.

### Other tools

`AGENTS.md` covers Codex and anything else reading that convention. Shared rules belong there; harness wiring belongs here.

**The ten skill files are duplicated at `.codex/skills/`** because Codex auto-discovers that directory and does not read `.claude/`. When you change a skill, change both copies in the same commit — if they drift, the two tools silently follow different rules. Check with `diff -r .claude/skills .codex/skills`.

Agent definitions are not duplicated: Codex does not load them from the repo.

**Change log:**

| Date | Change | Target | Reason |
|------|--------|--------|--------|
| 2026-09-10 | Initial build — 5 agents, 5 skills + orchestrator | all | — |
| 2026-09-10 | Added product/domain/status docs and project context | `CLAUDE.md`, `docs/` | Agents were designing against code alone with no product spec; the spec ↔ code gap was invisible |
| 2026-09-10 | Added delivery workflow — `git-workflow-engineer` + 4 skills, orchestrator Phase 5 | `agents/`, `skills/`, orchestrator | Work stopped at "code done"; commit granularity, PR template, and review-comment handling were unspecified |
| 2026-09-10 | Documented the English-skills / Korean-output language rule | `CLAUDE.md`, all skills | Output language was ambiguous once skill files were written in English |
| 2026-09-10 | Added `AGENTS.md`; `CLAUDE.md` now defers to it | `AGENTS.md`, `CLAUDE.md` | Codex support requested; shared rules were Claude-only and would have drifted if duplicated |
