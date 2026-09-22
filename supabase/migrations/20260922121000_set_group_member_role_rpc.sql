-- Add an RPC for changing a group member's role.
-- This replaces direct client updates to public.group_members.role.

create or replace function public.set_group_member_role(
    group_id uuid,
    profile_id uuid,
    role text
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

    if role not in ('admin', 'member') then
        raise exception 'invalid role';
    end if;

    if not private.is_group_admin(group_id) then
        raise exception 'only an admin can change member roles';
    end if;

    update public.group_members gm
    set role = set_group_member_role.role
    where gm.group_id = set_group_member_role.group_id
      and gm.profile_id = set_group_member_role.profile_id;

    if not found then
        raise exception 'group member not found';
    end if;

    -- The existing deferred group_keeps_an_admin constraint trigger
    -- prevents demoting the final admin in a group.
end;
$$;

revoke all on function public.set_group_member_role(uuid, uuid, text) from public;
revoke all on function public.set_group_member_role(uuid, uuid, text) from anon;
grant execute on function public.set_group_member_role(uuid, uuid, text) to authenticated;
