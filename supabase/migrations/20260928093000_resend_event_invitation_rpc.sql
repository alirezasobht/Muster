-- Adds resend_event_invitation, an admin-only action on a still-pending
-- event invitation of an event that hasn't started. Same shape as
-- resend_group_invitation: throttled to once per calendar day in the
-- caller-supplied tz, checked against the invitation's own last_sent_at
-- alone.
--
-- send_event_invitation_email now stamps last_sent_at on every batch it
-- sends, so the throttle also covers the sends add_players_to_event and
-- promote_standby fire on creation/promotion, not just resends.

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

    update public.event_invitations
    set last_sent_at = now()
    where id = any(invitation_ids);
end;
$$;

revoke all on function private.send_event_invitation_email(uuid[])
    from public, anon, authenticated;

create or replace function public.resend_event_invitation(
    event_id uuid,
    profile_id uuid,
    tz text
)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
    gid uuid;
    ts timestamptz;
    inv public.event_invitations;
begin
    if auth.uid() is null then
        raise exception 'not signed in';
    end if;

    select e.group_id into gid
    from public.events e
    where e.id = resend_event_invitation.event_id;

    if not found then
        raise exception 'no such event';
    end if;

    perform 1 from public.groups g
    where g.id = gid and g.archived_at is null
    for share;

    if not found then
        raise exception 'group not found or archived';
    end if;

    select e.starts_at into ts
    from public.events e
    where e.id = resend_event_invitation.event_id and e.group_id = gid
    for update;

    if not found then
        raise exception 'no such event';
    end if;

    if not private.is_group_admin(gid) then
        raise exception 'only an admin can resend an event invitation';
    end if;

    if ts <= pg_catalog.clock_timestamp() then
        raise exception 'event has already started';
    end if;

    select ei.* into inv
    from public.event_invitations ei
    where ei.event_id = resend_event_invitation.event_id
      and ei.profile_id = resend_event_invitation.profile_id
      and ei.group_id = gid
      and ei.status = 'pending'
    for update;

    if not found then
        raise exception 'event invitation not found';
    end if;

    if private.sent_within_today(inv.last_sent_at, resend_event_invitation.tz) then
        raise exception 'invitation sent too recently';
    end if;

    perform private.send_event_invitation_email(array[inv.id]);
end;
$$;

revoke all on function public.resend_event_invitation(uuid, uuid, text) from public, anon;
grant execute on function public.resend_event_invitation(uuid, uuid, text) to authenticated;
