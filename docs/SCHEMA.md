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
| can_create_groups | bool | default false. **Mirror only**, for 1.0.x clients that read it; the allowlist is the source. Drop once no client reads it |

A null `name` means the person has not set one yet. The account exists
from the moment the first sign-in code is requested, which is before
there is anywhere to have asked. The app requires a name before the home
screen; nothing in the schema depends on one being set. See CONTEXT.md →
Identity.

Email is the invite lookup key, so case and whitespace must not create
two identities, and the unique constraint means the schema doesn't lean
on `auth.users` to enforce it.

Signup is open to anyone; **creating a group is allowlisted**. Without
it an account can accept invitations and play, nothing more.
`set_profile_name` leaves `name` as the only self-editable field, with
no direct client write grants on `profiles`. `email` is not
editable here either, but that is not the whole story: Supabase Auth has
its own email-change flow outside these tables, and a trigger syncs any
such change back into `profiles`. See CONTEXT.md → Identity.

`private.group_creator_emails` (`email` text PK, lowercase and trimmed,
no grants, edited in the dashboard) is the allowlist. It is keyed by
email with no link to the account, so it survives account deletion and
a re-signup at the same address can create groups again. Two triggers
keep the `can_create_groups` column in step for 1.0.x clients, turning
it on only: removing an address revokes the right but leaves the column
set.

### groups

| Column | Type | Notes |
|---|---|---|
| id | uuid PK | |
| name | text | not null, non-blank |
| created_by | uuid | → profiles, nullable, set null when the account is deleted |
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
| invited_by | uuid | → profiles, nullable, set null when the account is deleted |
| status | text | `pending` \| `accepted` \| `declined` |
| created_at | timestamptz | |
| last_sent_at | timestamptz | nullable. Set by `send_group_invitation_email` on every send — the initial one and every resend. Backs the resend throttle |

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
| created_by | uuid | → profiles, nullable, set null when the account is deleted |
| created_at | timestamptz | |

UNIQUE `(id, group_id)` — not for uniqueness, which `id` already gives.
It exists so child tables can reference the pair and have Postgres
guarantee their denormalised `group_id` matches the event's.

Creation goes through `create_event`: a signed-in admin of a live group
supplies the event fields, and the database sets `created_by`. No direct
client write grants remain on `events`; editing and deleting events are
not exposed by the app.

### event_invitations
Event-level RSVP. A row means the player has been invited.

| Column | Type | Notes |
|---|---|---|
| id | uuid PK | |
| event_id | uuid | |
| group_id | uuid | denormalised from events |
| profile_id | uuid | |
| status | text | `pending` \| `in` \| `out` |
| last_sent_at | timestamptz | nullable. Set by `send_event_invitation_email` on every send it makes — add, promotion, or resend. Backs the resend throttle |

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

No write policies. Reordering goes through `set_standby_order()`, which
takes the whole queue as an ordered array and rewrites positions 1..n, so
the client never computes a position and the queue cannot half-apply.
Gaps are impossible after a rewrite, but promotion leaves them; nothing
depends on positions being contiguous. `add_players_to_event()` is the
other writer — it only appends, past whatever `position` is highest, so
it never needs to renumber anything.

### Deferred

`device_tokens` — for FCM. Not in the MVP.

## Rules

### Enforced in the database

1. **Group visibility** — a user can select a group only if a
   `group_members` row exists for them and that group. The same predicate
   cascades to `events`, `event_invitations`, and `event_standby`. A
   creator also sees their own group directly (`created_by = auth.uid()
   and archived_at is null`), originally needed so `insert ... returning` could hand
   the new row back before `on_group_created` (an `after insert` trigger)
   has written their membership row — see migration 7. The check is a
   plain column reference rather than `group_is_live(id)`: that helper is
   `stable` and looks the row up by id, which runs against the
   statement's own snapshot and never sees the row that same statement is
   still inserting. Creation now uses the definer `create_group` RPC;
   the read policy remains in place.
2. **At least one admin** — trigger on `group_members` delete and update
   rejecting any operation that would leave a group with zero admins.
   Covers: last admin leaving, last admin demoting themselves, an admin
   demoting the only other admin.
3. **Admin-only actions** — RPC guards checking `role = 'admin'` for:
   create event, invite member, promote/demote, remove member, archive
   group, reorder standby, change another player's RSVP.
4. **Group creation allowlist** — `create_group` requires the caller's
   email on `private.group_creator_emails`. The RPC sets
   `created_by = auth.uid()` itself; clients cannot supply it.
