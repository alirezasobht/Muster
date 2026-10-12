-- The Members tab in one snapshot: the caller's role, every member, and
-- for admins only the pending invitations and tags. Members get empty
-- pending and tags; RLS already hides the tag tables from them, which
-- also leaves their tag_id null.
-- Nested arrays keep the API row limit from truncating any list.

create or replace function public.get_group_members(group_id uuid)
returns table (
    my_role text,
    members jsonb,
    pending jsonb,
    tags jsonb
)
language sql
stable
security invoker
set search_path = ''
as $$
    with me as (
        select gm.role
        from public.group_members gm
        where gm.group_id = $1
          and gm.profile_id = (select auth.uid())
    )
    select
        me.role,
        coalesce((
            select pg_catalog.jsonb_agg(
                pg_catalog.jsonb_build_object(
                    'profile_id', gm.profile_id,
                    'name', p.name,
                    'role', gm.role,
                    'tag_id', mt.tag_id
                )
            )
            from public.group_members gm
            left join public.profiles p on p.id = gm.profile_id
            left join public.group_member_tags mt
                on mt.group_id = gm.group_id and mt.profile_id = gm.profile_id
            where gm.group_id = $1
        ), '[]'::jsonb),
        case when me.role = 'admin' then coalesce((
            select pg_catalog.jsonb_agg(
                pg_catalog.jsonb_build_object(
                    'invitation_id', gi.id,
                    'email', gi.email
                ) order by gi.email
            )
            from public.group_invitations gi
            where gi.group_id = $1
              and gi.status = 'pending'
        ), '[]'::jsonb) else '[]'::jsonb end,
        case when me.role = 'admin' then coalesce((
            select pg_catalog.jsonb_agg(
                pg_catalog.jsonb_build_object(
                    'id', t.id,
                    'name', t.name,
                    'position', t.position,
                    'members', coalesce((
                        select pg_catalog.jsonb_agg(
                            pg_catalog.jsonb_build_object(
                                'profile_id', mt.profile_id,
                                'position', mt.position
                            ) order by mt.position
                        )
                        from public.group_member_tags mt
                        where mt.tag_id = t.id
                    ), '[]'::jsonb)
                ) order by t.position
            )
            from public.group_tags t
            where t.group_id = $1
        ), '[]'::jsonb) else '[]'::jsonb end
    from me;
$$;

revoke all on function public.get_group_members(uuid) from public, anon;
grant execute on function public.get_group_members(uuid) to authenticated;
