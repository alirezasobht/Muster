# Muster — Project Context

What the app is and the rules it runs on. Code organisation is in
ARCHITECTURE.md, the database in SCHEMA.md, screens in SCREENS.md, and
the reasoning behind closed choices in DECISIONS.md.

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
- Push notifications (deferred past MVP)
- Public web page

## Stack

| Layer | Choice | Notes |
|---|---|---|
| UI | Compose Multiplatform | Android + iOS from one Kotlin codebase |
| Shared code | Kotlin Multiplatform | `commonMain` for models, logic, data access |
| Targets | Android, iOS, Web | Desktop and Server off |
| Backend | Supabase | Postgres, Auth, RLS, Edge Functions |
| Region | Sydney (`ap-southeast-2`) | Users are AU-based |
| Auth | Email codes | No passwords anywhere |
| Email | Resend | A sending subdomain of the app's domain, SPF and DKIM verified |
| Push (post-MVP) | FCM | Delivers to both Android and iOS (via APNs) |

Every row here was a choice with an argument behind it — see
DECISIONS.md.

Cost: Supabase free tier, Resend free tier. Domain fee (yearly). Apple Developer account(yearly).

## Known risks

- Firebase/FCM on KMP needs GitLive's wrapper or hand-written
  expect/actual bindings. Only relevant once push is picked back up.
- Supabase free-tier projects pause after 7 days of inactivity. Fine
  during a season; an off-season break will need a manual unpause.
- **Email delivery is the only way in.** Sign-in is code-only, so if
  Resend is down or a message is filtered, nobody can sign in. Sessions
  never expire, so it only bites on a new device or a reinstall. Worth
  re-checking spam folders after any change to the sending setup.

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
button. Delivery is fire-and-forget — if it never arrives, the invitation
still exists and the invitee still finds it on signing in.

The one thing it does depend on is **configuration**. Inviting and
queueing the email happen in one transaction, so an environment missing
its email secrets can't invite anyone at all. SCHEMA.md → Application
logic has the detail; setting them is a hard step before any environment
goes live.

Accepting and declining both happen in the app, as RPC calls to
`accept_group_invitation` / `decline_group_invitation`.

### Identity

`auth.users.id` is a Supabase-generated UUID and is what `auth.uid()`
returns in RLS policies, so `profiles.id` mirrors it. Email cannot be the
primary key.

**We do not build email change.** There is no screen for it; the column
grant on `profiles` allows only `name`. If someone needs a different
address, an admin re-invites them at it. A change made outside the app —
dashboard or Auth API — is copied into `profiles` by a trigger, and
memberships survive it. See DECISIONS.md.

**Setting a name is required, in the app.** `profiles.name` is nullable and
null means never set. The account exists from the moment the first sign-in
code is requested, before there is anywhere to have asked, so
`handle_new_user` leaves the column null rather than inventing a name from
the email's local part. After sign-in the app shows a name screen if it is
null — no name, no home screen. This is a UX gate rather than a security
rule, so unlike everything else it lives in the client; nothing in the
schema depends on a name being set.

## Standby

`capacity` is an event's ceiling on **invitations**, not on confirmed
players. An admin invites up to `capacity` players; anyone beyond that
goes into an ordered standby queue.

- A standby player has **no RSVP row**. They are a name and a queue
  position, nothing more, and have no status.
- Promotion is **automatic**, with no admin approval step: whenever a
  slot frees, the player at the front of the queue is invited. They can
  still decline, which frees the slot again and pulls in the next.
- The queue is **reorderable** by admins, and that is their only
  promotion lever — there is no direct "promote this player" action.
  Moving someone to the front while a slot is open promotes them
  immediately.
- An admin setting a player from `out` back to `in` on a full event is
  **rejected**. The admin must free a slot first.
- Promotion stops at `starts_at`, like everything else.

It lives in a **database trigger**, not the client — one of its entry
points is an `ON DELETE CASCADE`, and no client code runs on a cascade.
SCHEMA.md rules 11–13 have the invariant and every path that fires it.

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

## Time zones

`events.starts_at` is `timestamptz` — an absolute instant, correct
everywhere. What an instant cannot tell you is what wall-clock time it
was *meant* to be, and that is what people actually care about: kickoff
is 9pm at the pitch, wherever the admin happened to be when they created
it.

**For now every group plays in Sydney.** `Australia/Sydney` is a
constant, used in exactly two places — converting the picker's
`LocalDateTime` to an instant on the way in, and back again for display.
Never `TimeZone.currentSystemDefault()`: that is the obvious thing to
reach for and it is wrong. An admin creating a fixture while travelling
would silently set the wrong time, and nobody would notice until people
turned up at the wrong hour.

The zone is not stored. The instant is already unambiguous, and while
there is one city a constant does the rest of the job.

**Later: a `time_zone` column on `groups`,** set at creation with a
picker and editable afterwards. IANA names like `Australia/Sydney`, not
offsets — offsets move with daylight saving, names don't. Adding it
backfills cleanly: every existing row is Sydney by definition, so
`default 'Australia/Sydney'` is correct history rather than a guess. On
the group rather than the event, because a team plays where it plays;
per-event would only matter for a tour.

## Environments

**Dev** and **Production** are separate hosted Supabase projects. No
staging. Each has its own data,
users, URLs, keys, Edge Function secrets and Vault entries.

The Muster repo holds everything non-secret — migrations, Edge Function
source, `config.toml`, setup docs and the scripts under
`supabase/scripts/`. Real values live in a separate local repo,
`Muster-env`, one folder per environment. Setup steps are in
`supabase/docs/`.

## Working conventions

- Understand the architectural implications before writing code.
- Report differences rather than silently replacing files.
- Enforce rules in the database (RLS, constraints, triggers), not in the
  client. The client is not a trust boundary.
- Work step by step. One step at a time, wait for a go before the next.

## Open items

None.
