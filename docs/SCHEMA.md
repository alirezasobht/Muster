# Muster — Database Schema

Postgres via Supabase. SQL lives in `supabase/migrations/`; this file is
the design it implements.

## Tables

### profiles
Mirrors `auth.users`. Created by trigger on signup.

| Column | Type | Notes |
|---|---|---|
| id | uuid PK | → `auth.users.id`, cascade |
| name | text | **nullable**, non-blank when present, editable by owner |
| email | text | not null, **unique**, stored lowercase and trimmed (CHECK) |
| can_create_groups | bool | default false, set by hand in the dashboard |

A null `name` means the person has not set one yet. The account exists
from the moment the first sign-in code is requested, which is before
there is anywhere to have asked. The app requires a name before the home
screen; nothing in the schema depends on one being set. See CONTEXT.md →
Identity.

Email is the invite lookup key, so case and whitespace must not create
two identities, and the unique constraint means the schema doesn't lean
on `auth.users` to enforce it.

Signup is open to anyone; **creating a group is allowlisted**. Without
the flag an account can accept invitations and play, nothing more.
Column privileges leave `name` as the only self-editable field, so
`can_create_groups` cannot be granted by its owner. `email` is not
editable here either, but that is not the whole story: Supabase Auth has
its own email-change flow outside these tables, and a trigger syncs any
such change back into `profiles`. See CONTEXT.md → Identity.

### groups

| Column | Type | Notes |
|---|---|---|
| id | uuid PK | |
| name | text | not null, non-blank |
| created_by | uuid | → profiles, not null |
| created_at | timestamptz | |
| archived_at | timestamptz | null = live |

**Groups are never deleted.** Archiving sets `archived_at`; the group,
its members, events, invitations and RSVPs all survive. An archived
group is invisible and frozen for everyone including its admins, and can
only be restored from the Supabase dashboard — there is no in-app
archive page. That is deliberate: an admin archiving by mistake should
not also be able to make it permanent.

One condition implements all of it. `is_group_member()` and
`is_group_admin()` both require `archived_at is null`, and every policy
on `groups`, `events`, `event_invitations` and `event_standby` routes through
one of them.

### group_members
Many-to-many, and the visibility rule.

| Column | Type | Notes |
|---|---|---|
| group_id | uuid | → groups, cascade |
| profile_id | uuid | → profiles, cascade |
| role | text | `admin` \| `member` |
| joined_at | timestamptz | |

PK `(group_id, profile_id)`

Row exists only **after** the invitation is accepted.

### group_invitations
Keyed by email, not by profile.

| Column | Type | Notes |
|---|---|---|
| id | uuid PK | |
| group_id | uuid | → groups, cascade |
| email | text | lowercase |
| invited_by | uuid | → profiles, not null |
| status | text | `pending` \| `accepted` \| `declined` |
| created_at | timestamptz | |

No token column. The email is a notification only — nothing is redeemed,
so there is nothing to authenticate. The invitee is matched by address.

Partial unique index on `(group_id, email) WHERE status = 'pending'` —
one open invite per person per group. A second one raises 23505; someone
who has already accepted is caught separately, by rule 16.

### events

| Column | Type | Notes |
|---|---|---|
| id | uuid PK | |
| group_id | uuid | → groups, cascade |
| title | text | not null, non-blank |
| starts_at | timestamptz | |
| location | text | nullable |
| capacity | int | > 0. Ceiling on invitations, not on confirmations. **Set at creation, never editable** |
| created_by | uuid | → profiles, not null |
| created_at | timestamptz | |

UNIQUE `(id, group_id)` — not for uniqueness, which `id` already gives.
It exists so child tables can reference the pair and have Postgres
guarantee their denormalised `group_id` matches the event's.

### event_invitations
Event-level RSVP. A row means the player has been invited.

| Column | Type | Notes |
|---|---|---|
| id | uuid PK | |
| event_id | uuid | |
| group_id | uuid | denormalised from events |
| profile_id | uuid | |
| status | text | `pending` \| `in` \| `out` |

