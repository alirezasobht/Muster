-- Rule 14b: a locked event takes no invitation or queue inserts or
-- updates, the same as a started one. Deletes stay exempt, so removing a
-- member or deleting an account still clears their rows. Started wins
-- the message: a lock left on past kickoff reads as started.

create or replace function public.reject_if_event_started()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
    ts timestamptz;
    locked timestamptz;
begin
    select e.starts_at, e.locked_at into ts, locked
    from public.events e
    where e.id = new.event_id;

    if not found then
        return new;
    end if;

    if ts <= pg_catalog.clock_timestamp() then
        raise exception 'event has already started';
    end if;

    if locked is not null then
        raise exception 'event is locked';
    end if;

    return new;
end;
$$;
