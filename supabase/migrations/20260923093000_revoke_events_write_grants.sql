-- Apply after the app uses create_event. Reads and RLS remain available;
-- existing definer RPCs and triggers can still lock events for roster writes.

revoke insert, update, delete, truncate on table public.events
    from public, anon, authenticated;

-- The separate column grants survive a table-level UPDATE revoke.
revoke update (title, starts_at, location) on table public.events
    from public, anon, authenticated;