- UNIQUE `(event_id, profile_id)`
- FK `(event_id, group_id)` → `events (id, group_id)` `ON DELETE CASCADE`
- FK `(group_id, profile_id)` → `group_members (group_id, profile_id)`
  `ON DELETE CASCADE`

The second composite FK is deliberate: removing someone from a group
wipes all their RSVPs in that group automatically.

### event_standby
The ordered queue. A standby player has **no** `event_invitations` row.

| Column | Type | Notes |
|---|---|---|
| event_id | uuid | |
| group_id | uuid | denormalised from events |
| profile_id | uuid | |
| position | int | > 0, queue order, never client-written |
| added_at | timestamptz | |

- PK `(event_id, profile_id)`
- UNIQUE `(event_id, position)` — **deferrable**, so a reorder can
  renumber rows inside one transaction without tripping mid-update
- Same two composite FKs as `event_invitations`, so group removal drops
  the player from queues too

No write policies. Every change goes through `set_standby_order()`,
which takes the whole queue as an ordered array and rewrites positions
1..n. Adding, removing and reordering are the same operation, so the
client never computes a position and the queue cannot half-apply.
Gaps are impossible after a rewrite, but promotion leaves them; nothing
depends on positions being contiguous.

### Deferred

`device_tokens` — for FCM. Not in the MVP.

## Rules

### Enforced in the database

1. **Group visibility** — a user can select a group only if a
   `group_members` row exists for them and that group. The same predicate
   cascades to `events`, `event_invitations`, and `event_standby`. A
   creator also sees their own group directly (`created_by = auth.uid()
   and archived_at is null`), needed so `insert ... returning` can hand
   the new row back before `on_group_created` (an `after insert` trigger)
   has written their membership row — see migration 7. The check is a
   plain column reference rather than `group_is_live(id)`: that helper is
   `stable` and looks the row up by id, which runs against the
   statement's own snapshot and never sees the row that same statement is
   still inserting.
2. **At least one admin** — trigger on `group_members` delete and update
   rejecting any operation that would leave a group with zero admins.
   Covers: last admin leaving, last admin demoting themselves, an admin
   demoting the only other admin.
3. **Admin-only actions** — RLS policies checking `role = 'admin'` for:
   create event, invite member, promote/demote, remove member, archive
   group, reorder standby, change another player's RSVP.
4. **Group creation allowlist** — insert on `groups` additionally
   requires `profiles.can_create_groups`. Off by default.
5. **Column privileges** — RLS filters rows, not columns, so a member
   passing a row policy could otherwise rewrite any field on it (moving
   an RSVP to another event, say, skipping capacity and the queue; or
   granting themselves `can_create_groups`, which sits on their own
   profile row; or, as an admin, rewriting a `group_members.profile_id`
   to add someone who never accepted an invitation). `authenticated` may
   update only `event_invitations (status)`, `profiles (name)`,
   `groups (name, archived_at)`, `group_members (role)` and
   `events (title, starts_at, location)` — the last of which is what
   makes `capacity` immutable and pins an event to its group.

   Worth generalising: **any column on a row a user can update is a
   column that user can set.** Permission flags and roles either need an
   explicit column grant, or must live on a table the user cannot write.
6. **Own RSVP** — a member may update `event_invitations` where
   `profile_id = auth.uid()`. Everyone in the group may read all of them.
7. **Seeing your own invitations** — select on `group_invitations` where
   the row's email matches the caller's and the group is live. The only
   policy in the schema not keyed off group membership, and necessarily
   so: before accepting you have no membership row anywhere, so a
   membership-based policy would hide the invitation and make accepting
   impossible. `groups` has a matching branch so the invitee can read
   the group row before deciding. RLS filters rows, not columns, so
   that means the whole row — `id`, `name`, `created_by`, `created_at`,
   `archived_at`. Accepted: the only non-obvious field is a creator
   UUID whose `profiles` row they still cannot read. Nothing about the
   group's members, events or RSVPs is reachable.
