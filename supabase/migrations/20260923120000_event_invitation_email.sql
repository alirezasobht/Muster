-- Email players when they are invited to an event: directly by an admin,
-- or by promotion from standby. Promotion runs inside whoever freed the
-- slot, often a member's own RSVP change, so the send has no caller check
-- and never raises on missing configuration; it warns and skips instead.

create or replace function private.send_event_invitation_email(
    invitation_ids uuid[]
)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
    project_url text;
    webhook_secret text;
begin
    if coalesce(pg_catalog.array_length(invitation_ids, 1), 0) = 0 then
        return;
    end if;

    select decrypted_secret
    into project_url
    from vault.decrypted_secrets
    where name = 'project_url';

    select decrypted_secret
    into webhook_secret
    from vault.decrypted_secrets
    where name = 'event_invitation_webhook_secret';

    if project_url is null or webhook_secret is null then
        raise warning 'event invitation email skipped: email configuration is missing';
        return;
    end if;

    perform net.http_post(
        url := project_url || '/functions/v1/send-event-invitation-email',
        headers := pg_catalog.jsonb_build_object(
            'Content-Type', 'application/json',
            'x-webhook-secret', webhook_secret
        ),
        body := pg_catalog.jsonb_build_object(
            'invitation_ids', pg_catalog.to_jsonb(invitation_ids)
        )
    );
end;
$$;

revoke all on function private.send_event_invitation_email(uuid[])
    from public, anon, authenticated;

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
    new_id        uuid;
    new_ids       uuid[] := '{}';
begin
    if auth.uid() is null then
        raise exception 'not signed in';
    end if;

    -- Same lock order as set_standby_order: group before event. That order
    -- is fixed everywhere both are taken: member-removal RPCs lock the
    -- group before their cascades reach promote_standby, which locks the
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
            insert into public.event_invitations as ei (event_id, group_id, profile_id, status)
            values (add_players_to_event.event_id, ev.group_id, pid, 'pending')
            returning ei.id into new_id;
            new_ids := new_ids || new_id;
            occupied := occupied + 1;
        else
            next_position := next_position + 1;
            insert into public.event_standby (event_id, group_id, profile_id, position)
            values (add_players_to_event.event_id, ev.group_id, pid, next_position);
        end if;
    end loop;

    -- One request for the whole batch: Resend rate-limits per request, and
    -- one request per player would be throttled.
    perform private.send_event_invitation_email(new_ids);
end;
$$;

revoke all on function public.add_players_to_event(uuid, uuid[]) from public, anon;
grant execute on function public.add_players_to_event(uuid, uuid[]) to authenticated;

create or replace function public.promote_standby(eid uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  cap      int;
  ts       timestamptz;
  gid      uuid;
  occupied int;
  nxt      event_standby;
  new_id   uuid;
  new_ids  uuid[] := '{}';
begin
  -- Group before event, as in set_standby_order. FOR SHARE conflicts
  -- with the UPDATE that archives, so promotion cannot land in a group
  -- being archived concurrently. When this runs deferred inside a
  -- membership change, the caller already holds FOR UPDATE on the same
  -- row, which subsumes this.
  select group_id into gid from events where id = eid;
  if not found then
    return;
  end if;

  perform 1 from groups
  where id = gid and archived_at is null
  for share;
  if not found then
    return;
  end if;

  select capacity, starts_at into cap, ts
  from events where id = eid
  for update;

  -- event gone (cascade) or already started. clock_timestamp() because
  -- this runs deferred, at commit, which can be well after the
  -- transaction's now().
  if not found or ts <= clock_timestamp() then
    return;
  end if;

  loop
    select count(*) into occupied
    from event_invitations
    where event_id = eid and status in ('pending', 'in');

    exit when occupied >= cap;

    select * into nxt
    from event_standby
    where event_id = eid
    order by position
    limit 1;

    exit when not found;

    -- delete first, or the mutual-exclusion trigger rejects the insert
    delete from event_standby
    where event_id = eid and profile_id = nxt.profile_id;

    insert into event_invitations (event_id, group_id, profile_id, status)
    values (eid, nxt.group_id, nxt.profile_id, 'pending')
    returning id into new_id;
    new_ids := new_ids || new_id;
  end loop;

  perform private.send_event_invitation_email(new_ids);
end;
$$;

revoke all on function public.promote_standby(uuid) from public, anon, authenticated;
