# Runbook

Setting the repo up from a clone, every command worth knowing, and the
workflows that run the rest. What the app is: `README.md`. Why the
Supabase scripts are shaped the way they are:
`supabase/docs/deploying.md`. What runs on GitHub: `CI.md`. Release
order and versioning: `ARCHITECTURE.md` → "Versioning and releases".

All commands run from the repo root.

## First time

**Tools.** JDK 17+, Android Studio with the Kotlin Multiplatform plugin,
Xcode for iOS, the [Supabase CLI](https://supabase.com/docs/guides/cli),
Node for `npx netlify-cli`, and Docker or Podman for `backup-db.sh`.
Gradle itself needs no install — `./gradlew` fetches it.

**Sign in once.** `supabase login` stores an access token on the
machine; one login covers every project the account can reach, and each
script picks its target with `--project-ref`. Check it with `supabase
projects list`. For the Netlify deploy, `npx netlify-cli login`.

**Clone `Muster-env` beside this repo**, as `../Muster-env`. It holds
every secret and build value, per environment, in `<env>/env` and
`<env>/edge.env`; nothing runs without it. The scripts and
`set-env-vars.sh` read it directly, CI gets the same values through the
`Muster-env` sync, and a project's own Vault and Edge Function secrets
are set from it too — `supabase/docs/environment_setup.md`. Without the
repo, a local build can still fall back to `local.properties`:
`README.md` → Getting started.

**Make the scripts executable.** A fresh clone may not carry the bit,
and without it they fail with "permission denied":

```
chmod +x scripts/*.sh scripts/netlify/*.sh supabase/scripts/*.sh
```

`set-env-vars.sh` is sourced rather than run, so it doesn't need it.

## Pick the environment first

```
. scripts/set-env-vars.sh dev      # source it, don't run it
. scripts/set-env-vars.sh prod
```

Exports `SUPABASE_URL`, `SUPABASE_PUBLISHABLE_KEY`, `CONTACT_EMAIL`,
`WEB_APP_URL`, `MUSTER_ENV`, `ANDROID_CERT_SHA256`, `IOS_APP_STORE_ID`
and `IOS_TEAM_ID` into that shell only; the build prefers them over
`local.properties`. Run `./gradlew` from the same shell — Android
Studio's Run button won't see them. `MUSTER_ENV` decides the Android app
id suffix and label. Clear the app's data when switching, or the old
environment's session is sent to the new one.

## Style

Run before a task is done; `ktlintCheck` is a required PR check.

| Command | What it does |
|---|---|
| `./gradlew ktlintFormat` | Auto-fix, `:ktlint-rules` included |
| `./gradlew ktlintCheck` | Verify |
| `./gradlew :shared:ktlintFormat` | One module, faster |

## Android

| Command | What it does |
|---|---|
| `./gradlew :androidApp:assembleDebug` | Debug APK → `androidApp/build/outputs/apk/debug/` |
| `./gradlew :androidApp:installDebug` | Build and install on a device or emulator |
| `./gradlew :androidApp:uninstallDebug` | Remove it — when switching environment |
| `./gradlew :androidApp:bundleRelease` | Signed AAB → `androidApp/build/outputs/bundle/release/` |

## Web

| Command | What it does |
|---|---|
| `./gradlew :webApp:wasmJsBrowserDevelopmentRun --continuous` | Dev server, reloads on save |
| `./gradlew :webApp:jsBrowserDevelopmentRun` | JS target — the fallback path for browsers without Wasm GC |
| `./gradlew :webApp:clean :webApp:composeCompatibilityBrowserDistribution` | Production bundle → `webApp/build/dist/composeWebCompatibility/productionExecutable/` |
| `scripts/netlify/deploy-web-dev.sh` | Clean dev build, then deploys it to the dev Netlify site |

Build the production bundle from clean, or a stale development wasm can
end up in the output, over the host's per-file limit.

## iOS

Builds and runs from Xcode: open `iosApp/`. Gradle only to check the
framework links without it:

```
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
```

## Tests

| Command | Scope |
|---|---|
| `./gradlew :shared:testAndroidHostTest` | ViewModel and unit tests — the default |
| `./gradlew :shared:connectedAndroidDeviceTest` | Compose UI tests; needs a device or emulator |
| `./gradlew :shared:iosSimulatorArm64Test` | Common tests on the simulator |
| `./gradlew :shared:wasmJsTest`, `:shared:jsTest` | Common tests in headless Chrome |
| `./gradlew :shared:allTests` | Every target the host can run |

## Build plumbing

| Command | When |
|---|---|
| `./gradlew :shared:generateSupabaseConfig` | After changing an environment's build values |
| `./gradlew :shared:generateAppInfo` | After bumping `VERSION_NAME` |
| `./gradlew kotlinUpgradeYarnLock` | A web build fails on a `kotlin-js-store/yarn.lock` mismatch |
| `./gradlew clean` | Rarely needed |
| `./gradlew --no-configuration-cache`, `./gradlew --stop` | Ruling out the configuration cache or the daemon |

## Supabase

`<env>` is `dev` or `prod`; prod asks for `prod` to be typed first. Dev's
backend deploys from CI on a merge into `dev`, so these are for prod and
for deploying dev by hand. `deploying.md`.

| Command | What it does |
|---|---|
| `supabase/scripts/push-migrations.sh <env>` | `db push`; relinks dev on the way out |
| `supabase/scripts/push-secrets.sh <env>` | Edge Function secrets from `Muster-env/<env>/edge.env`. Prod only — dev's come from the `Muster-env` sync |
| `supabase/scripts/deploy-functions.sh <env>` | Deploys every Edge Function |
| `supabase/scripts/deploy-backend.sh <env>` | The three above in order, stopping at the first failure |
| `supabase/scripts/backup-db.sh <env>` | Roles, schema and data → `Muster-env/<env>/backups/<timestamp>/`; needs Docker or Podman |
| `supabase projects list` | Check the CLI is signed in and sees both projects |

## GitHub Actions

From the repo's Actions tab, or `gh workflow run <file>`. Branch and PR
rules: `CI.md`.

| Workflow | Runs | What it does |
|---|---|---|
| `pr-checks.yml` | PR into `dev` or `main` | ktlint, unit, Android, web and iOS checks |
| `deploy-dev.yml` | push to `dev` touching `supabase/` | Backs dev up, then migrations and functions |
| `release-prepare.yml` | by hand | Bumps `VERSION_NAME` if the branch hasn't, opens the release PR into `main` |
| `release-checks.yml` | PR into `main` | Fails unless `VERSION_NAME` is above main's |
| `release-guard.yml` | PR into `main` | Closes one opened by anyone but the maintainer |
| `deploy-prod.yml` | by hand, after approval | Per ticked box: backend (backup, migrations, secrets, functions), web to Pages, Android to Play internal, iOS to TestFlight |
| `release-finish.yml` | by hand, after the deploy | Tags `vX.Y.Z` and back-merges `main` into `dev` |

A release is Release prepare → merge the PR → Deploy prod → Release
finish, with the CHANGELOG entry on `dev` before any of it.

## Releasing by hand

```
scripts/deploy-prod.sh
```

The fallback when CI can't do it: prod backend, then clean web and
Android release builds with prod values. Ends on dev whatever happens.
The Pages and Play uploads stay manual; it prints both paths.