8. **Accepting an invitation** — `accept_group_invitation(invitation_id)`,
   a `security definer` function. Two writes that must be atomic: insert
   `group_members`, then mark the invitation accepted. `security definer`
   because the invitee has no membership yet and so no RLS route to
   insert one. The function's guard — a pending invitation exists whose
   email is the caller's — is therefore the entire security model and
   must be exact. `decline_group_invitation(invitation_id)` mirrors it.
9. **Group creation** — creator's `group_members` row with `role = 'admin'`
   is written in the same transaction as the group.
10. **Profile creation** — trigger on `auth.users` insert writes the
    matching `profiles` row. A second trigger, on sign-in, restores the
    row if it has gone missing — same Auth UUID, so anything still
    referencing it reconnects. It never overwrites an existing profile
    and does not bring back deleted memberships or RSVPs.
11. **Capacity invariant** — `pending` + `in` event_invitations <=
    `capacity`, per event. Spans rows, so a trigger, not a CHECK.
    `capacity` is not updatable, so inviting a player (or setting one to
    `in`) is the only way to reach the invariant — one entry point, one
    guard.
12. **Standby promotion** — while a slot is free and the queue is
    non-empty, invite the first standby player (new row, status
    `pending`) and remove them from the queue. Runs on: RSVP change to
    `out`, `event_invitations` row deleted by cascade, and any insert or
    reorder on `event_standby`. Must share a transaction with rule 11 so
    two concurrent declines cannot promote the same player twice.
13. **Mutual exclusion** — a player cannot hold an `event_invitations`
    row and an `event_standby` row for the same event. Cross-table, so a
    trigger.
14. **Event freeze** — once `starts_at` passes the event takes no further
    inserts or updates: no RSVP changes, no promotions, no new invites,
    no queue edits. Deletes are exempt, deliberately — guarding them
    would make an old group unarchivable-and-uncleanable, since any
    cascade would trip on every historic row.
15. **Cascades** — removing a member clears their RSVPs and queue places
    in that group via `ON DELETE CASCADE`. Groups themselves are
    archived rather than deleted, so a group's records are never
    cascaded away in normal use.
16. **Not already a member** — trigger on `group_invitations` insert,
    rejecting an address that already holds a `group_members` row in that
    group. The partial unique index covers a second *open* invitation;
    this covers one that was already accepted, which would otherwise
    insert fine and fail much later inside `accept_group_invitation` on
    the `group_members` primary key. `security definer`, because the
    admin cannot read the invitee's `profiles` row — a shared group is
    exactly what is being established.

### Functions and triggers

Everything callable or automatic, in one place.

**Helpers** — the policies call them. All `security definer stable`.
`security definer` is not optional: a policy on `group_members` that
queried `group_members` would recurse infinitely.

They live in the **`private`** schema, which PostgREST does not expose,
so they can't be called as RPCs. `authenticated` keeps `USAGE` on the
schema and `EXECUTE` on each, because RLS evaluates them as the caller.
Moving them preserved every policy that references them; anything that
calls one by name must qualify it, `private.is_group_admin(...)`.

| Function | Returns |
|---|---|
| `is_group_member(gid)` | caller is in the group, and it is not archived |
| `is_group_admin(gid)` | as above, and role is `admin` |
| `my_email()` | caller's email from `profiles`, not the JWT |
| `can_create_groups()` | caller's allowlist flag |
| `group_is_live(gid)` | not archived — for paths with no membership to check |
| `has_pending_invitation(gid)` | caller has a pending invitation to it |

**RPCs** — called from the app, granted to `authenticated` only.