5. **Column privileges** — RLS filters rows, not columns, so a member
   passing a row policy could otherwise rewrite any field on it (moving
   an RSVP to another event, say, skipping capacity and the queue; or
   granting themselves `can_create_groups`, which sits on their own
   profile row; or, as an admin, rewriting a `group_members.profile_id`
   to add someone who never accepted an invitation). `group_members`
   has no client write grants, including `UPDATE (role)`; role changes
   use `set_group_member_role`. `event_invitations` has no client write
   grants; RSVP changes go through `set_event_rsvp`, which updates only
   status.
   `events` has no client write grants; `create_event` exposes creation
   only, keeping `capacity` and the event's group immutable. `groups`
   has no client write grants, including column grants; creation and
   archiving go through RPCs, and renaming is not exposed.
   `profiles` also has no client write grants; `set_profile_name` updates
   only the caller's name. Auth triggers own profile creation and email sync.

   Worth generalising: **any column on a row a user can update is a
   column that user can set.** Permission flags and roles either need an
   explicit column grant, or must live on a table the user cannot write.
6. **Own RSVP** — `set_event_rsvp` lets a member of a live group change
   their own invitation's status; admins may change another player's.
   Everyone in the group may read all invitations.
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
| `can_create_groups()` | caller's email is on `private.group_creator_emails` |
| `group_is_live(gid)` | not archived — for paths with no membership to check |
| `has_pending_invitation(gid)` | caller has a pending invitation to it |

**RPCs** — called from the app, granted to `authenticated` only.

| Function | Why it exists |
|---|---|
| `get_event_detail(eid)` | read-only `security invoker`: returns event fields, roster with names, ordered standby with names, and the caller's RSVP in one SQL statement. Existing SELECT grants and RLS apply to every table |
| `list_upcoming_events(gid)` | read-only `security invoker`: returns upcoming events with complete in/pending counts and the caller's RSVP in one SQL statement. Uses existing RLS and SELECT grants, database time, and orders by start time then ID |
| `create_event(group_id, title, starts_at, capacity, location)` | `security definer`: requires a signed-in admin, locks the live group before inserting, sets the creator, and returns the created event. Location is optional |
| `set_profile_name(name)` | `security definer`: requires a signed-in caller, updates only their name, and returns their profile as `my_profile`, the flag from the `group_creator_emails` table; raises if the profile is missing. No group membership is required |
| `get_my_profile()` | `security definer`: the caller's profile as `my_profile` (`id, name, email, can_create_groups`), the flag from the `group_creator_emails` table. The app reads its profile here, not from `profiles` |
| `create_group(name)` | `security definer`: requires a signed-in, allowlisted caller, sets the creator, and returns the created group. The existing trigger creates its admin membership |
| `archive_group(group_id)` | `security definer`: locks a live group, requires its admin, and sets the archive timestamp on the server; raises for a missing or already archived group |
| `accept_group_invitation(id)` | accepts a pending group invitation addressed to the caller's profile email. Locks the invitation then its live group; creates membership and marks accepted atomically |
| `decline_group_invitation(id)` | declines a pending group invitation addressed to the caller's profile email. Same locks as accept; creates no membership |
| `set_standby_order(eid, uuid[])` | `security definer` with an explicit session check and empty search path; requires an admin of a live group, locks group then event, and replaces the whole queue (including adding or removing players) |
| `get_my_pending_invitations()` | Home shows who invited you (DESIGN.md 1d); `profiles_select` can't reach the inviter's row since the invitee has no membership yet. Read-only, so `security definer` instead of a new policy — see DECISIONS.md |
| `invite_group_member_by_email(group_id, email)` | the only way to create an invitation. `security definer`: locks the live group before checking admin authority, normalises the address, sets `invited_by`, inserts, then queues the email |
| `set_event_rsvp(event_id, profile_id, status)` | `security definer`: live-group members may change their own RSVP, admins anyone's; locks group then event and returns the invitation's group ID for app notifications. Raises if no invitation matched |
| `disinvite_player(event_id, profile_id)` | `security definer`: requires an admin of a live group, locks group then event, and deletes the invitation. Returns true on success; raises if nothing matched. Deletes remain exempt from the event freeze |
| `resend_event_invitation(event_id, profile_id, tz)` | admins only, on a still-pending event invitation of an event that hasn't started. Same lock order as `disinvite_player` (group, then event), then the invitation row. Throttled to once per calendar day in the caller-supplied `tz`, checked against the invitation's own `last_sent_at` — no per-admin or per-group limit; same throttle helper and app-side `MusterTimeZone` source as `resend_group_invitation`. Calls `private.send_event_invitation_email` with a single-element array |
| `add_players_to_event(event_id, uuid[])` | invites while slots remain, queues the rest, in the given order, one transaction. `security definer` with an explicit session check and empty search path: requires an admin of a live group and an event that hasn't started. **Skips** anyone whose row went stale between the picker loading and the admin confirming — already invited or queued, or since removed from the group. Raising on one player would roll back the batch and add nobody |
| `set_group_member_role(group_id, profile_id, role)` | promote or demote. Locks the live group before checking admin authority and updating the member |
| `remove_group_member(group_id, profile_id)` | locks the live group before checking admin authority and deleting the member. Refuses the caller's own id — that's `leave_group` |
| `revoke_group_invitation(group_id, invitation_id)` | admins only. Locks the invitation then its live group before checking authority and deleting; raises if nothing matched |
| `resend_group_invitation(group_id, invitation_id, tz)` | admins only, on a still-pending invitation. Same lock order as revoke. Throttled to once per calendar day in the caller-supplied `tz`, checked against the invitation's own `last_sent_at` — no per-admin or per-group limit. The app sends `MusterTimeZone`'s id today; a per-group or per-event tz column would just change what it sends, not this function. Calls `send_group_invitation_email`, which stamps `last_sent_at` on every send it makes, including the one from `invite_group_member_by_email` |
| `leave_group(group_id)` | locks the live group and removes only the signed-in caller's membership; raises if the group is missing/archived or the caller is not a member |
| `delete_account(force)` | `security definer`. Without `force`, returns the live groups where the caller is the only admin and deletes nothing if there are any. With `force`, or when there are none, archives those groups, deletes invitations sent to the caller's address, and deletes the caller's `auth.users` row; cascades remove the profile, memberships and RSVPs |

