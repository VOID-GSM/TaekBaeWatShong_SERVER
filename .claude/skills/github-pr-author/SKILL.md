---
name: github-pr-author
description: "Create a pull request on the VOID-GSM/TaekBaeWatShong_SERVER repository following .github/PULL_REQUEST_TEMPLATE.md, written in Korean. Use this skill whenever asked to 'PR 만들어줘' / 'open a PR' / 'PR 올려줘', after finishing a feature branch, or when a branch is ready for review. Covers filling every template section honestly, the test checklist that must not be checked without evidence, the hardcoded 'Closes #3' issue line that needs replacing, and the push confirmation gate."
---

# Pull Request Authoring

How to open a PR on this repository. The body is written in **Korean**, following the repository's template exactly.

## Before anything: confirm

Opening a PR is outward-facing — it notifies reviewers and publishes the branch. **Confirm with the user before pushing and before creating the PR**, unless they explicitly asked for the PR in this turn. Show the intended title and body first.

Never create a PR targeting `main` from `main`. Never force-push a branch that has an open PR.

## Pre-flight checks

```bash
git branch --show-current                       # must not be main
git status --short                              # working tree should be clean
git log --oneline main..HEAD                    # commits that will be in the PR
gh pr list --head "$(git branch --show-current)" # is a PR already open?
```

If a PR already exists for this branch, **do not create a second one** — push the new commits and report that the existing PR was updated.

If the working tree is dirty, commit or explain what is left over. The `gradle/wrapper/*` and `gradlew*` modifications currently sitting in the tree are pre-existing; leave them out.

Then push:

```bash
git push -u origin "$(git branch --show-current)"
```

## The template

`.github/PULL_REQUEST_TEMPLATE.md` has six sections. Fill every one — an unfilled placeholder tells a reviewer the author did not read their own PR.

```markdown
## 📌 개요
- <작업 요약과 목적, 1~3줄>

## 🔗 관련 이슈
- Closes #<번호>

## ✨ 주요 작업 내용
- [x] <작업 1>
- [x] <작업 2>
- [x] <작업 3>

## 🧪 테스트 체크리스트
- [ ] 단위 테스트 작성 및 통과 여부
- [ ] API 동작 및 응답값 검증
- [ ] 예외 처리 및 권한 검증 확인

## 📸 스크린샷 / 시연 (선택)
<!-- API 응답 결과나 Postman 스크린샷 등이 있다면 첨부해주세요 -->

## 💬 기타 참고사항
<!-- 리뷰어가 알아야 할 주의사항이나 논의하고 싶은 내용이 있다면 적어주세요 -->
```

### 📌 개요

What changed and why, in one to three lines. Describe the outcome, not the file list — the file list is the diff's job.

### 🔗 관련 이슈

**The template hardcodes `Closes #3`.** That is a leftover, not a real reference. Replace it with the actual issue number, or if there is no issue, write `- 없음` rather than leaving `#3` to auto-close an unrelated issue when the PR merges.

Check for a matching issue before assuming there is none:

```bash
gh issue list --state open
```

### ✨ 주요 작업 내용

One checked box per meaningful change. Derive them from the commit list:

```bash
git log --oneline main..HEAD
```

Commits are already one-concern each (see `git-commit-convention`), so they map almost directly onto checklist items. Merge trivially related ones — three commits adding an entity, repository, and service become `- [x] 분실 신고 엔티티/리포지토리/서비스 추가`.

Check the boxes: these are things that were done, and they are done.

### 🧪 테스트 체크리스트

**Only check a box if it is actually true.** This is the section most likely to be filled in dishonestly, and on this project it is almost never fully true:

- `단위 테스트 작성 및 통과 여부` — the repository has exactly one test, `contextLoads()`. Unless this PR added real tests, leave it **unchecked**.
- `API 동작 및 응답값 검증` — check only if the endpoint was actually called and the response inspected. Reading the code is not verification.
- `예외 처리 및 권한 검증 확인` — check only if the failure paths and role gates were actually exercised or statically cross-checked by QA.

When a box is left unchecked, say why in 기타 참고사항. An unchecked box with an explanation is useful information; a checked box that is false is a lie a reviewer will act on.

Remember that `./gradlew build` here runs only `contextLoads()` and needs a live MySQL plus six environment variables. If it could not run, that belongs in the notes.

### 📸 스크린샷 / 시연

Optional. If there is no screenshot, write `- 해당 없음` instead of leaving the HTML comment bare.

### 💬 기타 참고사항

The most valuable section. Put here:

- Unchecked test boxes and their reason
- Known defects the PR does **not** fix but touches
- Anything requiring a manual step — especially **schema changes**, since `ddl-auto: update` will not apply renames, type changes, or removals
- Open product questions the implementation had to assume an answer for
- Anything the reviewer should look at closely

## Creating the PR

Write the body to a temp file rather than passing it inline — Korean text plus markdown through shell quoting is fragile:

```bash
gh pr create \
  --base main \
  --head "$(git branch --show-current)" \
  --title "feat: 분실 신고 기능 추가" \
  --body-file /path/to/scratchpad/pr-body.md
```

Title format matches the commit convention: `<type>: <한국어 요약>`. Use the type of the PR's dominant change.

After creation, report the PR URL to the user.

## Updating an existing PR

```bash
git push
gh pr edit <번호> --body-file /path/to/scratchpad/pr-body.md
```

When new commits change what the PR does, update 주요 작업 내용 and the test checklist too. A PR body that describes only the first push is worse than none.

## Rules

- Never open a PR from `main`.
- Never force-push a branch with an open PR.
- Never check a test checklist box without evidence.
- Never leave `Closes #3` unless issue 3 is genuinely the target.
- One PR per branch. Update, do not duplicate.
- Do not include secrets, tokens, or `.env` contents in the body.

## Language rule

**Skill and agent definition files are written in English. Everything produced for a human — PR titles, PR bodies, checklists, review comments, replies, and every response to the user — is written in Korean.**
