-- Rule 14c: a cancelled event takes no invitation or queue inserts or
-- updates, the same as a locked one but for good. Deletes stay exempt.
-- Message order is started, then cancelled, then locked.

create or replace function public.reject_if_event_started()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
    ts timestamptz;
    locked timestamptz;
    cancelled timestamptz;
begin
    select e.starts_at, e.locked_at, e.cancelled_at into ts, locked, cancelled
    from public.events e
    where e.id = new.event_id;

    if not found then
        return new;
    end if;

    if ts <= pg_catalog.clock_timestamp() then
        raise exception 'event has already started';
    end if;

    if cancelled is not null then
        raise exception 'event is cancelled';
    end if;

    if locked is not null then
        raise exception 'event is locked';
    end if;

    return new;
end;
$$;