| Function | Why it exists |
|---|---|
| `accept_group_invitation(id)` | two writes that must be atomic; invitee has no membership yet, so no RLS route to insert one |
| `decline_group_invitation(id)` | mirrors accept; no update policy on `group_invitations` |
| `set_standby_order(eid, uuid[])` | whole queue in one call; the only write path to `event_standby` |
| `get_my_pending_invitations()` | Home shows who invited you (DESIGN.md 1d); `profiles_select` can't reach the inviter's row since the invitee has no membership yet. Read-only, so `security definer` instead of a new policy — see DECISIONS.md |
| `invite_group_member_by_email(group_id, email)` | the only way to create an invitation. `security definer`: checks the caller is an admin of a live group, normalises the address, sets `invited_by`, inserts, then queues the email |
| `disinvite_player(event_id, profile_id)` | removes a player from an event. `security invoker`, so the existing delete policy decides; returns whether a row went |

Things that need **no** RPC: changing your own or (as admin) another
player's RSVP is a plain update on `event_invitations`. Demoting an
invited player to the queue is a disinvite then `set_standby_order` —
the delete commits first, so promotion pulls the next queued player into
the freed slot and the demoted player joins behind them.

`set_standby_order` runs `security definer`, so RLS checks nothing and
its own guards are the entire security model: caller is a group admin,
event not started, every listed player is a member, and none of them
already holds an invitation.

**Triggers**

| Trigger | Function | Fires on | Does |
|---|---|---|---|
| `on_auth_user_created` | `handle_new_user` | insert on `auth.users` | creates the `profiles` row |
| `on_auth_user_sign_in_restore_profile` | `restore_profile_on_sign_in` | update of `last_sign_in_at` on `auth.users` | recreates a missing `profiles` row, never touches an existing one |
| `on_auth_user_email_changed` | `sync_user_email` | update of email on `auth.users` | keeps `profiles.email` in step with Auth |
| `on_group_created` | `handle_new_group` | insert on `groups` | creator's admin membership |
| `group_invitations_not_member` | `reject_if_already_member` | insert on `group_invitations` | rejects inviting someone already in the group |
| `group_keeps_an_admin` | `ensure_admin_remains` | update/delete on `group_members` | rejects leaving a group admin-less. **Deferred** — lets a transaction promote and demote in either order, and lets a group's cascade through |
| `event_invitations_frozen` | `reject_if_event_started` | insert/update on `event_invitations` | rejects writes past `starts_at` |
| `event_standby_frozen` | `reject_if_event_started` | insert/update on `event_standby` | as above |
| `events_frozen` | `reject_if_event_started_self` | update on `events` | as above, checked against the old row |
| `event_invitations_not_queued` | `reject_if_queued` | insert on `event_invitations` | mutual exclusion — invited or queued, never both |
| `event_standby_not_invited` | `reject_if_invited` | insert on `event_standby` | the other half of the same rule |
| `event_invitations_capacity` | `enforce_capacity` | insert/update on `event_invitations` | `pending + in <= capacity` |
| `event_invitations_promote` | `trigger_promote_standby` | update/delete on `event_invitations` | promotes from the queue. **Deferred** |
| `event_standby_promote` | `trigger_promote_standby` | insert/update on `event_standby` | as above. **Deferred** |

**Internal** — not granted to `authenticated`, not callable from the app.

| Function | Role |
|---|---|
| `promote_standby(eid)` | the promotion body itself |
| `trigger_promote_standby()` | thin wrapper, passes the event id from the changed row |
| `send_group_invitation_email(invitation_id)` | queues the invite email through `pg_net`. Called by the invite RPC. Kept for a future resend button, but not granted until resend has a throttle — otherwise any admin could email an address without limit |

Every other function above is a trigger function and is likewise
unreachable from the API.

`promote_standby(eid)` is the shared body: while a slot is free and the
queue is non-empty, delete the front of the queue and insert a `pending`
invitation. Deletion comes first or mutual exclusion rejects the insert.

The concurrency-sensitive parts rest on six details:

- **`select ... for update` on the event row**, taken by `enforce_capacity`
  on every insert before any early return, and by `promote_standby`.
  It carries mutual exclusion too, which has no lock of its own. Without
  it two simultaneous declines both see a free slot and promote the same
  player; two simultaneous invites both fit into the last space; and an
  `out` invite races a standby insert for the same player, each blind to
  the other's uncommitted row.
