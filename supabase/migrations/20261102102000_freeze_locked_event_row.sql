-- A locked event's own row changes only to unlock it. Account deletion
-- still nulls created_by and locked_by, on started and locked events
-- alike, or deleting the profile would fail.

create or replace function public.reject_if_event_started_self()
returns trigger
language plpgsql
set search_path = ''
as $$
declare
    unchanged public.events := old;
begin
    unchanged.created_by := new.created_by;
    unchanged.locked_by := new.locked_by;
    if unchanged is not distinct from new
       and (new.locked_by is null or new.locked_by is not distinct from old.locked_by) then
        return new;
    end if;

    if old.starts_at <= pg_catalog.clock_timestamp() then
        raise exception 'event has already started';
    end if;

    if old.locked_at is not null then
        unchanged := old;
        unchanged.locked_at := new.locked_at;
        unchanged.locked_by := new.locked_by;
        if unchanged is distinct from new then
            raise exception 'event is locked';
        end if;
    end if;

    return new;
end;
$$;
