# CI

How changes reach `dev` and `main`, what runs on GitHub, and where its
values come from. The local deploy scripts: `supabase/docs/deploying.md`.
Why it is set up this way: DECISIONS.md.

## Branches

- All work goes through a PR into `dev`. Branch from `origin/dev` with
  `--no-track`; a branch that tracks `origin/dev` pushes to `dev` and the
  ruleset rejects it.
- **feature → `dev`:** squash or rebase. `dev` takes no merge commits
  except back-merges.
- **release → `main`:** a merge commit. The release PR comes from `dev`, or
  from a fix branch for a hotfix (see Releasing).
- **`main` → `dev`:** after every release, a back-merge with a merge
  commit, so `dev` always contains `main` and a hotfix never needs a
  cherry-pick.

## Repository settings

Merge commits, squash and rebase are all enabled at repo level; each
branch's ruleset narrows them. "Automatically delete head branches" is
off. Fork PRs need approval before their workflows run (Settings →
Actions → "Require approval for all external contributors").

Only the owner has write access, so nobody else can push to any branch or
merge. Outsiders' PRs into `main` are closed by `release-guard.yml`.

## Rulesets

**`dev`**
- Restrict deletions, block force pushes, require linear history
- Pull request: 0 approvals, squash or rebase only
- Status checks: `Changes`, `Ktlint Check`, `Unit Tests`,
  `Android Tests`, `Web Build`, `iOS Tests`; branch must be up to date
- Bypass: Repository admin, **pull requests only**, for back-merges;
  the release app, **always**, for the version bump

**`main`**
- Restrict deletions, block force pushes
- Pull request: 0 approvals, merge commit only
- Status checks: the six above plus `Version Bump`; branch must be up
  to date (back-merges keep `dev` containing `main`)
- No bypass

## Workflows

| File | Runs | Does |
|---|---|---|
| `pr-checks.yml` | every PR | lint, tests, builds |
| `release-checks.yml` | PRs into `main` | `Version Bump` |
| `release-guard.yml` | PRs into `main` | closes PRs not from the owner or the release app |
| `release-prepare.yml` | by hand | bumps the version if needed, opens the release PR |
| `release-finish.yml` | by hand | tags the release, back-merges `main` into `dev` |
| `deploy-dev.yml` | push to `dev` touching `supabase/` | backs up dev, deploys its backend |
| `deploy-prod.yml` | push to `main` | backs up prod, deploys backend, web, Android, iOS |

`workflow_dispatch` buttons and `pull_request_target` workflows run from
`main`'s copy, so a change to them takes effect after the next release.

### PR checks — `pr-checks.yml`

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
`version.properties` and `pr-checks.yml` itself. A docs-only or
`supabase/`-only PR runs `Changes` and `Ktlint Check`; the rest show as
skipped, which counts as passing. `Changes` is required itself, or a
failure there would skip every test and still pass.

Every job but `Web Build` builds with dummy values (`ci.invalid`).
`Web Build` uses the dev build values and uploads the result as
`web-dev-pr<N>`, kept 14 days: PR → Checks → Summary.

### `Version Bump` — `release-checks.yml`

Fails unless `VERSION_NAME` in `version.properties` is `x.y.z` and above
`main`'s.

## Releasing

