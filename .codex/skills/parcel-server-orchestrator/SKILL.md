---
name: parcel-server-orchestrator
description: "Coordinates the TaekBaeWatShong backend agent team (API designer, domain engineer, web engineer, security engineer, QA inspector, git workflow engineer) to build, extend, review, and ship a feature on this Spring Boot Kotlin server. Use this skill for ANY non-trivial backend work here: adding or changing an endpoint, adding a domain/aggregate, changing an entity or query, wiring auth or roles, or auditing existing code — and for delivery: committing, opening a PR, reviewing a PR, or processing review comments. ALSO use it for follow-up work — 'redo the contract', 'rerun QA', 'update the parcel feature', 'fix what QA found', 'improve the previous result', 'only redo the security part', 'extend what we built', 're-verify', 'PR 올려줘', '리뷰 반영해줘' — the workspace from a previous run is reused rather than restarted. Skip it only for a single-line edit or a question that needs no code change."
---

# TaekBaeWatShong Backend Orchestrator

Coordinates a five-member agent team to deliver a backend feature on this Spring Boot 3.2.5 / Kotlin 1.9.23 server.

## Execution mode: agent team

The layers here are tightly coupled — a DTO shape decision constrains the entity, an access rule constrains both the controller and the service, and the highest-value defects (principal type, exception-to-status mapping) live between two agents' files. Members must therefore be able to challenge each other directly rather than reporting through a leader, and QA must run *while* implementation proceeds instead of after it. That is what an agent team provides and sub-agents do not.

The architecture is **pipeline into fan-out, with a producer-reviewer loop**: contract first, then three engineers in parallel, with the QA inspector cross-checking each piece as it lands.

## Team composition

| Member | subagent_type | Role | Skills | Output |
|--------|--------------|------|--------|--------|
| `api-designer` | `spring-api-designer` | Endpoint contract, DTO shapes, failure table | `spring-api-contract`, `spring-kotlin-conventions` | `_workspace/01_api-designer_contract.md` |
| `domain-engineer` | `spring-domain-engineer` | Entity, repository, service, exceptions | `spring-jpa-domain`, `spring-kotlin-conventions` | `_workspace/02_domain_notes.md` |
| `web-engineer` | `spring-web-engineer` | Controller, request/response DTOs | `spring-api-contract`, `spring-kotlin-conventions` | `_workspace/03_web_notes.md` |
| `security-engineer` | `spring-security-engineer` | SecurityConfig, role gates, ownership | `spring-security-guard`, `spring-kotlin-conventions` | `_workspace/03_security_notes.md` |
| `qa-inspector` | `spring-qa-inspector` | Seven cross-boundary checks | `spring-qa-verify`, `spring-kotlin-conventions` | `_workspace/04_qa_report.md` |
| `git-workflow` | `git-workflow-engineer` | Commits, PR, review comments and replies | `git-commit-convention`, `github-pr-author`, `github-review-responder`, `github-review-commenter` | `_workspace/05_delivery_notes.md` |
| leader (you) | — | Coordination, integration, reporting | this skill | final summary |

`git-workflow` joins the team only in Phase 5 (delivery). Creating it up front would let it commit half-finished work, so spawn it after Phase 4's build and reconciliation — or spawn it alone when the user asks only for commit/PR/review handling with no implementation work.

Every `Agent` / `TeamCreate` member is spawned with `model: "opus"`.

## Workflow

### Phase 0: Context check

Determine the run mode before doing anything else.

1. Check whether `_workspace/` exists in the repository root.
2. Branch:
   - **No `_workspace/`** → **initial run**. Continue to Phase 1.
   - **Exists + the user asks for a partial change** ("only redo the security part", "fix what QA found", "rerun QA") → **partial re-run**. Skip Phase 1, keep the workspace, and in Phase 2 create only the members needed for that slice. Pass each one the path to its own previous output file and instruct it to revise rather than regenerate.
   - **Exists + the user brings a new, unrelated feature** → **fresh run**. Move `_workspace/` to `_workspace_<YYYYMMDD_HHMMSS>/`, then continue to Phase 1.
