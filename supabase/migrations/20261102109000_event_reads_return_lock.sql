-- The event reads carry the lock. get_event_detail adds locked_at and
-- the locker's name to its event object; a null name with a lock means
-- the locker deleted their account or is hidden by RLS, and the app says
-- "an admin". list_upcoming_events gains a column, which changes its
-- return type, so it is dropped and recreated with its grants. Both stay
-- security invoker.

create or replace function public.get_event_detail(eid uuid)
returns table (
    event jsonb,
    roster jsonb,
    standby jsonb,
    my_status text
)
language sql
stable
security invoker
set search_path = ''
as $$
    select
        pg_catalog.jsonb_build_object(
            'id', e.id,
            'group_id', e.group_id,
            'title', e.title,
            'starts_at', e.starts_at,
            'location', e.location,
            'capacity', e.capacity,
            'locked_at', e.locked_at,
            'locked_by_name', (
                select p.name
                from public.profiles p
                where p.id = e.locked_by
            )
        ),
        coalesce((
            select pg_catalog.jsonb_agg(
                pg_catalog.jsonb_build_object(
                    'profile_id', ei.profile_id,
                    'name', p.name,
                    'status', ei.status
                )
            )
            from public.event_invitations ei
            left join public.profiles p on p.id = ei.profile_id
            where ei.event_id = e.id
        ), '[]'::jsonb),
        coalesce((
            select pg_catalog.jsonb_agg(
                pg_catalog.jsonb_build_object(
                    'profile_id', es.profile_id,
                    'name', p.name
                ) order by es.position
            )
            from public.event_standby es
            left join public.profiles p on p.id = es.profile_id
            where es.event_id = e.id
        ), '[]'::jsonb),
        (
            select ei.status
            from public.event_invitations ei
            where ei.event_id = e.id and ei.profile_id = (select auth.uid())
        )
    from public.events e
    where e.id = $1 and (select auth.uid()) is not null;
$$;

drop function public.list_upcoming_events(uuid);

create function public.list_upcoming_events(gid uuid)
returns table (
    id uuid,
    group_id uuid,
    title text,
    starts_at timestamptz,
    location text,
    capacity integer,
    locked_at timestamptz,
    in_count integer,
    pending_count integer,
    my_status text
)
language sql
stable
security invoker
set search_path = ''
as $$
    select
        e.id,
        e.group_id,
        e.title,
        e.starts_at,
        e.location,
        e.capacity,
        e.locked_at,
        (pg_catalog.count(*) filter (where ei.status = 'in'))::integer,
        (pg_catalog.count(*) filter (where ei.status = 'pending'))::integer,
        (
            select mine.status
            from public.event_invitations mine
            where mine.event_id = e.id
              and mine.profile_id = (select auth.uid())
        )
    from public.events e
    left join public.event_invitations ei on ei.event_id = e.id
    where e.group_id = $1
      and e.starts_at > pg_catalog.statement_timestamp()
      and (select auth.uid()) is not null
    group by e.id
    order by e.starts_at, e.id;
$$;

revoke all on function public.list_upcoming_events(uuid) from public, anon;
grant execute on function public.list_upcoming_events(uuid) to authenticated;
