-- Create, rename and delete tags. Admins only, live group locked first.
-- `for no key update` serialises every tag write with the others and with
-- membership changes, without blocking foreign-key checks on the group.

create or replace function public.create_group_tag(
    group_id uuid,
    name text,
    profile_id uuid default null
)
returns setof public.group_tags
language plpgsql
security definer
set search_path = ''
as $$
declare
    new_tag public.group_tags;
begin
    if auth.uid() is null then
        raise exception 'not signed in';
    end if;

    perform 1 from public.groups g
    where g.id = create_group_tag.group_id and g.archived_at is null
    for no key update;

    if not found then
        raise exception 'group not found or archived';
    end if;

    if not private.is_group_admin(create_group_tag.group_id) then
        raise exception 'only an admin can manage tags';
    end if;

    insert into public.group_tags as t (group_id, name, position)
    values (
        create_group_tag.group_id,
        pg_catalog.btrim(create_group_tag.name),
        coalesce((
            select max(t2.position) from public.group_tags t2
            where t2.group_id = create_group_tag.group_id
        ), 0) + 1
    )
    returning t.* into new_tag;

    -- Moves the member out of any other tag. A member who left since the
    -- screen loaded is skipped.
    if create_group_tag.profile_id is not null and exists (
        select 1 from public.group_members gm
        where gm.group_id = create_group_tag.group_id
          and gm.profile_id = create_group_tag.profile_id
    ) then
        insert into public.group_member_tags (group_id, profile_id, tag_id)
        values (create_group_tag.group_id, create_group_tag.profile_id, new_tag.id)
        on conflict on constraint group_member_tags_pkey
        do update set tag_id = excluded.tag_id;
    end if;

    return next new_tag;
end;
$$;

revoke all on function public.create_group_tag(uuid, text, uuid) from public, anon;
grant execute on function public.create_group_tag(uuid, text, uuid) to authenticated;

create or replace function public.update_group_tag(tag_id uuid, name text)
returns setof public.group_tags
language plpgsql
security definer
set search_path = ''
as $$
declare
    gid uuid;
begin
    if auth.uid() is null then
        raise exception 'not signed in';
    end if;

    select t.group_id into gid
    from public.group_tags t
    where t.id = update_group_tag.tag_id;

    if not found then
        raise exception 'tag not found';
    end if;

    perform 1 from public.groups g
    where g.id = gid and g.archived_at is null
    for no key update;

    if not found then
        raise exception 'group not found or archived';
    end if;

    if not private.is_group_admin(gid) then
        raise exception 'only an admin can manage tags';
    end if;

    return query
    update public.group_tags t
    set name = pg_catalog.btrim(update_group_tag.name)
    where t.id = update_group_tag.tag_id
    returning t.*;

    if not found then
        raise exception 'tag not found';
    end if;
end;
$$;

revoke all on function public.update_group_tag(uuid, text) from public, anon;
grant execute on function public.update_group_tag(uuid, text) to authenticated;

create or replace function public.delete_group_tag(tag_id uuid)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
    gid uuid;
begin
    if auth.uid() is null then
        raise exception 'not signed in';
    end if;

    select t.group_id into gid
    from public.group_tags t
    where t.id = delete_group_tag.tag_id;

    if not found then
        raise exception 'tag not found';
    end if;

    perform 1 from public.groups g
    where g.id = gid and g.archived_at is null
    for no key update;

    if not found then
        raise exception 'group not found or archived';
    end if;

    if not private.is_group_admin(gid) then
        raise exception 'only an admin can manage tags';
    end if;

    -- Cascades to group_member_tags: its members become untagged.
    delete from public.group_tags t
    where t.id = delete_group_tag.tag_id;

    if not found then
        raise exception 'tag not found';
    end if;
end;
$$;

revoke all on function public.delete_group_tag(uuid) from public, anon;
grant execute on function public.delete_group_tag(uuid) to authenticated;