3. On a partial re-run, always include `qa-inspector` — any change to shipped code invalidates part of the previous report, and re-verification is cheap next to a missed regression.

### Phase 1: Preparation

0. Read `docs/PRODUCT.md` and `docs/IMPLEMENTATION-STATUS.md`. Locate the request in the status document's gap map and note whether it depends on an unanswered open product question or on a piece listed as MISSING. If it depends on missing work, say so to the user before spawning a team — building on a gap produces a feature that cannot be finished.
1. Read the request and identify: which aggregate (`parcel`, `user`, or new), which endpoints, and whether auth rules change.
2. Create `_workspace/`.
3. Write the request and any user-supplied constraints to `_workspace/00_input/request.md`.
4. Read the target aggregate's existing source so the team's instructions can name real files rather than describing them abstractly.
5. Record the starting git state: `git rev-parse HEAD` and `git status --short` into `_workspace/00_input/baseline.md`, so the change set is recoverable later.

### Phase 2: Team formation

```
TeamCreate(
  team_name: "taekbae-backend",
  members: [
    { name: "api-designer",      agent_type: "spring-api-designer",      model: "opus",
      prompt: "Design the endpoint contract for <feature>. Load the spring-api-contract and spring-kotlin-conventions skills first. Read the existing <agg> aggregate before designing. Write _workspace/01_api-designer_contract.md, then message web-engineer, domain-engineer and security-engineer that it is ready." },
    { name: "domain-engineer",   agent_type: "spring-domain-engineer",   model: "opus",
      prompt: "Implement entity, repository, service and exception changes for <feature>. Load spring-jpa-domain and spring-kotlin-conventions. Wait for the contract, then publish your service method signatures to web-engineer immediately — before finishing the implementation. Write _workspace/02_domain_notes.md including a ## Schema impact section." },
    { name: "web-engineer",      agent_type: "spring-web-engineer",      model: "opus",
      prompt: "Implement controller and DTOs for <feature>. Load spring-api-contract and spring-kotlin-conventions. Start DTO work from the contract as soon as it lands; controller work once domain-engineer publishes signatures. Write _workspace/03_web_notes.md." },
    { name: "security-engineer", agent_type: "spring-security-engineer", model: "opus",
      prompt: "Own access control for <feature>. Load spring-security-guard and spring-kotlin-conventions. Verify SecurityConfig matchers, @PreAuthorize expressions, ownership checks and principal types. Write the access matrix to _workspace/03_security_notes.md." },
    { name: "qa-inspector",      agent_type: "spring-qa-inspector",      model: "opus",
      prompt: "Run the seven cross-boundary checks from spring-qa-verify incrementally — as each engineer reports a piece complete, not at the end. Write _workspace/04_qa_report.md with a mandatory Not-verified section." }
  ]
)
```

Then register the work:

```
TaskCreate(tasks: [
  { title: "Design endpoint contract",        assignee: "api-designer" },
  { title: "Entity + state transitions",      assignee: "domain-engineer",   depends_on: ["Design endpoint contract"] },
  { title: "Repository queries",              assignee: "domain-engineer",   depends_on: ["Entity + state transitions"] },
  { title: "Service methods + exceptions",    assignee: "domain-engineer",   depends_on: ["Repository queries"] },
  { title: "Request/response DTOs",           assignee: "web-engineer",      depends_on: ["Design endpoint contract"] },
  { title: "Controller methods",              assignee: "web-engineer",      depends_on: ["Request/response DTOs"] },
  { title: "SecurityConfig + role gates",     assignee: "security-engineer", depends_on: ["Design endpoint contract"] },
  { title: "Ownership checks",                assignee: "security-engineer", depends_on: ["Service methods + exceptions"] },
  { title: "Incremental cross-checks",        assignee: "qa-inspector" },
  { title: "Final verification report",       assignee: "qa-inspector",      depends_on: ["Controller methods", "Ownership checks"] }
])
```

