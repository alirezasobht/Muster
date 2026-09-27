# Deploying

How the deploy scripts work and what lets them in. Setting up a new
environment from scratch: `environment_setup.md`.

## Authorisation

Every `supabase` command runs as the **Supabase account signed in to the
CLI**, using the access token that `supabase login` stores on the machine
(in the OS credential store, such as the macOS Keychain, or in
`~/.supabase/access-token`; never in the repo). One login covers every
project that account can reach, so moving between environments needs no
new login; `--project-ref` only picks the target.

- **No database password.** `db push` signs in with a temporary login role
  the CLI creates through the same token.
- **The project ref is not a secret.** It is part of every app build's URL,
  and without a signed-in account it opens nothing.
- **Check it works:** `supabase projects list` lists every project in the
  signed-in account, which should include each environment's project.
- **CI:** store an access token as a `SUPABASE_ACCESS_TOKEN` secret; it
  replaces `supabase login`.

## Scripts

Run from the repo root with `<env>`, for example `dev` or `prod`. The
accepted names are listed in `_env.sh`; each needs its own
`Muster-env/<env>/` folder. For prod, each script asks for `prod` to be
typed first.

**`supabase/scripts/`**

- `_env.sh` — shared setup, sourced by the others: maps `<env>` to its
  `Muster-env` folder and project ref, and asks for the prod confirmation.
- `push-migrations.sh <env>` — applies pending migrations (`db push`). Links
  the CLI to that project for the push and always relinks dev afterwards.
- `push-secrets.sh <env>` — uploads `Muster-env/<env>/edge.env` as Edge
  Function secrets, prefix stripped, and removes stale ones.
- `deploy-functions.sh <env>` — deploys every Edge Function, settings from
  `supabase/config.toml`.
- `deploy-backend.sh <env>` — runs the three above in order: migrations,
  secrets, functions. Confirms prod once and stops at the first failure.

**`scripts/`**

- `set-env-vars.sh <env>` — source it (`. scripts/set-env-vars.sh <env>`):
  exports the app build values for that environment into the current shell
  only.
- `deploy-prod.sh` — full prod deploy: `deploy-backend.sh prod`, then a clean
  web build with prod values. Ends on dev whatever happens. The Pages
  upload stays manual; it prints the folder.

## Staying on dev

Dev is the default the scripts return to. Only one thing outlives a
script: the CLI link, saved in `supabase/.temp` and followed by every
terminal. `push-migrations.sh` and `deploy-prod.sh` always put it back on
dev. Build values from `set-env-vars.sh` die with their shell.
