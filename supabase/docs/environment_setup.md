# Environment setup

How to bring up a Muster environment. Values are never written here —
they live in `Muster-env/<env>/`, with every name prefixed `DEV_` or
`PROD_`. The examples at the repo root list the names.

| File in `Muster-env/<env>/` | Holds | Example |
|---|---|---|
| `env` | app build values, project ref, DB password | `.env.example` |
| `edge.env` | Edge Function secrets only | `edge.env.example` |

## 1. Supabase project

Create the project in `ap-southeast-2`. Record its ref, URL, publishable
key and database password in `env`.

## 2. Database

```
supabase link --project-ref <ref>
supabase db push
```

Applies every migration, including `pg_net`. Nothing in the schema is
set by hand.

## 3. Vault

Three entries, read by the database when it queues an invite email:

| Name | Value |
|---|---|
| `project_url` | this environment's project URL |
| `group_invitation_webhook_secret` | a new random value, identical to `GROUP_INVITATION_WEBHOOK_SECRET` in `edge.env` |
| `event_invitation_webhook_secret` | a new random value, identical to `EVENT_INVITATION_WEBHOOK_SECRET` in `edge.env` |

**Without `project_url` and the group secret, inviting anyone to a
group fails** — the invite and its email share a transaction. Event
invites never fail on configuration: promotion runs inside a member's
own RSVP change, so a missing entry skips the email with a warning in
the database log instead. Set all three before the environment is used.

```sql
select vault.create_secret('<value>', 'project_url');
select vault.create_secret('<value>', 'group_invitation_webhook_secret');
select vault.create_secret('<value>', 'event_invitation_webhook_secret');
```

## 4. Resend

- Sending domain verified, with SPF and DKIM.
- An API key for this environment.
- Both templates published from `supabase/resend.template/`, with the
  subjects and variables listed in its `README.md`.

Record the key, sender and both template IDs in `edge.env`.

## 5. Edge Function secrets and deploy

```
supabase/scripts/push-secrets.sh <env>
supabase/scripts/deploy-functions.sh <env>
```

The first refuses any line in `edge.env` without the right prefix, checks
every required secret is present, uploads, then removes stale ones. The
second deploys every function. Both take the project ref from `env`, so
they can't target the wrong project. `prod` asks for confirmation.

Push secrets first: a function refuses to start if any required one is
missing. `ANDROID_APP_URL` is optional and falls back to the web URL.

Confirm `verify_jwt = false` took effect on
`send-group-invitation-email` and `send-event-invitation-email` —
`config.toml` sets it, and the functions authenticate with their
webhook secrets instead.

## 6. Auth

Supabase Auth sends sign-in codes itself, separately from the invite
function. Configure it in the dashboard:

- Custom SMTP pointing at Resend, with this environment's sender.
- Email templates using `{{ .Token }}`, not a confirmation link — sign-in
  is code-only.
- Site URL and redirect URLs for this environment.
- Session time-boxing and inactivity timeout off.

## 7. Check

Sign in with a code, then invite an address you control to a group and
to an event, and confirm both emails arrive. If one doesn't:

```sql
select status_code, content, error_msg, created
from net._http_response
order by created desc
limit 5;
```

- `401` — Vault secret and Edge secret differ.
- `500` with `WORKER_ERROR` — the function crashed on start; check its
  **Logs** tab, not Invocations, for the missing secret.
- No status, `error_msg` set — check Vault's `project_url`.
- No row — nothing was queued; check the invitation was created.
