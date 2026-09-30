-- Apply after the app uses set_event_rsvp and disinvite_player is a definer.
-- All invitation writes now run through definer RPCs or existing triggers.
-- Keep read grants and RLS for roster queries.

revoke insert, update, delete, truncate on table public.event_invitations
    from public, anon, authenticated;

-- The separate column grant survives a table-level UPDATE revoke.
revoke update (status) on table public.event_invitations
    from public, anon, authenticated;
