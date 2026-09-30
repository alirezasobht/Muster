create or replace function public.reject_if_event_started_self()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  if old.starts_at <= clock_timestamp() then
    raise exception 'event has already started';
  end if;

  return new;
end;
$$;