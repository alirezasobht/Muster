-- Adds an RPC for admins to add players to an event: invites while slots
-- remain, then appends the rest to the standby queue, in profile_ids
-- order, all in one transaction. The split is never computed client-side —
-- two admins adding players at the same time would otherwise both see the
-- same free slot (SCHEMA.md rules 11-13, CONTEXT.md Standby).

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
    from public.event_invitations
    where event_id = add_players_to_event.event_id
      and status in ('pending', 'in');

    select coalesce(max(position), 0) into next_position
    from public.event_standby
    where event_id = add_players_to_event.event_id;

    foreach pid in array coalesce(profile_ids, '{}'::uuid[]) loop
        if not exists (
            select 1 from public.group_members
            where group_id = ev.group_id and profile_id = pid
        ) then
            raise exception 'player % is not a member of this group',
                coalesce((select name from public.profiles where id = pid), pid::text);
        end if;

        -- The picker's list can be stale — skip, don't error, on anyone who
        -- already holds either kind of row (rule 13, mutual exclusion).
        if exists (
            select 1 from public.event_invitations
            where event_id = add_players_to_event.event_id and profile_id = pid
        ) or exists (
            select 1 from public.event_standby
            where event_id = add_players_to_event.event_id and profile_id = pid
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

revoke all on function public.add_players_to_event(uuid, uuid[]) from public;
revoke all on function public.add_players_to_event(uuid, uuid[]) from anon;
grant execute on function public.add_players_to_event(uuid, uuid[]) to authenticated;
