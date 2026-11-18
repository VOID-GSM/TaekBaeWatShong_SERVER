# TaekBaeWatShong (택배왔숑) — Product Specification

> Source: product brief from the VOID team, 2026-09-10. This document is the authoritative product description for the server. When code and this document disagree, that is a finding — see [IMPLEMENTATION-STATUS.md](IMPLEMENTATION-STATUS.md).

## Purpose

A parcel management service for a school, built to **prevent parcels from being left unattended or lost**. Students and teachers order parcels to the school; parcels accumulate on shelves in a storage area; without tracking, they sit unclaimed for days or disappear entirely. The service makes every parcel's location, arrival time, and owner visible, and applies social pressure to unclaimed parcels.

Built by **VOID** (GSM). Organization: `VOID-GSM`.

## Two applications

| App | Users |
|-----|-------|
| **Admin** | One 학생생활안전부 teacher, 학생회 학생생활안전부 students, the VOID team, and **delivery drivers** (택배 기사님) |
| **Client** | Every teacher and student at the school who orders parcels |

Both are separate frontends against this one server. They already have separate OAuth redirect targets: `app.oauth2.redirect-uri` (client, default port 3000) and `app.oauth2.admin-redirect-uri` (admin, default port 3001).

## Authentication

**All login is Google OAuth.** Signup collects different fields per role:

| Role | Signup fields |
|------|--------------|
| Student (Client) | name, **student number (학번)** |
| Teacher (Client) | name |
| Admin | name |

The user picks "teacher or student" during Client signup. Roles map to the existing `Role` enum: `STUDENT`, `TEACHER`, `ADMIN`.

## Storage zones

Two three-tier shelves in the storage area, six zones total:

```
   Shelf 1 (left)        Shelf 2 (right)
  ┌──────────────┐      ┌──────────────┐
  │      A       │ top  │      D       │ top
  ├──────────────┤      ├──────────────┤
  │      B       │ mid  │      E       │ mid
  ├──────────────┤      ├──────────────┤
  │      C       │ btm  │      F       │ btm
  └──────────────┘      └──────────────┘
```

This matches the `Zone` enum's `description` values exactly (`A("1번 선반 - 1단 (상단)")` … `F("2번 선반 - 3단 (하단)")`). A zone map is shown in both apps.

## Client application

### Parcel registration

The user enters:

- Invoice number (운송장 번호) — required
- Delivery carrier (택배사) — required
- Product name / alias (상품명) — **optional**, a name of the user's choosing

When the alias is omitted, the parcel falls back to a default label rather than being nameless.

### Main page — my parcels

Each registered parcel shows:

| Field | Notes |
|-------|-------|
| Invoice number | |
| **Current delivery progress stage** (배송 진행단계) | From the carrier tracking API — finer-grained than arrived/not-arrived |
| Arrival state | Arrived → arrival time + **live count-up timer** since arrival. Not arrived → no timestamp |
| Product name | user-chosen alias, else the default product name |

Also on the main page:

- **Zone map** — how many parcels sit in each of the six zones (A–F)
- **Unclaimed parcels TOP 3** (가져가지 않은 택배 TOP3) — rank, owner name, elapsed time. A parcel enters this list once it has been left **more than 1 day** after arrival; its owner is then treated as a 방치자 (neglector)

### Parcel pickup (택배 회수)

When a parcel has arrived, the pickup screen opens a **barcode scan view**. Scanning the parcel's invoice barcode marks it as picked up (가져간 택배).

### Lost-parcel report (분실 신고)

Tapping one's own parcel opens a report form:

- Expected time of loss (분실 예상 시각)
- Description (내용)

### 1:1 chat

A Client user can start a 1:1 chat with an admin. Admins see incoming chat requests.

### Notifications

Push notifications on parcel state changes:

| Trigger | Notification |
|---------|-------------|
| Parcel registered | confirmation |
| Parcel arrived | arrival notice |
| Zone assigned | where it is stored |
| Parcel picked up | confirmation |
| **Parcel left unclaimed** | reminder to collect it, **including the number of days elapsed** |

## Admin application

### Main page — all parcels

Three sections:

| Section | Fields shown |
|---------|-------------|
| **Arrived** (도착한 택배) | invoice, orderer name, arrival time, **count-up timer** |
| **Not arrived** (도착하지 않은 택배) | invoice, orderer name |
| **Picked up** (가져간 택배) | invoice, orderer name |

**Picked-up parcels remain on the page for 3 days only,** then drop off.

Also shown: parcels grouped by zone, and the **unclaimed TOP 3** (방치자 TOP3).

### Lost-report list

One row per report:

- Reporter name
- Student number — or "선생님" when the reporter is a teacher
- Invoice number
- Product name
- Expected time of loss
- Description

An admin can start a 1:1 chat from a report, and can also see chat requests initiated by students.

### Parcel scan page — used by delivery drivers

A page operated by delivery drivers with a **physical barcode scanner** (an off-the-shelf retail scanner). Scanning a parcel's barcode marks it arrived and fires the arrival notification to its owner.

### Driver QR pages

- **QR generation** — produces a QR code identifying a delivery driver
- **QR recognition** — scanning a driver's QR opens that driver's parcel list:

| Field |
|-------|
| Orderer name |
| Product name |
| Invoice number |
| Progress stage |
| "Complete" button |

This page exists so a driver can **mark many parcels arrived at once** rather than scanning each individually.

## Business rules worth stating precisely

These are the rules the server must enforce; they are the ones most likely to be assumed rather than implemented.

1. **Neglect threshold: more than 1 day after arrival.** A parcel arriving at 14:00 on day 1 becomes neglected after 14:00 on day 2. Whether the boundary is calendar-day or 24-hour based is a decision the current code has already made implicitly — see IMPLEMENTATION-STATUS.
2. **Picked-up retention: 3 days.** Claimed parcels disappear from admin and client lists 3 days after pickup. They are *filtered*, not deleted — the row stays for audit.
3. **The count-up timer is client-side.** The server supplies `arrivedAt`; the frontend renders the running clock. The server must not try to push per-second updates.
4. **TOP 3 is a ranking, not a filter.** It shows the three longest-neglected parcels, so it needs an ordering by neglect duration, not merely a "neglected = true" flag.
5. **An unclaimed reminder repeats with a day count,** so notification state must record what has already been sent to avoid duplicate reminders on the same day.
6. **A parcel belongs to exactly one owner,** and only that owner may pick it up or report it lost. Admins may view all parcels but the ownership rule still holds for pickup.

## Open product questions

Points the brief does not settle. They need answers before the corresponding endpoints can be specified.

1. **Delivery progress stages** — what is the stage vocabulary, and does it come verbatim from the carrier API or map onto a fixed internal enum?
2. **Neglect boundary** — calendar days or elapsed 24-hour periods?
3. **Double pickup** — is scanning an already-picked-up parcel an error (409) or idempotent?
4. **Lost report lifecycle** — does a report have a status (received / investigating / resolved), or is it a flat list?
5. **Driver identity** — is a driver a `User` with a new role, or a separate entity? The brief lists drivers as Admin-app users but they never log in with Google; they are identified by QR.
6. **Chat delivery** — polling, SSE, or WebSocket? The current stack has none of these configured.
7. **Notification transport** — web push, FCM, or in-app only? No dependency for any of these exists yet.
8. **Zone assignment** — who assigns a zone, and when? The current API allows an admin to set it manually and to set it during arrival scanning.
