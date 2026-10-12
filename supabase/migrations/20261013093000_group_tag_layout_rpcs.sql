-- Member moves and tag order, each replaced whole like set_standby_order:
-- the client sends the result it wants and never computes a position.
-- Kept apart so a card move can't undo another admin's member moves.
-- Last write wins between admins.

create or replace function public.set_group_tag_layout(group_id uuid, layout jsonb)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
    entry  jsonb;
    tid    uuid;
    pid    uuid;
    i      int;
    listed uuid[];
begin
    if auth.uid() is null then
        raise exception 'not signed in';
    end if;

    perform 1 from public.groups g
    where g.id = set_group_tag_layout.group_id and g.archived_at is null
    for no key update;

    if not found then
        raise exception 'group not found or archived';
    end if;

    if not private.is_group_admin(set_group_tag_layout.group_id) then
        raise exception 'only an admin can manage tags';
    end if;

    if pg_catalog.jsonb_typeof(layout) is distinct from 'array' then
        raise exception 'layout must be an array';
    end if;

    select coalesce(pg_catalog.array_agg(x::uuid), '{}'::uuid[]) into listed
    from pg_catalog.jsonb_array_elements(layout) e,
         pg_catalog.jsonb_array_elements_text(coalesce(e -> 'profile_ids', '[]'::jsonb)) x;

    if pg_catalog.cardinality(listed) <> (
        select count(distinct x) from pg_catalog.unnest(listed) x
    ) then
        raise exception 'member listed twice';
    end if;

    -- Only members under tags that still exist count as listed. Anyone
    -- listed under a deleted tag is untagged rather than left behind at a
    -- position the renumbering below would collide with.
    select coalesce(pg_catalog.array_agg(x::uuid), '{}'::uuid[]) into listed
    from pg_catalog.jsonb_array_elements(layout) e
    join public.group_tags t
        on t.id = (e ->> 'tag_id')::uuid and t.group_id = set_group_tag_layout.group_id,
         pg_catalog.jsonb_array_elements_text(coalesce(e -> 'profile_ids', '[]'::jsonb)) x;

    -- Untags whoever was dropped from a listed tag. Tags left out of
    -- layout keep their members.
    delete from public.group_member_tags mt
    where mt.group_id = set_group_tag_layout.group_id
      and mt.tag_id in (
          select (e ->> 'tag_id')::uuid from pg_catalog.jsonb_array_elements(layout) e
      )
      and mt.profile_id <> all (listed);

    for entry in select e from pg_catalog.jsonb_array_elements(layout) e loop
        tid := (entry ->> 'tag_id')::uuid;

        -- Deleted since the screen loaded, or not this group's.
        if not exists (
            select 1 from public.group_tags t
            where t.id = tid and t.group_id = set_group_tag_layout.group_id
        ) then
            continue;
        end if;

        i := 0;
        for pid in
            select x::uuid
            from pg_catalog.jsonb_array_elements_text(coalesce(entry -> 'profile_ids', '[]'::jsonb))
                with ordinality as a(x, n)
            order by a.n
        loop
            -- Left the group since the screen loaded.
            if not exists (
                select 1 from public.group_members gm
                where gm.group_id = set_group_tag_layout.group_id and gm.profile_id = pid
            ) then
                continue;
            end if;

            i := i + 1;
            insert into public.group_member_tags (group_id, profile_id, tag_id, position)
            values (set_group_tag_layout.group_id, pid, tid, i)
            on conflict on constraint group_member_tags_pkey
            do update set tag_id = excluded.tag_id, position = excluded.position;
        end loop;
    end loop;
end;
$$;

revoke all on function public.set_group_tag_layout(uuid, jsonb) from public, anon;
grant execute on function public.set_group_tag_layout(uuid, jsonb) to authenticated;

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
