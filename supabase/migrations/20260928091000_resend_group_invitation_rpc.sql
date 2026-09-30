-- Adds resend_group_invitation, an admin-only action on an existing pending
-- invitation. Throttled to once per calendar day, checked against
-- last_sent_at alone — no per-admin or per-group limit stacks on top.
--
-- The caller passes tz rather than the function assuming one: the app
-- already has MusterTimeZone as its single notion of "what day is it"
-- (CONTEXT.md, "Time zones") and sends its id ('Australia/Sydney', for
-- now every group). Once groups or events carry their own tz column, the
-- app switches to sending that instead — this function needs no change.
--
-- send_group_invitation_email now stamps last_sent_at on every send, so the
-- throttle also covers the email invite_group_member_by_email fires on
-- creation, not just resends.
--
-- The day-boundary check is its own function, generic on the zone, so
-- resend_event_invitation can share it later rather than duplicating the
-- math, and so can any other throttle keyed to a calendar day. It is
-- called only from within resend_group_invitation's security definer
-- context, the same as send_group_invitation_email below, so it carries no
-- grant to authenticated.
--
-- tz is an IANA name, not an offset — Postgres has no dedicated timezone
-- type, and a name rather than a fixed offset is what makes the boundary
-- follow DST.

create or replace function private.sent_within_today(last_sent_at timestamptz, tz text)
returns boolean
language sql
stable
set search_path = ''
as $$
    select last_sent_at is not null
        and last_sent_at >= date_trunc('day', now() at time zone tz) at time zone tz;
$$;

revoke all on function private.sent_within_today(timestamptz, text) from public, anon;

create or replace function public.send_group_invitation_email(
    invitation_id uuid
)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
    inv public.group_invitations;
    project_url text;
    webhook_secret text;
begin
    if auth.uid() is null then
        raise exception 'not signed in';
    end if;

    select *
    into inv
    from public.group_invitations
    where id = invitation_id
      and status = 'pending';

    if not found then
        raise exception 'pending invitation not found';
    end if;

    if not private.is_group_admin(inv.group_id) then
        raise exception 'only an admin can send invitation emails';
    end if;

    select decrypted_secret
    into project_url
    from vault.decrypted_secrets
    where name = 'project_url';

    select decrypted_secret
    into webhook_secret
    from vault.decrypted_secrets
    where name = 'group_invitation_webhook_secret';

    if project_url is null or webhook_secret is null then
        raise exception 'email configuration is missing';
    end if;

    perform net.http_post(
        url := project_url || '/functions/v1/send-group-invitation-email',
        headers := jsonb_build_object(
            'Content-Type', 'application/json',
            'x-webhook-secret', webhook_secret
        ),
        body := jsonb_build_object(
            'invitation_id', invitation_id
        )
    );

    update public.group_invitations
    set last_sent_at = now()
    where id = invitation_id;
end;
$$;

create or replace function public.resend_group_invitation(
    group_id uuid,
    invitation_id uuid,
    tz text
)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
    inv public.group_invitations;
begin
    if auth.uid() is null then
        raise exception 'not signed in';
    end if;

    select gi.* into inv
    from public.group_invitations gi
    where gi.group_id = resend_group_invitation.group_id
      and gi.id = resend_group_invitation.invitation_id
      and gi.status = 'pending'
    for update;

    if not found then
        raise exception 'group invitation not found';
    end if;

    perform 1 from public.groups g
    where g.id = resend_group_invitation.group_id and g.archived_at is null
    for share;

    if not found then
        raise exception 'group not found or archived';
    end if;

    if not private.is_group_admin(resend_group_invitation.group_id) then
        raise exception 'only an admin can resend invitations';
    end if;

    if private.sent_within_today(inv.last_sent_at, resend_group_invitation.tz) then
        raise exception 'invitation sent too recently';
    end if;

    perform public.send_group_invitation_email(inv.id);
end;
$$;

revoke all on function public.resend_group_invitation(uuid, uuid, text) from public, anon;
grant execute on function public.resend_group_invitation(uuid, uuid, text) to authenticated;
