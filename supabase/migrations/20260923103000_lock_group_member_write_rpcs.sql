-- Lock the live group before checking authority or changing memberships.
-- FOR UPDATE also excludes roster writers' group-share locks before member
-- deletion cascades to invitations/standby and promotion locks events.
-- The existing deferred trigger remains responsible for the last admin.

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

    if role is null or role not in ('admin', 'member') then
        raise exception 'invalid role';
    end if;

    perform 1 from public.groups g
    where g.id = set_group_member_role.group_id and g.archived_at is null
    for update;

    if not found then
        raise exception 'group not found or archived';
    end if;

    if not private.is_group_admin(set_group_member_role.group_id) then
        raise exception 'only an admin can change member roles';
    end if;

    update public.group_members gm
    set role = set_group_member_role.role
    where gm.group_id = set_group_member_role.group_id
      and gm.profile_id = set_group_member_role.profile_id;

    if not found then
        raise exception 'group member not found';
    end if;
end;
$$;

revoke all on function public.set_group_member_role(uuid, uuid, text) from public, anon;
grant execute on function public.set_group_member_role(uuid, uuid, text) to authenticated;

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

    perform 1 from public.groups g
    where g.id = remove_group_member.group_id and g.archived_at is null
    for update;

    if not found then
        raise exception 'group not found or archived';
    end if;

    if not private.is_group_admin(remove_group_member.group_id) then
        raise exception 'only an admin can remove members';
    end if;

    delete from public.group_members gm
    where gm.group_id = remove_group_member.group_id
      and gm.profile_id = remove_group_member.profile_id;

    if not found then
        raise exception 'group member not found';
    end if;
end;
$$;

revoke all on function public.remove_group_member(uuid, uuid) from public, anon;
grant execute on function public.remove_group_member(uuid, uuid) to authenticated;

create or replace function public.leave_group(group_id uuid)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
    if auth.uid() is null then
        raise exception 'not signed in';
    end if;

    perform 1 from public.groups g
    where g.id = leave_group.group_id and g.archived_at is null
    for update;

    if not found then
        raise exception 'group not found or archived';
    end if;

    delete from public.group_members gm
    where gm.group_id = leave_group.group_id
      and gm.profile_id = auth.uid();

    if not found then
        raise exception 'group membership not found';
    end if;
end;
$$;

revoke all on function public.leave_group(uuid) from public, anon;
grant execute on function public.leave_group(uuid) to authenticated;
