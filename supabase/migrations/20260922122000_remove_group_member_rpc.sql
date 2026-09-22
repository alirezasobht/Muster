-- Add an RPC for an admin to remove another member from a group.
-- This replaces direct client deletes from public.group_members.

create or replace function public.remove_group_member(
    group_id uuid,
    profile_id uuid
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

    if profile_id = auth.uid() then
        raise exception 'use leave group instead';
    end if;

    if not private.is_group_admin(group_id) then
        raise exception 'only an admin can remove members';
    end if;

    delete from public.group_members gm
    where gm.group_id = remove_group_member.group_id
      and gm.profile_id = remove_group_member.profile_id;

    if not found then
        raise exception 'group member not found';
    end if;

    -- Existing group_keeps_an_admin constraint trigger prevents
    -- removal of the final admin in a group.
end;
$$;

revoke all on function public.remove_group_member(uuid, uuid) from public;
revoke all on function public.remove_group_member(uuid, uuid) from anon;
grant execute on function public.remove_group_member(uuid, uuid) to authenticated;
