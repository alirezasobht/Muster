-- Read the event, roster, queue and names in one statement snapshot.
-- Nested arrays keep the API row limit from truncating the roster/queue.
-- The invoker's existing SELECT grants and RLS apply to every table.

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
            'capacity', e.capacity
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

revoke all on function public.get_event_detail(uuid) from public, anon;
grant execute on function public.get_event_detail(uuid) to authenticated;
