-- Add an RPC for the signed-in member to leave a group.
-- This replaces direct client deletes from public.group_members.

create or replace function public.leave_group(
    group_id uuid
)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
    if auth.uid() is null then
        raise exception 'not signed in';
    end if;

    delete from public.group_members gm
    where gm.group_id = leave_group.group_id
      and gm.profile_id = auth.uid();

    if not found then
        raise exception 'group membership not found';
    end if;

    -- Existing group_keeps_an_admin constraint trigger prevents
    -- the final admin from leaving the group.
end;
$$;

revoke all on function public.leave_group(uuid) from public;
revoke all on function public.leave_group(uuid) from anon;
grant execute on function public.leave_group(uuid) to authenticated;