Note that "Request/response DTOs" depends only on the contract, not on the domain work — that is what lets three engineers run in parallel instead of in sequence.

### Phase 3: Implementation

**Execution: members self-coordinate.** They claim tasks from the shared list and work independently. Required communication:

| From | To | What |
|------|----|------|
| `api-designer` | all three engineers | contract ready; access rules; persistence implications |
| `domain-engineer` | `web-engineer` | service method signatures, **as soon as fixed** — not when implemented |
| `domain-engineer` | `security-engineer` | where an ownership check is needed, with owner field and id types |
| `web-engineer` | `security-engineer` | every new endpoint path and its intended access rule |
| `web-engineer` | `domain-engineer` | a needed service method that is missing or the wrong shape |
| any engineer | `qa-inspector` | "this piece is complete" — the trigger for an incremental check |
| `qa-inspector` | both sides of a boundary | findings with `file:line` on each side |
| any member | `api-designer` | the contract is unimplementable or contradicts the entity |

**Leader monitoring:** you receive an idle notification when a member finishes. Check `TaskGet` for overall progress. Intervene when a member is blocked for a reason it cannot resolve — a missing decision, a disputed finding, a contradiction between two members.

**Do not let QA wait until the end.** If `qa-inspector` has not reported anything by the time two engineers have completed a task, message it directly.

### Phase 4: Integration

1. Confirm all tasks are complete via `TaskGet`.
2. Read all five workspace files.
3. Run the build yourself — do not take a member's word for it:
   ```
   ./gradlew compileKotlin
   ./gradlew test
   ```
4. Reconcile: every BLOCKER in the QA report is either fixed or explicitly accepted by the user. If any remain open, that goes at the top of your summary, not buried.
5. Collect the `## Schema impact` section from the domain notes. Any manual SQL required under `ddl-auto: update` is surfaced prominently — it is the one thing that will not fail loudly.
6. Produce the final summary (format below).

### Phase 5: Delivery

Runs only when the user wants the work committed, opened as a PR, or reviewed. Skip it entirely for an exploratory or audit-only run.

1. Spawn `git-workflow` into the team now — not earlier, so it cannot commit half-finished work.
2. Hand it the QA report. Its open blockers and its "not verified" list feed straight into the PR's 테스트 체크리스트 and 기타 참고사항.
3. It commits in small single-purpose units, in dependency order, following `git-commit-convention`.
4. **Confirm with the user before pushing and before `gh pr create`** — both are outward-facing. Show the PR title and body first.
5. When review comments arrive, `git-workflow` judges each one. Substantive changes are **routed back to the owning engineer**, not patched at delivery time, and `qa-inspector` re-checks the affected boundary before the reply goes out.
6. Every thread ends applied-with-a-hash or declined-with-a-reason. Declined threads are **not** resolved.

### Phase 6: Teardown

1. Message members to stand down.
2. `TeamDelete`.
3. **Preserve `_workspace/`** — it is the audit trail and the input for the next partial re-run. Do not delete it.
4. Report to the user, in Korean.

## Data flow

```
                  request
                     │
              [api-designer] ──── 01_contract.md
                     │
        ┌────────────┼────────────┐
        ↓            ↓            ↓
 [domain-eng]   [web-eng]   [security-eng]
   02_notes      03_web      03_security
        │            │            │
        └──── SendMessage ────────┘
                     │  (signatures, paths, ownership rules)
                     ↓
              [qa-inspector] ──── 04_qa_report.md
                     │  (findings routed back to both sides)
                     ↓
                 [leader]
              build + summary
```

Coordination runs through the shared task list, artifacts through `_workspace/` files, and real-time exchanges through `SendMessage`.

## Error handling

