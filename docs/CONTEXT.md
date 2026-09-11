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
- **Config is injected via a generated Kotlin file, not a plugin.** A Gradle
  task in `shared/build.gradle.kts` reads `SUPABASE_URL` and
  `SUPABASE_PUBLISHABLE_KEY` from the environment, falling back to
  `local.properties` (gitignored), and generates `app.muster.SupabaseConfig`
  into `commonMain`. `BuildConfig` is Android-only and `expect`/`actual` would
  mean four copies of two strings; BuildKonfig would work but is a third-party
  plugin whose Wasm support is one more thing to verify, which is exactly what
  the rule below exists to avoid. Any future build-time config value follows
  the same path rather than adding a plugin.

  The publishable key is not a secret — it ships in every APK and in the web
  bundle. Keeping it out of git is rotation convenience, not security; RLS is
  the trust boundary. The secret key never reaches the client.
- **Email codes, no passwords.** Signup and sign-in are one flow: a six-digit
  code sent to the address. `signInWith(OTP)` creates the account if the
  address is new; `verifyEmailOtp` with `OtpType.Email.EMAIL` covers both the
  new and existing cases. No password means no reset flow to build, and every
  auth email carries `{{ .Token }}` rather than `{{ .ConfirmationURL }}`, so
  there are no deep links or redirect URLs on any of the four targets.

  It also hardens the invite model. SCHEMA lists "email confirmation must stay
  enabled" as a known limit, because invitations match on address alone. With
  code-only sign-in a session is unreachable without receiving mail at that
  address, so confirmation stops being a toggle that could be turned off.

  Cost: email delivery is the only way in, with no fallback if Resend is down.
  Sessions never expire, so this only bites on a new device or a reinstall.
- **Sessions never expire.** Time-boxing and inactivity timeout stay off in
  Auth → Sessions. Only signing out or losing local storage returns someone to
  the login screen. supabase-kt keeps the session in `SharedPreferences` on
  Android and `NSUserDefaults` on iOS — not Keychain, not encrypted. Accepted
  for a private app; a Keychain-backed `SessionManager` is a maybe, later.

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
- **No sending domain yet.** Resend is configured with its shared test
  sender (`onboarding@resend.dev`), which only delivers to the address on
  the Resend account. Sign-in codes to that address work; anything sent to
  anyone else is dropped. Invitations fail the most quietly, since the
  invite email is fire-and-forget by design and the app never learns it
  never arrived. Buying a domain and verifying it in Resend changes one
  field in Supabase's SMTP settings and nothing in the app. Must be done
  before inviting a real teammate.

## Roles

Two roles only: `admin` and `member`.

- Signup is open to anyone, but **creating a group is allowlisted** —
  `profiles.can_create_groups`, off by default, flipped by hand in the
  Supabase dashboard. An account without it can accept invitations and
  play, nothing more.
- Group creator becomes the first admin.
- Admins can promote members to admin.
- Admins can create events, invite members, remove members, and archive
  the group.
- Admins can change any player's RSVP status and reorder the standby
  queue.
- A member can see every player's status on an event but can change only
  their own.
- A group can never be left without an admin.

## Membership model

- Members are always added **by email**, whether or not they're registered.
- The invited person must **accept** before the group becomes visible to them.
- Email is the key, not the user account — an unregistered invitee signs
  up first, then sees the pending invite matching their address.

### The invite email carries no functionality

It is a notification, nothing more: "you've been invited to <group>, sign
in to the app with this email to accept." No link, no token, no accept
button. Sending it is fire-and-forget — if it never arrives, the
invitation still exists and the invitee still finds it on signing in.

Accepting and declining both happen in the app, as RPC calls to
`accept_group_invitation` / `decline_group_invitation`.

### Identity

`auth.users.id` is a Supabase-generated UUID and is what `auth.uid()`
returns in RLS policies, so `profiles.id` mirrors it. Email cannot be the
primary key.

**We do not build email change.** There is no screen for it; the column
grant on `profiles` allows only `name`. If someone needs a different
address, an admin re-invites them at it.

**Setting a name is required, in the app.** `profiles.name` is nullable and
null means never set. The account exists from the moment the first sign-in
code is requested, before there is anywhere to have asked, so
`handle_new_user` leaves the column null rather than inventing a name from
the email's local part. After sign-in the app shows a name screen if it is
null — no name, no home screen. This is a UX gate rather than a security
rule, so unlike everything else it lives in the client; nothing in the
schema depends on a name being set.

If an address is changed anyway — from the Supabase dashboard, or via the
Auth API, both of which sit outside these tables — a trigger copies it
into `profiles`. **Identity and memberships survive**; only the invite
lookup key moves. That is a deliberate change from the earlier "email
change means a new user" rule, which was unenforceable: nothing in the
schema could stop Auth from changing the address, and leaving `profiles`
stale would have pointed invitation matching at an address the user no
longer owns.

## App architecture

Layered, in `shared/src/commonMain/kotlin/app/muster/`:

