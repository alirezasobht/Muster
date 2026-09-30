-- Complete the earlier authenticated INSERT/UPDATE/DELETE revoke: cover
-- PUBLIC, anon and TRUNCATE too. Queue RPCs and promotion are definers.
-- No column-level write grants were introduced for event_standby.
-- Keep read grants and RLS for queue queries.

revoke insert, update, delete, truncate on table public.event_standby
    from public, anon, authenticated;