| Situation | Response |
|-----------|----------|
| A member stalls or errors | Leader gets the idle notification → `SendMessage` to check state → restart once. On a second failure, reassign its remaining tasks and note the gap in the summary. |
| `./gradlew test` fails on missing DB/env vars | Not a code failure. Record the exact output, mark context-dependent checks "not verified", proceed with static verification. **Never report this as a pass or as a defect.** |
| QA and an engineer disagree | Leader reads both cited files and decides. Do not let the loop repeat more than twice. |
| Contract contradicts the entity | The entity wins — it is the runtime truth. `api-designer` updates the contract; note the correction. |
| Implementation contradicts `docs/PRODUCT.md` | A finding, not something to reconcile silently. Report it and update the doc only with the user's agreement. |
| Feature depends on an open product question | Implement the conservative reading, label it, and surface the question in the final summary rather than blocking the whole run. |
| A schema change is unsafe under `ddl-auto: update` | Stop that task. Surface the required manual SQL to the user before proceeding. Never report a rename or type change as "done". |
| A change would weaken existing access | Halt and confirm with the user, stating exactly who gains access. |
| Two members edit the same file | Assign the file to one owner (`web-engineer` owns controllers, including their `@PreAuthorize` annotations; `security-engineer` specifies the expression to apply). |
| More than 3 open BLOCKERs at Phase 4 | Do not present the work as complete. Report the blockers and ask how to proceed. |

## Final summary format

```markdown
## <feature> — complete | blocked

**Changed:** <n> files across <layers>
**Build:** compileKotlin PASS | test PASS/NOT RUN (<reason>)
**QA:** <n> blockers, <n> major, <n> minor — <n> open

### Endpoints
| Verb | Path | Access | Status |

### Open items
- <blocker or product decision needed>

### Schema impact
- <manual SQL required, or "none — additive only">

### Not verified
- <checks the environment prevented>
```

The last two sections are mandatory. On this project the schema and the untested paths are exactly where silent failures accumulate.

## Test scenarios

**Normal flow.** User asks to add `GET /parcel/{parcelId}` returning a single parcel. Phase 0 finds no `_workspace/` → initial run. `api-designer` writes a contract with a 404 row for a missing id and a 403 row for a non-owner. Three engineers run in parallel; `domain-engineer` messages the `findById` service signature early so `web-engineer` builds the controller without waiting. `qa-inspector` cross-checks the DTO nullability against `Parcel`'s states and flags that the controller declares `UserPrincipal` while the JWT filter sets `User`. `web-engineer` and `security-engineer` are both notified; the leader escalates it as a product decision since it affects existing endpoints. Leader runs the build, reports one open blocker.

**Error flow.** `domain-engineer` stops mid-task with a compilation error. Leader receives the idle notification, messages it, restarts once. It fails again on the same error. Leader reassigns the remaining repository task, records "repository query for zone filtering not implemented" in the summary, and lets `qa-inspector` verify what does exist. The final summary reports the feature as **blocked**, not complete, and names the missing piece.

## Partial re-run examples

| User says | Members created | Workspace |
|-----------|----------------|-----------|
| "rerun QA" | `qa-inspector` only | reused; report appended with a dated section |
| "fix what QA found" | the cited engineers + `qa-inspector` | reused; each engineer reads its own prior notes |
| "only redo the security part" | `security-engineer` + `qa-inspector` | reused |
| "the contract was wrong about X" | `api-designer` + affected engineers + `qa-inspector` | reused |
| "add a new endpoint for Y" | full team | reused, appended |
| "build the notification feature" (unrelated) | full team | archived to `_workspace_<timestamp>/`, fresh one created |
| "커밋하고 PR 올려줘" | `git-workflow` only | reused |
| "리뷰 코멘트 반영해줘" | `git-workflow` + the cited engineers + `qa-inspector` | reused |

## Language rule

**Skill and agent definition files are written in English. Everything produced for a human is written in Korean** — commit messages (after the English type prefix), PR titles and bodies, review comments, thread replies, workspace notes intended for the team, and every response to the user, including this skill's final summary.
