---
name: github-review-commenter
description: "Review a pull request on this repository and leave Korean line comments — deciding what is actually worth commenting on rather than flagging everything. Use this skill when asked to 'PR 리뷰해줘' / '코멘트 남겨줘' / 'review this PR' / '코드 리뷰 부탁', or when reviewing a teammate's branch before merge. Covers the worth-commenting bar, this codebase's high-value defect patterns, comment phrasing in Korean, and posting line comments and reviews through gh."
---

# Review Comment Authoring

How to review a PR here and leave comments worth reading. Comments are written in **Korean**.

## The bar for leaving a comment

Every comment costs the author time to read, judge, and answer. A review with twenty comments gets skimmed; a review with four gets acted on. So the question for each observation is not "is this true?" but **"would the author change the code because of this?"**

Leave a comment when the answer is yes:

| Leave a comment | Do not leave a comment |
|-----------------|------------------------|
| The code is wrong for a concrete input | It differs from how you would have written it |
| A convention in this codebase is violated | A general best practice that this codebase does not follow anywhere |
| A failure path is unhandled or returns the wrong status | Naming that is merely not your preference |
| A security or access rule is missing or wrong | Formatting a linter would catch — there is no linter here, but this is still noise |
| A boundary between layers disagrees | Something already flagged in another comment |
| Something is genuinely unclear and you need an answer | A restatement of what the diff obviously does |

If an observation is real but minor, bundle several into one summary comment rather than scattering line comments.

**Praise is allowed but must be specific.** "LGTM" adds nothing. Naming what was done well helps a teammate repeat it.

## What to look for in this codebase

Ordered by how often they actually break things here. See the `spring-qa-verify` skill for the full method.

1. **`@AuthenticationPrincipal` type** — `JwtAuthenticationFilter` sets the raw `User`; OAuth2 sets `UserPrincipal`. A mismatch injects `null`, so it surfaces as an NPE, not a 401. Highest-value single check.
2. **Missing `@Transactional` on a write method.** The class default is `readOnly = true`; a write without the override is silently discarded — no exception, no log.
3. **`ROLE_` prefix inside `hasRole` / `hasAnyRole`.** Fails closed, so it will not show up in testing.
4. **Exceptions with no `@ResponseStatus`,** or a `@ResponseStatus` disagreeing with the `status` property. With no `@RestControllerAdvice`, the thrown type *is* the status; a bare `RuntimeException` is a 500.
5. **N+1** — a DTO `from()` factory dereferencing a `LAZY` association in a list endpoint.
6. **Missing `OrderBy`** on a collection query. MySQL row order is otherwise unspecified.
7. **Unsafe schema change under `ddl-auto: update`** — a rename orphans the old column, a non-null addition fails on a populated table.
8. **Entity returned from a controller** instead of a DTO.
9. **A new public endpoint not added to `SecurityConfig`'s permitAll list**, or a sensitive one caught by the `/oauth2/**` and `/login/**` wildcards.
10. **Bare array response** instead of a wrapped list object.

## Writing the comment

Structure: what is wrong → why it matters → what to do. Concrete beats general.

**좋은 예:**

```
`registerParcel`이 쓰기 메서드인데 `@Transactional`이 없습니다.
클래스에 `readOnly = true`가 걸려 있어서 저장이 조용히 무시됩니다.
메서드에 `@Transactional`을 추가해주세요.
```

```
`hasRole('ROLE_ADMIN')`은 매칭되지 않습니다.
권한이 이미 `ROLE_${user.role}` 형태로 만들어지고 `hasRole`이 접두사를 다시 붙여서
`ROLE_ROLE_ADMIN`이 됩니다. `hasAnyRole('ADMIN')`으로 바꿔주세요.
```

```
이 목록 API에서 N+1이 발생합니다.
`ParcelResponse.from`이 `parcel.owner.name`을 읽는데 `owner`가 LAZY라
택배 수만큼 추가 쿼리가 나갑니다. 리포지토리에 `JOIN FETCH p.owner`를 추가하는 게 좋겠습니다.
```

**나쁜 예:**

- `이거 좀 이상한 것 같아요` — 무엇이, 왜인지 없음
- `트랜잭션 처리 필요` — 어디에 왜 필요한지 없음
- `@Valid 붙여주세요` — 이 프로젝트엔 validation 의존성이 없어서 동작하지 않음

Tone: polite 해요체, direct about the problem, not about the person. Write `이 메서드는 ~합니다`, not `왜 이렇게 하셨나요`.

Mark severity when it is not obvious:

- **(필수)** — must fix before merge: a defect, a security gap
- **(제안)** — worth doing, author's call
- **(질문)** — genuinely asking, not a disguised request

A comment with no marker reads as 필수. Use 제안 liberally so the required ones stand out.

## Posting

Read the diff first — the whole diff, not just the changed lines:

```bash
gh pr view <번호> --json title,body,files
gh pr diff <번호>
```

Then read the surrounding files. A boundary defect is invisible in a diff, because the other side of the boundary is not in it — that is exactly why these survive review.

**Line comment:**

```bash
gh api repos/VOID-GSM/TaekBaeWatShong_SERVER/pulls/<번호>/comments \
  -f body='<한국어 코멘트>' \
  -f commit_id=<HEAD SHA> \
  -f path='src/main/kotlin/.../ParcelService.kt' \
  -F line=42 \
  -f side=RIGHT
```

Get the head SHA with `gh pr view <번호> --json headRefOid -q .headRefOid`.

**Overall review** — submit one review carrying the summary rather than a stray top-level comment:

```bash
gh pr review <번호> --comment --body-file /path/to/scratchpad/review.md
```

Use `--comment`. Do not use `--approve` or `--request-changes` unless the user explicitly asks — those are formal states that affect merge rules and speak for the user.

## Review summary format

```markdown
## 리뷰 요약

<전반적인 평가 1~2줄>

### 필수 수정 (n건)
- `파일:줄` — <요약>

### 제안 (n건)
- `파일:줄` — <요약>

### 확인하지 못한 부분
- <실행/검증하지 못한 항목과 이유>
```

The last section matters here. `./gradlew build` runs only `contextLoads()` and needs a live MySQL plus six environment variables, so most runtime behavior cannot be verified by review. Say what you could not check rather than implying the whole PR was validated.

## Rules

- Never approve or request changes unless asked — use `--comment`.
- Never leave a comment you cannot justify with a concrete failure or a codebase convention.
- Never repeat a point already made in another comment on the same PR.
- Read both sides of any boundary before commenting on it; a comment based on one file is a guess.
- If the PR is large, comment on the highest-value items and say the rest was reviewed at a lower depth.

## Language rule

**Skill and agent definition files are written in English. Everything produced for a human — commit messages, PR bodies, review comments, thread replies, and every response to the user — is written in Korean.**