- **`select ... for update` on the group row** in `ensure_admin_remains`.
  Deferring a check to commit makes multi-step changes possible; it does
  not serialise transactions. Two concurrent demotions would each still
  see the other's uncommitted admin row.
- **`select ... for share` on the group row** in the two invitation
  RPCs, in `set_standby_order`, and in `promote_standby`. Checking
  `archived_at is null` reads a snapshot; without the lock an archive
  committing in between would still let the operation write into an
  archived group.
- **Lock order is group before event**, everywhere both are taken.
  `ensure_admin_remains` locks the group and its cascade reaches
  `promote_standby`, which locks the event — so the queue paths must
  follow the same order or deadlock. The invitation RPCs take
  invitation then group, and never touch an event.
- **`clock_timestamp()` rather than `now()`** for every `starts_at`
  cutoff. `now()` is fixed at transaction start, so a request that
  began before kickoff and waited on a lock would pass the cutoff after
  it, and deferred promotion runs at commit — potentially well after the
  transaction's `now()`.
- **The promotion triggers are deferred**, so they run at commit rather
  than mid-statement. A reorder passes through states where positions
  duplicate, and promoting from one of those picks the wrong player.
  They still fire once per affected row; `promote_standby` is idempotent,
  so the repeats are harmless rather than collapsed.

### Grants

Postgres gives `EXECUTE` to `PUBLIC` on **every new function** by
default, and `anon` inherits it. So every function revokes it itself:

```sql
revoke execute on function public.f(...) from public, anon;
grant execute on function public.f(...) to authenticated;  -- RPCs only
```

Revoking from `anon` alone is not enough — the `PUBLIC` grant still
reaches it. Internal and trigger functions revoke from `authenticated`
too. A new function that forgets this is callable by anyone with the
publishable key.

`create or replace` keeps existing grants; a fresh `create` does not.

**Still open: table grants.** `authenticated` still has broad direct
write access to the tables, narrowed only by RLS and the column grants
in rule 5. The plan is to move writes behind RPCs one at a time, then
revoke. The trap: a `security invoker` RPC like `disinvite_player` runs
with the caller's own table permissions, so revoking the grant breaks
it. Each RPC has to become `security definer` with its own checks, or
keep a narrow grant, before the revoke.

### Known limits

- **Lost update on the standby queue.** `set_standby_order` replaces the
  whole queue. Two admins on stale screens — one adds C, the other
  submits the list without C — and C is silently dropped. The stale check
  catches a player who was promoted meanwhile, not a concurrent edit. A
  revision token would close it; not worth it for one team with one or
  two admins.
- **Sign-in must prove the email is yours.** An invitation is matched by
  email address and nothing else, so whoever signs in as an address gets
  that address's invitations. Email codes guarantee it — you cannot sign
  in without reading the mailbox. If passwords are ever added, email
  confirmation must be on, or anyone could register as a teammate's
  address and collect their invitations.

### Application logic

- **Invite email** — `invite_group_member_by_email` calls
  `send_group_invitation_email` in the same transaction. That reads the
  project URL and a shared webhook secret from **Vault** and queues a
  request with **`pg_net`**, which sends it only after the transaction
  commits — a rolled-back invite sends nothing. The Edge Function
  `send-group-invitation-email` checks the secret, re-reads the
  invitation, and sends through Resend.

  Delivery itself is fire-and-forget: a failed send never reaches the
  app, and shows up only in `net._http_response`. But **the invite does
  depend on configuration** — if either Vault entry is missing the
  function raises, and the invitation rolls back with it. Both must exist
  before an environment can invite anyone.

Everything else is in the database. Standby promotion in particular
cannot be client-side: one of its entry points is an `ON DELETE CASCADE`,
and no client code runs on a cascade.

## Migrations

`supabase/migrations/`, applied with the Supabase CLI. Each file's header
comment says what it does and why. Migrations 1–3 are the schema,
policies and triggers; everything after is a correction or an addition,
and the rules above describe the current state rather than the history.

## Open questions

None.
