-- An admin can cancel an unstarted event, for good (rule 14c). Nothing
-- in the app clears cancelled_at; restoring is a dashboard edit.
-- cancelled_by survives as null when the cancelling admin deletes their
-- account. Existing SELECT grants and events_select cover the new
-- columns; there are still no write grants.

alter table public.events
    add column cancelled_at timestamptz,
    add column cancelled_by uuid references public.profiles (id) on delete set null,
    add constraint events_cancelled_by_needs_cancelled_at
        check (cancelled_at is not null or cancelled_by is null);
