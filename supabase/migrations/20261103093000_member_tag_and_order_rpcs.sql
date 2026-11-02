-- Moving one member between tags, and the order of the tags. A move is
-- one row, so two admins moving different members never overwrite each
-- other. Tag order is replaced whole like set_standby_order: the client
-- sends the result it wants and never computes a position.

create or replace function public.set_member_tag(
    group_id uuid,
    profile_id uuid,
    tag_id uuid
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

    perform 1 from public.groups g
    where g.id = set_member_tag.group_id and g.archived_at is null
    for no key update;

    if not found then
        raise exception 'group not found or archived';
    end if;

    if not private.is_group_admin(set_member_tag.group_id) then
        raise exception 'only an admin can manage tags';
    end if;

    if set_member_tag.tag_id is null then
        delete from public.group_member_tags mt
        where mt.group_id = set_member_tag.group_id
          and mt.profile_id = set_member_tag.profile_id;
        return;
    end if;

    -- Deleted since the screen loaded, or not this group's.
    if not exists (
        select 1 from public.group_tags t
        where t.id = set_member_tag.tag_id
          and t.group_id = set_member_tag.group_id
    ) then
        raise exception 'tag not found';
    end if;

    -- Left the group since the screen loaded.
    if not exists (
        select 1 from public.group_members gm
        where gm.group_id = set_member_tag.group_id
          and gm.profile_id = set_member_tag.profile_id
    ) then
        return;
    end if;

    insert into public.group_member_tags (group_id, profile_id, tag_id)
    values (set_member_tag.group_id, set_member_tag.profile_id, set_member_tag.tag_id)
    on conflict on constraint group_member_tags_pkey
    do update set tag_id = excluded.tag_id;
end;
$$;

revoke all on function public.set_member_tag(uuid, uuid, uuid) from public, anon;
grant execute on function public.set_member_tag(uuid, uuid, uuid) to authenticated;

create or replace function public.set_group_tag_order(group_id uuid, tag_ids uuid[])
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
    where g.id = set_group_tag_order.group_id and g.archived_at is null
    for no key update;

    if not found then
        raise exception 'group not found or archived';
    end if;

    if not private.is_group_admin(set_group_tag_order.group_id) then
        raise exception 'only an admin can manage tags';
    end if;

    if pg_catalog.cardinality(coalesce(tag_ids, '{}'::uuid[])) <> (
        select count(distinct x) from pg_catalog.unnest(tag_ids) x
    ) then
        raise exception 'tag listed twice';
    end if;

    -- Listed tags first in the given order, then any created meanwhile in
    -- their old order. Ids that no longer exist match nothing.
    with ordered as (
        select t.id,
               pg_catalog.row_number() over (order by o.n nulls last, t.position) as rn
        from public.group_tags t
        left join pg_catalog.unnest(coalesce(tag_ids, '{}'::uuid[]))
            with ordinality as o(id, n) on o.id = t.id
        where t.group_id = set_group_tag_order.group_id
    )
    update public.group_tags t
    set position = ordered.rn
    from ordered
    where t.id = ordered.id
      and t.position <> ordered.rn;
end;
$$;

revoke all on function public.set_group_tag_order(uuid, uuid[]) from public, anon;
grant execute on function public.set_group_tag_order(uuid, uuid[]) to authenticated;
