-- Fixes add_players_to_event: every bare "event_id" inside the function
-- body was ambiguous between the parameter of that name and the column of
-- the same name on event_invitations/event_standby. PL/pgSQL's default
-- variable_conflict = error means that's a hard failure (42702) rather
-- than a silent wrong answer, so every call raised "column reference
-- event_id is ambiguous" instead of running. Table aliases make every
-- reference explicit; behaviour is otherwise unchanged.

create or replace function public.add_players_to_event(
    event_id uuid,
    profile_ids uuid[]
)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
    ev            public.events;
    gid           uuid;
    pid           uuid;
    occupied      int;
    next_position int;
begin
    select group_id into gid
    from public.events
    where id = add_players_to_event.event_id;

    if not found then
        raise exception 'no such event';
    end if;

    perform 1 from public.groups
    where id = gid and archived_at is null
    for share;

    if not found then
        raise exception 'group is archived';
    end if;

    select * into ev
    from public.events
    where id = add_players_to_event.event_id
    for update;

    if not found then
        raise exception 'no such event';
    end if;

    if not private.is_group_admin(ev.group_id) then
        raise exception 'only an admin can add players';
    end if;

    if ev.starts_at <= clock_timestamp() then
        raise exception 'event has already started';
    end if;

    select count(*) into occupied
    from public.event_invitations ei
    where ei.event_id = add_players_to_event.event_id
      and ei.status in ('pending', 'in');

    select coalesce(max(es.position), 0) into next_position
    from public.event_standby es
    where es.event_id = add_players_to_event.event_id;

    foreach pid in array coalesce(profile_ids, '{}'::uuid[]) loop
        if not exists (
            select 1 from public.group_members gm
            where gm.group_id = ev.group_id and gm.profile_id = pid
        ) then
            raise exception 'player % is not a member of this group',
                coalesce((select name from public.profiles where id = pid), pid::text);
        end if;

        -- The picker's list can be stale — skip, don't error, on anyone who
        -- already holds either kind of row (rule 13, mutual exclusion).
        if exists (
            select 1 from public.event_invitations ei
            where ei.event_id = add_players_to_event.event_id and ei.profile_id = pid
        ) or exists (
            select 1 from public.event_standby es
            where es.event_id = add_players_to_event.event_id and es.profile_id = pid
        ) then
            continue;
        end if;

        if occupied < ev.capacity then
            insert into public.event_invitations (event_id, group_id, profile_id, status)
            values (add_players_to_event.event_id, ev.group_id, pid, 'pending');
            occupied := occupied + 1;
        else
            next_position := next_position + 1;
            insert into public.event_standby (event_id, group_id, profile_id, position)
            values (add_players_to_event.event_id, ev.group_id, pid, next_position);
        end if;
    end loop;
end;
$$;
