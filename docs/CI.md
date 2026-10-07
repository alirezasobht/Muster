# CI

How changes reach `dev` and `main`, what runs on GitHub, and where its
values come from. The local deploy scripts: `supabase/docs/deploying.md`.
Why it is set up this way: DECISIONS.md.

## Branches

- All work goes through a PR into `dev`. Branch from `origin/dev` with
  `--no-track`; a branch that tracks `origin/dev` pushes to `dev` and the
  ruleset rejects it.
- **feature → `dev`:** squash or rebase. `dev` history is linear.
- **`dev` → `main`:** merge commit, then tag the release. `main`'s merge
  commits never come back to `dev`; the next release PR only carries what
  `dev` gained since the last one.
- **Hotfix:** branch from `main`, PR into `main`. Bring it back to `dev`
  on a branch from `origin/dev` with
  `git cherry-pick --no-merges origin/dev..origin/main`, then a PR into
  `dev`. A PR straight from `main` would carry the release merge commits,
  which `dev` rejects.

## Repository settings

Merge commits, squash and rebase are all enabled at repo level; each
branch's ruleset narrows them. "Automatically delete head branches" is
off.

## Ruleset: `dev`

- Restrict deletions, block force pushes
- Require linear history
- Require a pull request: 0 approvals, squash or rebase only
- Require status checks: `Changes`, `Ktlint Check`, `Unit Tests`,
  `Android Tests`, `Web Build`, `iOS Tests`; branch must be up to date

No bypass list. The `main` ruleset is not created yet (see Not done).

## PR checks — `ci.yml`

Runs on every PR into `dev` or `main`.

| Check | Runs | What it does |
|---|---|---|
| `Changes` | always | Decides whether the PR touches code |
| `Ktlint Check` | always | `ktlintCheck` |
| `Unit Tests` | code changed | `testAndroidHostTest` and `assembleDebug` |
| `Android Tests` | code changed | `connectedAndroidDeviceTest` on an API 34 emulator |
| `Web Build` | code changed | Compatibility web build against dev, kept as an artifact |
| `iOS Tests` | code changed | `iosSimulatorArm64Test` on macOS |

"Code" is `shared/`, `androidApp/`, `webApp/`, `iosApp/`, `buildSrc/`,
`ktlint-rules/`, `gradle/`, the Gradle build files, `gradle.properties`,
`version.properties` and `ci.yml` itself. A docs-only or `supabase/`-only
PR runs `Changes` and `Ktlint Check`; the rest show as skipped, which
counts as passing.

The skip is per job, not a `paths:` filter on the workflow: a workflow
that never starts never reports its checks, and a required check that
never reports blocks the PR for good. `Changes` is required itself, or a
failure there would skip every test and still pass.

Every job but `Web Build` builds with dummy values (`ci.invalid`).
`Web Build` uses the dev build values and uploads the result as
`web-dev-pr<N>`, kept 14 days: PR → Checks → CI → Summary.

## Dev deploy — `deploy-dev.yml`

Runs on a push to `dev` that touches `supabase/` or the workflow itself,
and can be run by hand once it is on the default branch. Uses the `dev`
environment.

1. Dump roles, schema and data; stop if any file is empty.
2. Encrypt the dump with `age` and commit it to `Muster-backups`.
3. `db push` the migrations.
4. Deploy every Edge Function, settings from `supabase/config.toml`.

A failed step stops the rest, so nothing reaches dev without a backup.
Edge Function secrets are not pushed here; the `Muster-env` sync does it.

## Where the values live

| Name | In Muster | Comes from |
|---|---|---|
| `DEV_SUPABASE_URL`, `DEV_SUPABASE_PUBLISHABLE_KEY` | repository secret | sync, `dev/env` |
| `DEV_WEB_APP_URL`, `DEV_CONTACT_EMAIL` | repository secret | sync, `dev/edge.env` |
| `DEV_SUPABASE_PROJECT_REF`, `DEV_SUPABASE_ACCESS_TOKEN` | `dev` environment | sync, `dev/env` |
| `MUSTER_BACKUPS_TOKEN` | `dev` environment | by hand |
| `AGE_PUBLIC_KEY` | `dev` environment, variable | by hand |
| `PROD_*` (same six) | `prod` environment | sync, `prod/env` and `prod/edge.env` |

Build values are repository secrets because PR jobs need the dev ones.
Deploy values sit in environments, which release them only to their own
branch: `dev` to `dev`, `prod` to `main`. Never set a synced value by
hand in Muster; the next sync overwrites it.

## `Muster-env` sync

`Muster-env/.github/workflows/sync-ci-secrets.yml` runs on a push to
`Muster-env`'s `main` that changes an `env` or `edge.env` file, or by
hand. It:

- copies the values above into Muster, through `MUSTER_SECRETS_TOKEN`;
- uploads `dev/edge.env` to the dev project as Edge Function secrets,
  with the same checks and stale-secret cleanup as `push-secrets.sh`.

Prod Edge Function secrets stay manual: `supabase/scripts/push-secrets.sh
prod`. Values never print; they go to `gh secret set` through stdin.

## Backups

`Muster-backups` is a private repo. Each dev deploy adds
`dev/<UTC timestamp>-<commit>.tar.gz.age`: roles, schema and data, tarred
and encrypted with `age`. CI holds only the public key; the private key is
`backup.key` at the root of `Muster-env`, with a copy in the password
manager. Without it no backup can be opened.

To read one, from `Muster-env`, into a folder outside every repo — the
dump holds users' emails:

```
git -C ../Muster-backups pull
mkdir -p ~/Muster-restore
age -d -i backup.key "$(ls -t ../Muster-backups/dev/*.age | head -n 1)" \
  | tar -xzf - -C ~/Muster-restore
```

Delete `~/Muster-restore` afterwards.

## Tokens

| Token | Scope | Stored in |
|---|---|---|
| `MUSTER_SECRETS_TOKEN` | fine-grained, Muster only: Secrets and Environments read/write | `Muster-env` repository secret |
| `MUSTER_BACKUPS_TOKEN` | fine-grained, `Muster-backups` only: Contents read/write | Muster `dev` environment |
| Supabase "GitHub Actions" | the whole Supabase account | `Muster-env` `dev/env` and `prod/env` |

The two GitHub tokens expire after a year. Replace them in the same place;
for the Supabase one, update both `env` files and push.

## Not done

- **`main`:** ruleset (PR only, merge commit only, checks plus a
  `Version Bump` check that `VERSION_CODE` went up; no "up to date", since
  `dev` never holds `main`'s merge commits), and a prod deploy on push to
  `main` with approval: backup, migrations, Edge secrets, functions,
  Cloudflare Pages, Play, App Store.
- **Netlify** dev web deploy. The `Web Build` artifact also lacks
  `.well-known/`: `upload-artifact` skips hidden files unless
  `include-hidden-files: true`.
