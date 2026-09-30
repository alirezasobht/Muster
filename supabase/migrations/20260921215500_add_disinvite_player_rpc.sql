-- Adds an RPC for removing a player from an event.
--
-- Only callers who are allowed to delete the matching event invitation
-- through the existing RLS policy can disinvite a player.
--
-- The function uses SECURITY INVOKER, so it does not bypass RLS.
--
-- Deleting the invitation also allows the existing standby-promotion
-- trigger to run normally if the event has a waiting player.
--
-- Returns true when an invitation was deleted.
-- Returns false when no matching invitation existed.

create or replace function public.disinvite_player(
    event_id uuid,
    profile_id uuid
)
returns boolean
language plpgsql
security invoker
set search_path = ''
as $$
declare
    deleted_count integer;
begin
    delete from public.event_invitations ei
    where ei.event_id = disinvite_player.event_id
      and ei.profile_id = disinvite_player.profile_id;

    get diagnostics deleted_count = row_count;

    return deleted_count > 0;
end;
$$;

revoke all
on function public.disinvite_player(uuid, uuid)
from public, anon;

grant execute
on function public.disinvite_player(uuid, uuid)
to authenticated;