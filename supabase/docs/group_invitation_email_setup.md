# Group Invitation Email Setup

This document records the setup used for Muster group invitation emails so it can be reproduced later in production.

## 1. Resend

Create and verify the sending domain in Resend.

Current sender:

```text
Muster <noreply@send.musterapp.fyi>
```

Create a Resend API key for the Supabase Edge Function.

Create and publish the template:

```text
group-member-invitation
```

Template variables:

```text
GROUP_NAME
INVITER_NAME
RECIPIENT_EMAIL
CONTACT_EMAIL
ANDROID_APP_URL
WEB_APP_URL
```

Current values used by the Edge Function:

```text
CONTACT_EMAIL = muster.team.app@gmail.com
WEB_APP_URL = https://musterteamapp.netlify.app/
ANDROID_APP_URL = placeholder until Play Store release
```

`RECIPIENT_EMAIL` fallback in Resend:

```text
the email address that received this invitation
```

## 2. Supabase Edge Function secrets

In Supabase:

**Edge Functions → Secrets**

Add:

```text
RESEND_API_KEY
GROUP_INVITATION_WEBHOOK_SECRET
```

`GROUP_INVITATION_WEBHOOK_SECRET` should be a long random value.

## 3. Edge Function

Create and deploy:

```text
send-group-invitation-email
```

The function:

1. Receives `invitation_id`.
2. Verifies the `x-webhook-secret` header.
3. Loads the pending invitation from `group_invitations`.
4. Loads the group name and inviter name.
5. Sends the email through Resend using the `group-member-invitation` template.

The function passes these variables to Resend:

```text
GROUP_NAME
INVITER_NAME
RECIPIENT_EMAIL
CONTACT_EMAIL
ANDROID_APP_URL
WEB_APP_URL
```

Turn this setting off:

```text
Verify JWT with legacy secret: OFF
```

The function is protected instead by:

```text
x-webhook-secret
```

which must match `GROUP_INVITATION_WEBHOOK_SECRET`.

## 4. Enable pg_net

Run in Supabase SQL Editor:

```sql
create extension if not exists pg_net
with schema extensions;
```

This allows Postgres to call the Edge Function.

## 5. Supabase Vault

Create these Vault secrets:

```text
project_url
group_invitation_webhook_secret
```

Development `project_url`:

```text
https://hiffnlqlhdupfvlyhjuv.supabase.co
```

The value of `group_invitation_webhook_secret` must exactly match `GROUP_INVITATION_WEBHOOK_SECRET`.

Create them with:

```sql
select vault.create_secret(
  'https://hiffnlqlhdupfvlyhjuv.supabase.co',
  'project_url'
);

select vault.create_secret(
  '<same webhook secret>',
  'group_invitation_webhook_secret'
);
```

For production, replace the project URL and use a new production webhook secret.

## 6. Database RPC migrations

The invitation flow is handled by these migrations:

```text
20260921223000_add_invite_group_member_by_email_rpc.sql
20260921224000_add_group_invitation_email_rpc.sql
20260921225000_wire_group_invitation_email_edge_function.sql
```

The final flow is:

```text
Muster app
→ invite_group_member_by_email()
→ create pending group_invitations row
→ send_group_invitation_email()
→ pg_net
→ send-group-invitation-email Edge Function
→ Resend
→ group-member-invitation template
→ email delivered
```

## 7. Production setup

The following settings are environment-specific and must be recreated manually in production:

```text
Resend API key
Resend verified sending domain
Resend template: group-member-invitation
Supabase Edge Function: send-group-invitation-email
Edge Function secret: RESEND_API_KEY
Edge Function secret: GROUP_INVITATION_WEBHOOK_SECRET
Verify JWT with legacy secret: OFF
Vault secret: project_url
Vault secret: group_invitation_webhook_secret
pg_net extension
```

The complete invitation email flow has been tested successfully end to end.