The four member RPCs are `security definer` with their own checks, and
raise if nothing matched rather than succeeding silently. None of them
checks for the last admin: `group_keeps_an_admin` still fires, since a
definer function doesn't bypass triggers, and still raises at commit.

`list_upcoming_events` aggregates invitations before PostgREST applies
its response row limit. A large combined roster therefore cannot truncate
the counts or hide the caller's RSVP. Events with no invitations return
zero counts and a null RSVP; an `out` RSVP is retained. Missing, archived
or inaccessible groups return an empty list through RLS. The API limit
still applies to the number of event summaries returned (currently
1,000 in the repository configuration), not the invitation rows counted.

`get_event_detail` returns one row with nested roster and standby arrays,
so promotion cannot fall between separate invitation and queue reads.
Standby is ordered by position; the roster is unordered, since the Event
screen sorts it. Empty lists return `[]`; null or RLS-hidden names keep the app's
empty-name fallback. Missing or inaccessible events return no row, which
the repository treats as a failed load. Past events remain readable.
The API row limit does not truncate the nested arrays. Counts are derived
from the returned roster, and the caller's RSVP is read in the same snapshot.

`set_group_member_role`, `remove_group_member` and `leave_group` take
`FOR UPDATE` on the live group before touching membership rows. This
serialises authority checks and membership changes with archiving and
the roster RPCs, whose group share locks conflict with it. Removal and
leaving keep that lock through cascades and deferred promotion.

RSVP updates use `set_event_rsvp`; the existing triggers still enforce
capacity, the freeze and deferred promotion. Demoting an invited player
to the queue is a disinvite then `set_standby_order` —
the delete commits first, so promotion pulls the next queued player into
the freed slot and the demoted player joins behind them.

`set_standby_order` runs `security definer`, so RLS checks nothing and
its own guards are the entire security model: caller is a group admin,
event not started, every listed player is a member, and none of them
already holds an invitation.

Both queue-writing RPCs raise for a missing event or non-live group.
An empty queue replacement is valid, including when already empty;
`add_players_to_event` may also do nothing when every supplied player
is stale. These existing whole-queue and batch semantics are preserved.

**Triggers**