1. **Actions → Release prepare** → base (default `dev`; a fix branch for
   a hotfix), version (only if the branch isn't bumped yet). If the
   branch's `VERSION_NAME` isn't above `main`'s, it commits
   "Bump version to x.y.z" on the branch as the release app, then opens
   **Release x.y.z** into `main`.
2. Wait for the checks, then **Create a merge commit**.
3. **Actions → Deploy prod** → approve each job (see below).
4. **Actions → Release finish.** Tags `main` `vX.Y.Z`, then opens
   **Back-merge vX.Y.Z** from a `back-merge/vX.Y.Z` branch into `dev` and
   merges it with a merge commit once the checks pass. On conflicts it
   comments on the PR and fails: resolve them on that branch, then run it
   again.

Release prepare and Release finish act as the GitHub App
`muster-release-alirezasobht`: Contents, Pull requests read/write; Checks
and Commit statuses read. Its token is minted per run from the app's key;
PRs and pushes it makes trigger CI as normal.

Doing it by hand instead: bump `VERSION_NAME` in a PR into `dev`, open the
release PR yourself, tag after the merge, and back-merge with a PR
`main` → `dev` merged by `gh pr merge <n> --merge --admin`.

## Dev deploy — `deploy-dev.yml`

On a push to `dev` that touches `supabase/` or the workflow, or by hand.
Uses the `dev` environment.

1. Dump roles, schema and data; stop if any file is empty.
2. Encrypt with `age`, commit to `Muster-backups/dev/`.
3. `db push` the migrations.
4. Deploy every Edge Function.

Dev's Edge Function secrets aren't pushed here; the `Muster-env` sync
uploads them.

## Prod deploy — `deploy-prod.yml`

On every push to `main`. Uses the `prod` environment, which needs your
approval per job; secrets are released only after it.

- **Backend:** backup to `Muster-backups/prod/`, migrations, Edge
  Function secrets from the `PROD_EDGE_*` secrets (stale ones removed),
  functions. The order matches `deploy-backend.sh`.
- After Backend succeeds, each with its own approval:
  - **Web:** prod web build, uploaded to Cloudflare Pages as a
    production deployment (`--branch=main`).
  - **Android:** `bundleRelease` signed with the upload key, uploaded to
    Play's **internal** track. Promote to production in Play Console.
  - **iOS:** Release archive signed with the Apple Distribution
    certificate and the **Muster App Store** profile, uploaded to
    **TestFlight**. Submit for review in App Store Connect.

The iOS job recreates `../Muster-env/prod/ios.xcconfig` from a secret,
since `Config.xcconfig` includes it. The Kotlin build phase skips
`set-env-vars.sh` when `CI` is set and takes the values from the job.

`scripts/deploy-prod.sh` still works as a manual fallback.

## Environments

| Environment | Branches | Approval | Holds |
|---|---|---|---|
| `dev` | `dev` | no | dev deploy values, backups token |
| `prod` | `main` | you, per job | everything prod |
| `release` | `dev`, `main` | no | the release app's key |

Build values PR jobs need are repository secrets; everything else lives
in an environment, so PR jobs never see it.

## Where the values live

All synced from `Muster-env` unless marked by hand. Never set a synced
value by hand in Muster: the next sync overwrites it.

**Repository secrets:** `DEV_SUPABASE_URL`, `DEV_SUPABASE_PUBLISHABLE_KEY`,
`DEV_WEB_APP_URL`, `DEV_CONTACT_EMAIL`.

**`dev`:** `DEV_SUPABASE_PROJECT_REF`, `DEV_SUPABASE_ACCESS_TOKEN`;
`MUSTER_BACKUPS_TOKEN` and variable `AGE_PUBLIC_KEY` by hand.

**`prod`:**
- Build: `PROD_SUPABASE_URL`, `PROD_SUPABASE_PUBLISHABLE_KEY`,
  `PROD_WEB_APP_URL`, `PROD_CONTACT_EMAIL`, `PROD_ANDROID_CERT_SHA256`,
  `PROD_IOS_APP_STORE_ID`, `PROD_IOS_TEAM_ID` (from `prod/ios.xcconfig`)
- Backend: `PROD_SUPABASE_PROJECT_REF`, `PROD_SUPABASE_ACCESS_TOKEN`,
  `PROD_EDGE_*` (one per `prod/edge.env` line)
- Web: `PROD_CLOUDFLARE_API_TOKEN`, `PROD_CLOUDFLARE_ACCOUNT_ID`,
  `PROD_CLOUDFLARE_PAGES_PROJECT`
- Android: `PROD_UPLOAD_KEYSTORE_BASE64`, `PROD_UPLOAD_KEYSTORE_PASSWORD`,
  `PROD_PLAY_SERVICE_ACCOUNT_JSON`
- iOS: `PROD_IOS_P12_BASE64`, `PROD_IOS_P12_PASSWORD`,
  `PROD_IOS_PROFILE_BASE64`, `PROD_IOS_XCCONFIG`, `PROD_ASC_KEY`,
  `PROD_ASC_KEY_ID`, `PROD_ASC_ISSUER_ID`
- By hand: `MUSTER_BACKUPS_TOKEN`, variable `AGE_PUBLIC_KEY`

**`release`:** `MUSTER_RELEASE_APP_KEY` (synced from `release-app.pem`);
variable `MUSTER_RELEASE_APP_ID` by hand.

## `Muster-env` sync

`Muster-env/.github/workflows/sync-ci-secrets.yml` runs on a push to
`Muster-env`'s `main` that changes one of its files, or by hand. It:

- copies the values above into Muster, through `MUSTER_SECRETS_TOKEN`;
- uploads `dev/edge.env` to the dev project as Edge Function secrets,
  with the checks and stale-secret cleanup of `push-secrets.sh`;
- copies `prod/edge.env` into the `prod` environment as `PROD_EDGE_*`,
  deleting names no longer in the file, for the prod deploy to upload
  with the functions.

Values go to `gh secret set` through stdin and never print. Binary files
(keystore, `.p12`, profile) go base64-encoded.

## Backups

`Muster-backups` is a private repo. Each deploy adds
`<env>/<UTC timestamp>-<commit>.tar.gz.age`: roles, schema and data, tarred
and encrypted with `age`. CI holds only the public key; the private key is
`backup.key` at the root of `Muster-env`, with a copy in the password
manager. Without it no backup can be opened.

To read one, from `Muster-env`, into a folder outside every repo — the
dump holds users' emails:

```
git -C ../Muster-backups pull
mkdir -p ~/Muster-restore
age -d -i backup.key "$(ls -t ../Muster-backups/prod/*.age | head -n 1)" \
  | tar -xzf - -C ~/Muster-restore
```

Delete `~/Muster-restore` afterwards.

## Credentials

| Credential | Scope | Stored in | Renew |
|---|---|---|---|
| `MUSTER_SECRETS_TOKEN` | fine-grained, Muster: Secrets, Environments write | `Muster-env` secret | yearly |
| `MUSTER_BACKUPS_TOKEN` | fine-grained, `Muster-backups`: Contents write | Muster `dev`, `prod` | yearly |
| Release app key | the app on Muster only | `Muster-env/release-app.pem` | when rotated |
| Supabase "GitHub Actions" token | whole Supabase account | `Muster-env` `*/env` | on expiry |
| Cloudflare API token | Pages edit | `Muster-env/prod/env` | on expiry |
| Play service account key | `play-upload`, Muster on Play | `Muster-env/prod/play-service-account.json` | when rotated |
| Upload keystore | Play upload key | `Muster-env/prod/upload-keystore.jks` | — |
| Apple Distribution certificate | team signing | `Muster-env/prod/ios-distribution.p12` | yearly |
| Muster App Store profile | `app.muster.prod` | `Muster-env/prod/ios-app-store.mobileprovision` | yearly, with the certificate |
| App Store Connect API key | App Manager | `Muster-env/prod/app-store-connect-key.p8` | when rotated |

Replace a file or value in `Muster-env` and push; the sync does the rest.

The Play key exists because the Google Cloud organisation's
`iam.managed.disableServiceAccountKeyCreation` policy is overridden
(enforcement off) on project `muster-ci` only.

## Not done

- **Netlify** dev web deploy. The `Web Build` artifact also lacks
  `.well-known/`: `upload-artifact` skips hidden files unless
  `include-hidden-files: true`.
