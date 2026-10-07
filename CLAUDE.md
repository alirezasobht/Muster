# CLAUDE.md

Muster — a personal football-team organizing app replacing Teamer.
Private use, not a product. Android, iOS and Web from one Kotlin codebase.

## Read before proposing anything

The docs are the source of truth, not this file and not your memory of an
earlier session. Read the ones your task needs, and **re-read before
editing** — they change often, and other sessions edit them too.

| File | What it covers |
|---|---|
| `docs/CONTEXT.md` | What it is, scope, roles, product rules |
| `docs/ARCHITECTURE.md` | Layers, DI, navigation, Kotlin style, testing |
| `docs/SCHEMA.md` | Tables, constraints, what the DB enforces vs the app |
| `docs/SCREENS.md` | Screen map, navigation, per-screen behaviour |
| `docs/DECISIONS.md` | Why closed choices were closed |
| `docs/design/DESIGN.md` | Visual design of record, frames 1a–1t and 2a–2d |
| `docs/design/muster-screens-v6.html` | The frames — open in a browser. v1–v5 superseded, kept as history |
| `supabase/docs/environment_setup.md` | Bringing up an environment: Vault, secrets, Resend, Auth |
| `supabase/docs/deploying.md` | Deploy scripts, one line each, and how the CLI is authorised |
| `docs/CI.md` | Branch flow, rulesets, PR checks, CI deploys, secrets, backups |

Keep them updated when decisions change.

## Stack

Compose Multiplatform (Android + iOS + Web/wasm), Supabase (Postgres,
Auth, RLS, Edge Functions), Koin for DI, Resend for email.
Package `app.muster`. Sign-in is email codes, no passwords. Push
notifications deferred past MVP.

## Commands

```
./gradlew :androidApp:assembleDebug            build Android
./gradlew :webApp:wasmJsBrowserDevelopmentRun  run web
./gradlew :shared:testAndroidHostTest          unit + ViewModel tests
./gradlew :shared:iosSimulatorArm64Test        iOS tests
./gradlew ktlintFormat                         auto-fix style
./gradlew ktlintCheck                          verify style
```

iOS builds from Xcode: open `iosApp/`.

Migrations in `supabase/migrations/`. Migrations, Edge Function secrets
and deploys go through `supabase/scripts/`, never by hand; a full prod
deploy is `scripts/deploy-prod.sh`. See `supabase/docs/deploying.md`.
Merging into `dev` deploys dev's backend from CI; PRs go into `dev`, from a
branch made with `--no-track`. See `docs/CI.md`.

## How to work

- **Step by step.** One step at a time, show the work, wait for a go
  before the next. Never batch a multi-step task into one pass.
- **Screens first**, then domain, then data, then wire up.
- Understand the architectural implications before writing code.
- Report differences rather than silently replacing files.
- **Run `./gradlew ktlintFormat` before a task is done**, then
  `ktlintCheck` must pass. Only formatting changes it makes to files you
  touched belong in the change.
- Be concise. Don't over-explain. Ask rather than listing every option.

## Rules that are not preferences

- **Enforce rules in the database** — RLS, constraints, triggers. Never
  in the client. The app talks to Supabase directly, so the client is not
  a trust boundary and the publishable key ships in every build.
- **Don't re-implement database rules in Kotlin.** Capacity, standby
  promotion, the last-admin invariant and the event freeze all live in
  Postgres. A second copy drifts and the client cannot enforce it anyway.
- **Never commit secrets.** `local.properties` is gitignored and holds the
  build values (Supabase URL and publishable key, web app URL, contact
  email). Everything else lives in the separate `Muster-env` repo. The
  service role key belongs nowhere in this repo.
- **Every new function revokes `EXECUTE` from `public` and `anon` in its
  own migration.** Postgres grants it to `PUBLIC` by default, so one that
  forgets is callable by anyone with the publishable key. SCHEMA.md →
  Grants.

## Code style

- **No trailing comma** after the last argument in a call or parameter
  list. Android Studio's formatter adds them — the setting is off, keep
  it off.
- **Comments are the exception, not the default.** Only for an edge case,
  a constraint from outside the file, or something that silently breaks
  if changed. Never restate what the line does.
- Commit messages: max 10 words, as short as possible, no body.

## State

Built: auth (email code, set-name gate), Home (1d/1e/1s/1t), Settings
(1f), New group (1g), Group shell with tabs (1h), Members tab (1i/1j),
app bar overflow — Leave group for everyone, Archive for admins (1j),
Add by email with the invitation email sent end to end, resending a group
or event invitation (both throttled to once per calendar day), Events
tab, New event, Event — roster with RSVP and per-row admin actions, all
of which confirm first, standby queue with drag-to-reorder, Add players,
account deletion (Settings and the web app's `/delete-account` page),
Privacy policy (embedded `privacy.html`).

**Dev** and **prod** both exist; the prod web app is live.

All writes go through definer RPCs, and every table's write grants are
revoked — SCHEMA.md → Grants.

Not built:
- **Edit group** — renaming a group. No screen or entry point, and
  `create_group`'s RPC has no rename counterpart.
- **Edit event** — title, time, location. Not capacity: it is set at
  creation and never editable, which is what keeps the capacity
  invariant to one entry point (SCHEMA.md rule 11).
- **The 2a–2d error treatment** — done on 1a, 1b, 1g and Add by email.
  Still outstanding on 1c and 1f, which carry a single `error` field and
  put everything in the field's slot, including errors that aren't about
  the field.

Next: Edit group and Edit event.