| Trigger | Function | Fires on | Does |
|---|---|---|---|
| `on_auth_user_created` | `handle_new_user` | insert on `auth.users` | creates the `profiles` row |
| `on_auth_user_sign_in_restore_profile` | `restore_profile_on_sign_in` | update of `last_sign_in_at` on `auth.users` | recreates a missing `profiles` row, never touches an existing one |
| `on_auth_user_email_changed` | `sync_user_email` | update of email on `auth.users` | keeps `profiles.email` in step with Auth |
| `profiles_group_creator_allowlist` | `private.apply_group_creator_allowlist` | before insert/update of email on `profiles` | sets `can_create_groups` when the email is on `private.group_creator_emails`; covers all three triggers above |
| `group_creator_emails_grant` | `private.grant_listed_group_creator` | insert on `private.group_creator_emails` | sets `can_create_groups` on an existing profile with that email |
| `on_group_created` | `handle_new_group` | insert on `groups` | creator's admin membership |
| `group_invitations_not_member` | `reject_if_already_member` | insert on `group_invitations` | rejects inviting someone already in the group |
| `group_keeps_an_admin` | `ensure_admin_remains` | update/delete on `group_members` | rejects leaving a live group admin-less; archived groups are skipped so account deletion can cascade. **Deferred** — lets a transaction promote and demote in either order, and lets a group's cascade through |
| `event_invitations_frozen` | `reject_if_event_started` | insert/update on `event_invitations` | rejects writes past `starts_at` |
| `event_standby_frozen` | `reject_if_event_started` | insert/update on `event_standby` | as above |
| `events_frozen` | `reject_if_event_started_self` | update on `events` | as above, checked against the old row. An update that only nulls `created_by` is allowed, so deleting an account that created a past event can cascade |
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
| `send_group_invitation_email(invitation_id)` | queues the invite email through `pg_net` and stamps `last_sent_at`. Called by `invite_group_member_by_email` on creation and by `resend_group_invitation`, which owns the throttle check. Not itself granted to `authenticated` — only reachable through those two `security definer` callers, so nothing can call it in a loop and bypass the throttle |
| `private.sent_within_today(last_sent_at, tz)` | pure date math, no table access — `last_sent_at` falls within today's calendar day in the given IANA zone `tz`. `stable`, not `security definer`: it needs no elevated privilege. Called by both `resend_group_invitation` and `resend_event_invitation`, each passing through whatever `tz` its own caller sent, so the boundary math exists once. Reachable only from within a `security definer` caller's context, like `send_group_invitation_email` above |
| `private.send_event_invitation_email(uuid[])` | queues one request for a batch of event invitations and stamps `last_sent_at` on all of them. Called by `add_players_to_event`, `promote_standby`, and `resend_event_invitation` (with a one-element array). No caller check — promotion runs inside whoever freed the slot, often a member — so no grants at all, `authenticated` included; `resend_event_invitation` owns its own admin check before calling in |

Every other function above is a trigger function and is likewise
unreachable from the API.

`promote_standby(eid)` is the shared body: while a slot is free and the
queue is non-empty, delete the front of the queue and insert a `pending`
invitation. Deletion comes first or mutual exclusion rejects the insert.

The concurrency-sensitive parts rest on six details:

- **`select ... for update` on the event row**, taken by `enforce_capacity`
  on every insert before any early return, and by `promote_standby`,
  `set_standby_order`, `add_players_to_event`, `set_event_rsvp` and
  `disinvite_player`. The latter two lock the event before changing an
  invitation, so they cannot hold an invitation while waiting for its event.
  It carries mutual exclusion too, which has no lock of its own. Without
  it two simultaneous declines both see a free slot and promote the same
  player; two simultaneous invites both fit into the last space; and an
  `out` invite races a standby insert for the same player, each blind to
  the other's uncommitted row.
- **`select ... for update` on the group row** in `ensure_admin_remains`,
  `archive_group`, `set_group_member_role`, `remove_group_member` and
  `leave_group`. The member RPCs take it before membership writes, so
  cascade deletes and their promotion triggers already hold the group
  lock before reaching events. Archiving takes this lock before checking
  admin authority, conflicts with the live-group share locks, and
  touches no event rows.
  Deferring a check to commit makes multi-step changes possible; it does
  not serialise transactions. Two concurrent demotions would each still
  see the other's uncommitted admin row.
- **`select ... for share` on the group row** in all four group-invitation
  write RPCs, in `set_standby_order`, in `promote_standby`, and in
  `add_players_to_event`, `create_event`, `set_event_rsvp` and
  `disinvite_player`. Creation holds the group
  lock before inserting its new event. Checking `archived_at is null` reads a
  snapshot; without the lock an archive committing in between would
  still let the operation write into an archived group.
- **Lock order is group before event**, everywhere both are taken.
  Member removal and leaving lock the group before their cascades reach
  `promote_standby`, which locks the event — so the queue paths
  (`set_standby_order`, `add_players_to_event`) and invitation writes
  (`set_event_rsvp`, `disinvite_player`) must follow the same order or
  deadlock. Group-invitation accept, decline and revoke take the existing
  invitation row first, then the group. Creating an invitation locks the
  group before inserting a new row. None of these paths touches an event.
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

