# Muster — Database Schema

Postgres via Supabase. No SQL written yet — this is the agreed design.

## Tables

### profiles
Mirrors `auth.users`.

| Column | Type | Notes |
|---|---|---|
| id | uuid PK | = `auth.users.id` |
| name | text | |
| email | text | |

### groups

| Column | Type | Notes |
|---|---|---|
| id | uuid PK | |
| name | text | |
| created_by | uuid | → profiles |
| created_at | timestamptz | |

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
| email | text | |
| invited_by | uuid | → profiles |
| status | text | `pending` \| `accepted` \| `declined` |
| token | text | for the accept link |
| created_at | timestamptz | |
| responded_at | timestamptz | null until answered |

Partial unique index on `(group_id, email) WHERE status = 'pending'` —
one open invite per person per group.

### events

| Column | Type | Notes |
|---|---|---|
| id | uuid PK | |
| group_id | uuid | → groups, cascade |
| title | text | |
| starts_at | timestamptz | |
| location | text | |
| capacity | int | confirmed players before standby |
| created_by | uuid | → profiles |

### invitations
Event-level RSVP.

| Column | Type | Notes |
|---|---|---|
| id | uuid PK | |
| event_id | uuid | → events, cascade |
| group_id | uuid | denormalised from events — needed for the FK below |
| profile_id | uuid | |
| status | text | `pending` \| `in` \| `out` |
| is_standby | bool | |
| standby_order | int | queue position, null if not standby |
| responded_at | timestamptz | |

- UNIQUE `(event_id, profile_id)`
- FK `(group_id, profile_id)` → `group_members (group_id, profile_id)`
  `ON DELETE CASCADE`

That composite FK is deliberate: removing someone from a group wipes all
their RSVPs in that group automatically.

### Deferred

`device_tokens` — for FCM. Not in the MVP.

## Rules

### Enforced in the database

1. **Group visibility** — a user can select a group only if a
   `group_members` row exists for them and that group. The same predicate
   cascades to `events` and `invitations`.
2. **At least one admin** — trigger on `group_members` delete and update
   rejecting any operation that would leave a group with zero admins.
   Covers: last admin leaving, last admin demoting themselves, an admin
   demoting the only other admin.
3. **Admin-only actions** — RLS policies checking `role = 'admin'` for:
   create event, invite member, promote/demote, remove member, delete group.
4. **Group creation** — creator's `group_members` row with `role = 'admin'`
   is written in the same transaction as the group.
5. **Cascades** — deleting a group clears its members, events, and
   invitations via `ON DELETE CASCADE`.

### Application logic

- **Invite flow** — admin enters email → row in `group_invitations` →
  Edge Function sends the email via Resend. On accept, a function verifies
  the token, creates the `group_members` row, and marks the invitation
  accepted.
- **Standby promotion** — automatic, no admin approval. When confirmed
  players drop below `capacity`, promote the lowest `standby_order`,
  clear their standby flag, and notify them. Triggers on: a player
  declining, and **a member being removed from the group** (the FK
  cascade silently frees a spot).
- **Event freeze** — `starts_at` is the only cutoff. Once reached, the
  event is immutable: no RSVP changes, no standby promotions, no new
  invites. There is no separate RSVP deadline.

## Open questions

None.
