-- Aggregate invitations before the API row limit is applied. RLS still
-- controls both tables: this read RPC uses the caller's SELECT privileges.

create or replace function public.list_upcoming_events(gid uuid)
returns table (
    id uuid,
    group_id uuid,
    title text,
    starts_at timestamptz,
    location text,
    capacity integer,
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