**New tables need their own grants.** From 2026-10-30 Supabase stops
granting its API roles on new objects in `public` (environment_setup.md
step 1). Existing tables keep theirs. A migration creating a table must
grant what it needs: `select` to `authenticated` for reads through RLS,
and to `service_role` if an Edge Function reads it. Without it the Data
API refuses the table before RLS runs. Functions already grant
`EXECUTE` explicitly, as above.

The read-only `list_upcoming_events` and `get_event_detail` RPCs are
deliberately `security invoker`: both need SELECT on `events` and
`event_invitations`; detail also reads `event_standby` and `profiles`.
They retain the existing RLS helper permissions, add no table privileges
and do not bypass RLS. EXECUTE is granted only to `authenticated`.

**Groups converted:** `create_group` and `archive_group` own all app
writes. A separate migration revokes `INSERT`, `UPDATE`, `DELETE` and
`TRUNCATE` on `groups` from `PUBLIC`, `anon` and `authenticated`, plus
the separate `UPDATE (name, archived_at)` grants. Read grants and
SELECT policies remain.

**Profiles converted:** `set_profile_name` owns the app's name update.
A separate migration revokes `INSERT`, `UPDATE`, `DELETE` and `TRUNCATE`
on `profiles` from `PUBLIC`, `anon` and `authenticated`, plus the
separate `UPDATE (name)` grants. Read grants and SELECT policies remain;
Auth's definer triggers still create/restore profiles and sync email.

**Events converted:** `create_event` owns the app's event creation.
A separate migration revokes `INSERT`, `UPDATE`, `DELETE` and `TRUNCATE`
on `events` from `PUBLIC`, `anon` and `authenticated`, plus the separate
`UPDATE (title, starts_at, location)` grants. Read grants and SELECT
policies remain. Existing definer RPCs and triggers still lock events
for roster operations; `disinvite_player` needs no write grant on
`events`.

**Event invitations converted:** creation uses `add_players_to_event`
or standby promotion, RSVP changes use `set_event_rsvp`, and disinviting
uses the definer `disinvite_player`. A separate migration revokes
`INSERT`, `UPDATE`, `DELETE` and `TRUNCATE` on `event_invitations` from
`PUBLIC`, `anon` and `authenticated`, plus `UPDATE (status)`. Read grants
and SELECT policies remain. A missing invitation raises on disinvite.

**Group members converted:** role changes, removal and leaving use
definer RPCs with upfront live-group locks. Group creation's trigger and
invitation acceptance remain the only inserters. A separate migration
revokes `INSERT`, `UPDATE`, `DELETE` and `TRUNCATE` on `group_members`
from `PUBLIC`, `anon` and `authenticated`, plus `UPDATE (role)`.
Read grants and SELECT policies remain.

**Group invitations converted:** invite, revoke, accept and decline all
use definer RPCs with explicit authentication, authority and locked
live-group checks. A separate migration revokes `INSERT`, `UPDATE`,
`DELETE` and `TRUNCATE` on `group_invitations` from `PUBLIC`, `anon` and
`authenticated`. There are no column write grants in the migrations to
revoke. Read grants and SELECT policies remain. Email queuing and the
existing invitation constraints and triggers remain in place.

**Event standby converted:** queue replacement and appends use
`set_standby_order` and `add_players_to_event`; promotion uses internal
definer functions. Both public RPCs explicitly check the existing session,
use an empty search path, and retain group-before-event locking. A separate
migration revokes `INSERT`, `UPDATE`, `DELETE` and `TRUNCATE` for
`PUBLIC`, `anon` and `authenticated`. No column write grants occur in the
migrations. Read grants and SELECT policies remain.

All seven tables have write-grant revocations in migration files.
With every write grant gone, the old INSERT, UPDATE and DELETE policies
could never apply; a later migration drops them, leaving SELECT only.
Any future invoker RPC must retain the table privileges it needs or
become a definer with its own guards before those privileges are revoked.

The app never writes directly to these tables.

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
- **Event invite email** — `add_players_to_event` collects the ids of
  the invitations it creates (not standby rows) and calls
  `send_event_invitation_email` once at the end; `promote_standby` does
  the same for the players it promotes. One request per call, not per
  player: Resend rate-limits requests, and a 20-player add would be
  throttled. The Edge Function `send-event-invitation-email` re-reads
  the invitations, sends only those still `pending`, and uses Resend's
  batch endpoint.

  Unlike the group invite, **a missing Vault entry never fails the
  write** — it logs a warning and skips the email. Promotion runs inside
  a member's own RSVP change, and a missing secret must not stop anyone
  dropping out. See DECISIONS.md.

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
