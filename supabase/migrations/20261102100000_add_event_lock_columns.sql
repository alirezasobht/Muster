-- An admin can lock an unstarted event until an admin unlocks it.
-- locked_by survives as null when the locking admin deletes their
-- account, so the lock itself outlives them. Existing SELECT grants and
-- events_select cover the new columns; there are still no write grants.

alter table public.events
    add column locked_at timestamptz,
    add column locked_by uuid references public.profiles (id) on delete set null,
    add constraint events_locked_by_needs_locked_at
        check (locked_at is not null or locked_by is null);
