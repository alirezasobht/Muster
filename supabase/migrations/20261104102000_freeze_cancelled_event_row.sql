-- A cancelled event's own row takes no change at all, so it can't be
-- edited, locked or unlocked. A locked row also takes the cancel itself.
-- Account deletion still nulls created_by, locked_by and cancelled_by on
-- any event, or deleting the profile would fail.

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
    unchanged.cancelled_by := new.cancelled_by;
    if unchanged is not distinct from new
       and (new.locked_by is null or new.locked_by is not distinct from old.locked_by)
       and (new.cancelled_by is null or new.cancelled_by is not distinct from old.cancelled_by) then
        return new;
    end if;

    if old.starts_at <= pg_catalog.clock_timestamp() then
        raise exception 'event has already started';
    end if;

    if old.cancelled_at is not null then
        raise exception 'event is cancelled';
    end if;

    if old.locked_at is not null then
        unchanged := old;
        unchanged.locked_at := new.locked_at;
        unchanged.locked_by := new.locked_by;
        unchanged.cancelled_at := new.cancelled_at;
        unchanged.cancelled_by := new.cancelled_by;
        if unchanged is distinct from new then
            raise exception 'event is locked';
        end if;
    end if;

    return new;
end;
$$;
