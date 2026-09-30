-- add_players_to_event: a player removed from the group since the picker
-- loaded is skipped, not raised on.
--
-- The check was the only thing in the loop that aborted. Everything else
-- tolerates a stale list — someone already invited or queued is skipped —
-- but one removed player rolled back the whole batch, so none of the others
-- were added either. Both are the same staleness.
--
-- Nothing is lost by skipping: the composite FK on (group_id, profile_id)
-- already makes a non-member impossible to insert, so the check only ever
-- produced a nicer message than a foreign key violation.
--
-- The comments 20260922133000 dropped are restored here.

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
    -- Same lock order as set_standby_order: group before event. That order
    -- is fixed everywhere both are taken, since ensure_admin_remains locks
    -- the group and its cascade reaches promote_standby, which locks the
    -- event.
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

    -- clock_timestamp(), not now(): a request that began before kickoff and
    -- then waited on the event lock could otherwise still pass the cutoff.
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

    -- Both skips below are the same case: the picker's list went stale
    -- between loading and confirming.
    foreach pid in array coalesce(profile_ids, '{}'::uuid[]) loop
        if not exists (
            select 1 from public.group_members gm
            where gm.group_id = ev.group_id and gm.profile_id = pid
        ) then
            continue;
        end if;

        -- Already holds either kind of row (rule 13, mutual exclusion).
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
