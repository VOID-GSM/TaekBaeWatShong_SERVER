---
name: git-commit-convention
description: "Split work into small single-purpose commits and write them in this project's convention — English type prefix, short Korean subject, one file or one concern per commit. Use this skill whenever committing anything in this repository, whenever asked to '커밋해줘' / 'commit this' / 'split these changes into commits', when a working tree has accumulated mixed changes that need separating, and before opening a PR. Covers the seven allowed types (feat, fix, chore, refactor, docs, test, init) and how to decide commit boundaries."
---

# Commit Convention

How to commit in this repository. All commit messages are written in **Korean** after an English type prefix — see the language rule at the bottom.

## Message format

```
<type>: <한국어 요약>
```

One line. No scope parentheses, no body, no issue reference in the subject. This matches every commit in the repository's history.

| Type | Meaning |
|------|---------|
| `feat` | 새로운 기능에 대한 커밋 |
| `fix` | 버그 수정에 대한 커밋 |
| `chore` | 자잘한 작업에 대한 커밋 |
| `refactor` | 코드 리팩토링에 대한 커밋 |
| `docs` | 문서 수정에 대한 커밋 |
| `test` | 테스트 코드 수정에 대한 커밋 |
| `init` | 프로젝트 초기화 |

Examples of the target style:

```
feat: axios 인스턴스 설정
fix: input border gray 색상으로 변경
feat: 중복 운송장 등록 예외 처리 추가
refactor: 불필요한 도착 알림 이벤트 로직 삭제
docs: 제품 스펙 문서 추가
```

## Commit granularity — the important part

**Default to one file per commit, one main function per commit, and a short subject.** A reviewer should be able to read the subject and know exactly which file changed and why, without opening the diff.

This matters more than message wording. A commit touching eight files with the subject `feat: 택배 기능 추가` is unreviewable — a reviewer cannot comment on one part without the whole thing being in scope, and it cannot be reverted piecewise.

### How to decide a boundary

Split when the answer to "what did this commit do?" needs the word **and**.

| Changes | Commits |
|---------|---------|
| Added `Report` entity + `ReportRepository` + `ReportService` | 3 commits, one per file |
| Added a field to an entity and used it in one service method | 2 commits — the field, then the usage |
| Renamed a variable across 5 files, nothing else | 1 commit (`refactor:`) — it is one concern |
| Fixed a bug and reformatted the file while there | 2 commits — the fix, then the formatting |
| Added a controller method + its request DTO + its response DTO | 3 commits — DTOs first, then the controller that uses them |

The rename case is the exception that shows the real rule: the unit is **one concern**, which usually *is* one file, but not always. Never bundle two concerns because they touch the same file, and never split one concern across commits in a way that leaves an intermediate commit broken.

### Ordering

Commit in dependency order, so every commit compiles on its own:

1. entity / enum
2. repository
3. exception class
4. service
5. DTOs
6. controller
7. docs, tests, chores

A commit that does not compile makes `git bisect` useless and blocks anyone from checking out that point.

## Procedure

1. **Look at what actually changed** before staging anything:
   ```bash
   git status --short
   git diff
   git diff --staged
   ```
   Never commit based on an assumption about what is in the working tree. If a file was modified by something other than this session's work, that is a separate commit with its own message — or a question for the user.

2. **Stage one unit at a time** — `git add <path>`, never `git add -A` or `git add .`. Bulk staging is how unrelated files end up in a commit.

3. **Check the staged content matches the message you are about to write:**
   ```bash
   git diff --staged --stat
   ```

4. **Commit**, using a heredoc so the Korean subject survives shell quoting:
   ```bash
   git commit -m "$(cat <<'EOF'
   feat: 분실 신고 엔티티 추가
   EOF
   )"
   ```

5. Repeat for each unit.

## Rules

- **Never commit on `main`.** The default branch is `main`; work happens on `feat/<kebab-topic>` or `fix/<kebab-topic>`. If the current branch is `main`, create a branch first and say so.
- **Never `git add -A` / `git add .`.**
- **Never amend a pushed commit,** and never `--force` push. Prefer a new commit.
- **Never skip hooks** (`--no-verify`) or bypass signing.
- **Do not commit unrelated pre-existing changes.** The working tree currently carries modifications to `gradle/wrapper/*` and `gradlew*` from a previous session — leave them alone unless the user asks, and mention them rather than silently sweeping them into a commit.
- **Do not commit secrets.** `application.yml` uses env placeholders; keep it that way. Check any new config file before staging it.
- **Ask before committing when the user did not ask you to commit.** Committing is not part of "make this change" unless stated.

## Subject writing

Write what changed, in Korean, concretely and briefly:

| Bad | Good | Why |
|-----|------|-----|
| `feat: 기능 추가` | `feat: 미수령 택배 TOP3 조회 API 추가` | names the thing |
| `fix: 버그 수정` | `fix: 방치 일수 계산을 시간 기준으로 변경` | names the behavior |
| `chore: 수정` | `chore: gitignore에 _workspace 추가` | names the file and the change |
| `feat: 택배 등록 API 추가 및 예외 처리 추가 및 테스트 작성` | three separate commits | the word "및" appears twice |

Keep it under roughly 50 characters where the Korean allows. Use the imperative-noun style the existing history uses (`추가`, `수정`, `삭제`, `변경`), not sentences.

## Language rule

**Skill and agent definition files are written in English. Everything produced for a human — commit messages, PR bodies, review comments, replies, and every response to the user — is written in Korean.** A commit message is human-facing output, so it is Korean after the English type prefix.
