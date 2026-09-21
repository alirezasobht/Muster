-- Wires group invitation email sending to the Supabase Edge Function.
--
-- send_group_invitation_email() now sends the invitation ID to the
-- send-group-invitation-email Edge Function using pg_net.
--
-- The Edge Function request is authenticated with the shared secret
-- stored in Supabase Vault as group_invitation_webhook_secret.

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
end;
$$;