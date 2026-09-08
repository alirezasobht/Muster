# Muster — Project Context

## What this is

A personal football-team organizing app, built to replace Teamer
(shutting down). Private use — for my own teams, not a product for others.

Name: **Muster** — to gather people together. Works as verb and noun,
and doesn't tie the app to one sport.

## Scope

In scope:
- Multiple groups, each with its own members
- Create events within a group
- Invite players to events
- Standby players with an ordered queue
- Manage invitations (group-level and event-level)

Explicitly out of scope:
- Payments
- Chat / messaging
- Push notifications (deferred past MVP — see below)
- Public web page

## Stack

| Layer | Choice | Notes |
|---|---|---|
| UI | Compose Multiplatform | Android + iOS from one Kotlin codebase |
| Shared code | Kotlin Multiplatform | `commonMain` for models, logic, data access |
| Targets | Android, iOS, Web | Desktop and Server off. Web is exploratory |
| Backend | Supabase | Postgres, Auth, RLS, Edge Functions |
| Region | Sydney (`ap-southeast-2`) | Users are AU-based |
| Email | Resend via custom SMTP | Supabase's built-in sender is test-only |
| Push (post-MVP) | FCM | Delivers to both Android and iOS (via APNs) |

Cost: Supabase free tier, Resend free tier. Apple Developer account
($99/yr) needed only when shipping to iPhones.

### Decisions already closed

- **Native over PWA.** PWA was seriously considered to avoid the $99/yr
  and cover all platforms at once. Rejected because iOS web push is
  unreliable — silent unsubscriptions, listeners failing after restart —
  and notifications are the core feature.
- **Supabase over Firebase.** The data is relational; standby queues need
  ordered SQL queries. Also has an official KMP SDK, where Firebase relies
  on a community wrapper.
- **Compose Multiplatform over Flutter.** Existing Kotlin/Compose skills
  transfer directly; Flutter's larger ecosystem doesn't pay off at this size.
- **Supabase over a self-hosted Ktor backend.** Ktor would give shared
  models across client and server, but means writing auth, running Postgres,
  migrations, deployment, and hosting (~$5/mo). More importantly, Supabase
  enforces visibility in RLS at the database; with Ktor every check is code
  that must not be forgotten in any endpoint. Revisit only if the standby
  logic outgrows an Edge Function.

## Project setup

Created via the Kotlin Multiplatform wizard (JetBrains KMP plugin in
Android Studio, or kmp.jetbrains.com) — **not** a standard Android project.

Targets: Android, iOS with **Share UI**, and Web. Desktop and Server unchecked.

```
shared/              shared module — UI + logic
  src/commonMain/    shared Compose UI, models, logic
  src/androidMain/
  src/iosMain/
  src/jsMain/
  src/wasmJsMain/
  src/commonTest/    + androidHostTest, iosTest, webTest
androidApp/          Android host — MainActivity only
iosApp/              Xcode project, thin SwiftUI wrapper
webApp/              Web host — main.kt, index.html, styles.css
```

Package: `app.muster`.

Xcode must be installed; the plugin's preflight checks will flag it otherwise.

### On the Web target

Included to experiment with, not committed to. It's the Wasm canvas
target — heavy initial load, weak on Safari — so it is not the path to a
lightweight public invite page.

The cost of keeping it: `wasmJs` constrains `commonMain`. Every shared
dependency must support Wasm or it has to move into platform-specific
source sets. Supabase's Kotlin SDK does support wasmJs; smaller libraries
often won't.

Rule: if a library needed for Android/iOS lacks Wasm support, drop the
web target rather than the library.

### Known risks

- Firebase/FCM on KMP needs GitLive's wrapper or hand-written
  expect/actual bindings. Only relevant once push is picked back up.
- Supabase free-tier projects pause after 7 days of inactivity. Fine
  during a season; an off-season break will need a manual unpause.

## Roles

Two roles only: `admin` and `member`.

- Group creator becomes the first admin.
- Admins can promote members to admin.
- Admins can create events, invite members, remove members, and delete
  the group.
- A group can never be left without an admin.

## Membership model

- Members are always added **by email**, whether or not they're registered.
- The invited person must **accept** before the group becomes visible to them.
- Email is the key, not the user account — an unregistered invitee signs
  up first, then sees the pending invite matching their address.

### Identity

`auth.users.id` is a Supabase-generated UUID and is what `auth.uid()`
returns in RLS policies, so `profiles.id` mirrors it. Email cannot be the
primary key.

**Email change means a new user.** We do not build email change at all. If
someone needs a different address they sign up again and an admin
re-invites them. Email is the invite lookup key; UUID is the identity.

## Working conventions

- Understand the architectural implications before writing code.
- Print files for review before pushing. Never push without explicit
  permission.
- Report differences rather than silently replacing files.
- Enforce rules in the database (RLS, constraints, triggers), not in the
  client. The client is not a trust boundary.

## Standby

Events have a `capacity`. Players beyond it go on standby in an ordered
queue.

- Promotion is **automatic** — whenever a spot opens, the first standby
  player in the queue takes it. No admin approval step.
- A spot opens whenever confirmed players drop below capacity. Triggers:
  a confirmed player declining, and a member being removed from the group
  (the FK cascade silently frees their spot).
- Promotion stops at `starts_at`, like everything else.

## Event freeze

`starts_at` is the only cutoff. Once an event starts it is immutable — no
RSVP changes, no standby promotions, no new invites. There is no separate
RSVP deadline.

## No attendance history

RSVPs are operational data, not records. Removing a member from a group
deletes their RSVPs across all its events, past included. There are no
season stats, so nothing depends on those rows surviving.

## Open items

None.
