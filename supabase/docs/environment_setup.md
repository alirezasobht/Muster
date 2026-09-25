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

Under **Security**, tick all three:

- **Enable Data API** — the app reaches Postgres only through it.
- **Automatically expose new tables** — the migrations never grant
  `SELECT`; reads rely on the default grants this creates.
- **Enable automatic RLS** — creates `public.rls_auto_enable()`, which
  `20260921220000_restrict_public_function_execute.sql` revokes. Without
  it, `db push` fails there.

Auto-expose is being retired. From 2026-10-30 Supabase enforces it off
on every project: tables that exist keep their grants, but any table or
function created afterwards needs explicit grants in its migration, or
the Data API refuses it.

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
- Email templates from `supabase/auth.template/`, pasted into the slots
  and with the subjects listed in its `README.md`. They use `{{ .Token }}`,
  not a confirmation link — sign-in is code-only.
- Site URL and redirect URLs for this environment.
- Session time-boxing and inactivity timeout off.

## 7. Check

Point the app build at this environment. Source the script, don't run
it, or the exports never reach your shell:

```
. scripts/set-env-vars.sh <env>
./gradlew :androidApp:installDebug
```

It exports `SUPABASE_URL` and `SUPABASE_PUBLISHABLE_KEY` from `env`, and
`CONTACT_EMAIL` and `WEB_APP_URL` from `edge.env`, which the build
prefers over `local.properties`. Build from that same
shell; Android Studio's Run button won't see them. Clear the app's data
when switching, or the old environment's session is sent to the new one.
A new shell goes back to `local.properties`.

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

## 8. Web app (prod only)

The web app is the prod site, `https://www.musterapp.fyi`, and the
value of `WEB_APP_URL` and the Auth Site URL. It's a Cloudflare
**Pages** project, `muster-prod`, on direct upload. Not a Worker: a
Worker's custom domain needs Cloudflare to run DNS, and DNS stays at
Porkbun.

Build from clean, or a stale development wasm (over 30 MB) can end up
in the output — over Pages' 25 MiB per-file limit:

```
. scripts/set-env-vars.sh prod
./gradlew :webApp:clean :webApp:wasmJsBrowserDistribution
```

Upload `webApp/build/dist/wasmJs/productionExecutable/` in the
project's **Create deployment**. `composeResources` must be included.

`delete-account.html` rides along and is served at `/delete-account`,
the account deletion link for Play. The build fills its contact address
and web link from `CONTACT_EMAIL` and `WEB_APP_URL`, and fails without
them.

Domain, set once:

- Pages → Custom domains: `www.musterapp.fyi`.
- Porkbun DNS: `CNAME www → muster-prod.pages.dev`.
- Porkbun URL forwarding: root → `https://www.musterapp.fyi`, 301, path
  included, no wildcard. A wildcard would also catch `send`.

Leave the MX and TXT records alone: they carry `support@musterapp.fyi`
forwarding and Resend's SPF and DKIM.