```
data/
  di/           dataModule — client, repositories
  supabase/     createClient(), session storage
  dto/          @Serializable table shapes
  mapper/       dto -> model, and error mapping
  repository/   *RepositoryImpl
  fake/         Fake*Repository — in-memory, for previews and tests
domain/
  di/           domainModule — use cases
  model/        Profile, Group, Member, Event, Rsvp, StandbyEntry
  error/        DomainError — LastAdmin, EventFull, EventStarted, ...
  repository/   interfaces
  usecase/      one class per operation
ui/
  di/           uiModule — ViewModels
  navigation/
  theme/ component/ auth/ home/ group/ event/ settings/
```

`data`, `domain` and `ui` are treated as separate modules, each owning its
own DI. There is no shared DI package and nothing at the root besides the
entry points. They are packages inside `:shared`, not Gradle modules, so the
compiler does not enforce the boundaries — keep imports pointing inward:
`ui` -> `domain` <- `data`.

The layering is kept even where it looks like overhead — interfaces in
`domain`, implementations in `data`, a use case per operation, fakes
behind the same interfaces.

`domain` is unusually thin, because capacity, standby promotion, the
last-admin invariant and the freeze all live in Postgres. Use cases
mostly delegate. Do not re-implement those rules in Kotlin: the client
cannot enforce them, and a second copy would drift.

`DomainError` is where the layering earns its keep. The database rejects
things the client cannot predict — last admin, full event, started event
— so `data/mapper` turns Postgres error codes into typed errors in one
place. Without it the UI shows raw `PostgrestRestException` strings.

There is no network module. supabase-kt *is* the client: Postgrest builds
the REST calls, Auth handles sessions and refresh, Ktor is the engine
underneath. Repositories call `supabase.from("groups")` directly — no API
interface, no manual JSON, and no `where user_id = ...`, since RLS
decides what comes back. Retrofit would not work here anyway; it is
JVM-only and cannot live in `commonMain`.

### DI: Koin

One module per layer, each in that layer's `di/` package: `dataModule`
(client, repositories), `domainModule` (use cases), `uiModule`
(ViewModels). A layer's module appears once it has something to declare.
Plus a platform module per target for anything platform-specific.
`initKoin()` loads them all. It lives at the root (`app/muster/Koin.kt`),
beside `App.kt`, as an entry point rather than a DI package — the one
place that sees every layer. Each host calls it before any UI:
`MusterApplication` on Android, `MainViewController` on iOS, `main.kt` on
web.

Android calls it from an `Application` subclass, not `MainActivity`:
`onCreate` of an activity re-runs on every rotation and configuration
change, and Koin throws if started twice. `initKoin()` also guards against
an already-started Koin, because iOS may call `MainViewController()` more
than once.

The `SupabaseClient` is a Koin `single` built by a `createClient()`
factory, not a top-level `val`, so the fakes can replace it. Repositories
are `single`; use cases are `factory`, being stateless and cheap.

## Working conventions

- Understand the architectural implications before writing code.
- Print files for review before pushing. Never push without explicit
  permission.
- Report differences rather than silently replacing files.
- Enforce rules in the database (RLS, constraints, triggers), not in the
  client. The client is not a trust boundary.

## Standby

`capacity` is an event's ceiling on **invitations**, not on confirmed
players. An admin invites up to `capacity` players; anyone beyond that
goes into an ordered standby queue.

- A standby player has **no RSVP row**. They are a name and a queue
  position, nothing more, and have no status.
- Invariant: `pending` + `in` invitations <= `capacity`.
- Promotion is **automatic**, with no admin approval step: while a slot
  is free and the queue is non-empty, the first standby player is
  invited — a new invitation with status `pending`. They can still
  decline, which frees the slot again and pulls in the next.
- It fires on every path that opens a slot or changes the queue: a player
  declining, an admin setting a player to `out`, a member being removed
  from the group (the FK cascade), and a player being added to or moved
  up the queue.
- The queue is **reorderable** by admins. Moving someone to the front
  while a slot is open promotes them immediately. Reordering is the
  admin's only promotion lever — there is no direct "promote this player"
  action.
- An admin setting a player from `out` back to `in` on a full event is
  **rejected**. The admin must free a slot first.
- Promotion stops at `starts_at`, like everything else.

This lives in a **database trigger**, not the client. One of its entry
points is an `ON DELETE CASCADE` (member removal), and no client code
runs on a cascade.

## Event freeze

`starts_at` is the only cutoff. Once an event starts it takes no further
edits — no RSVP changes, no standby promotions, no new invites. Deleting
is still allowed: an admin can remove an old event, and blocking that
would also block the cascade that clears its rows. There is no separate
RSVP deadline.

## Groups are archived, never deleted

An admin "removing" a group sets `archived_at`. Everything survives —
members, events, invitations, RSVPs — and the group becomes invisible
and frozen for everyone, admins included.

Restoring is **dashboard-only**. There is no in-app archive page, and an
archived group cannot be unarchived from the app. That's the point: an
admin who archives by mistake shouldn't also be able to make it
permanent. An archive page is a maybe, later.

## No attendance history

RSVPs are operational data, not records. Removing a member from a group
deletes their `event_invitations` rows across all its events, past
included. There are no season stats, so nothing depends on those rows
surviving.

## Open items

None.
