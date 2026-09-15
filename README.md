# Muster

A personal football-team organizing app, built to replace Teamer. Create
events, invite players, keep an ordered standby queue, and see who is
actually turning up.

Private project, not a product.

## Docs

Read these before changing anything. They are the source of truth, and
they move often.

| File | What it covers |
|---|---|
| [docs/CONTEXT.md](docs/CONTEXT.md) | What it is, scope, roles, product rules |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Layers, DI, navigation, Kotlin style, testing |
| [docs/SCHEMA.md](docs/SCHEMA.md) | Tables, constraints, RLS, triggers, what the database enforces versus the app |
| [docs/SCREENS.md](docs/SCREENS.md) | Screen map, navigation, per-screen behaviour |
| [docs/DECISIONS.md](docs/DECISIONS.md) | Why closed choices were closed |
| [docs/design/DESIGN.md](docs/design/DESIGN.md) | Visual design of record, frames 1a–1t and 2a–2d |
| [docs/design/muster-screens-v5.html](docs/design/muster-screens-v5.html) | The frames themselves — open in a browser. v1–v4 are superseded, kept as history |

[CLAUDE.md](CLAUDE.md) at the root is the same map plus commands and
conventions, read automatically by Claude Code.

## Stack

Compose Multiplatform (Android, iOS, Web/wasm) over Supabase — Postgres,
Auth, RLS, Edge Functions. Koin for DI. Gmail SMTP for email. Sign-in is
email codes, no passwords. Push notifications are deferred past the MVP.

Package `app.muster`.

## Layout

```
shared/       shared module — Compose UI, models, data access
androidApp/   Android host
iosApp/       Xcode project, thin SwiftUI wrapper
webApp/       Web host
supabase/     migrations
buildSrc/     build-time Supabase config generation
docs/         see above
```

## Setup

Supabase credentials are injected at build time, never committed. Add to
`local.properties`:

```
supabase.url=https://<project>.supabase.co
supabase.publishableKey=<publishable key>
```

Or set `SUPABASE_URL` and `SUPABASE_PUBLISHABLE_KEY` as environment
variables, which take precedence. Without either, the build fails with a
message telling you which is missing.

The publishable key is safe to ship — RLS is what protects the data. The
service role key belongs nowhere near this repo.

## Running

- Android — `./gradlew :androidApp:assembleDebug`
- Web (wasm) — `./gradlew :webApp:wasmJsBrowserDevelopmentRun`
- Web (js) — `./gradlew :webApp:jsBrowserDevelopmentRun`
- iOS — open `iosApp/` in Xcode and run

## Tests

- Android — `./gradlew :shared:testAndroidHostTest`
- iOS — `./gradlew :shared:iosSimulatorArm64Test`
- Web — `./gradlew :shared:wasmJsTest` or `:shared:jsTest`

## Database

Migrations live in `supabase/migrations/` and are applied with the
Supabase CLI. Rules are enforced in the database — RLS policies,
constraints and triggers — never in the client. The app talks to Supabase
directly, so the client is not a trust boundary.

## Status

Backend built and tested, seven migrations. Auth done — Koin wired,
sign-in by email code, set-name gate. Home done — group cards, pending
invitations with accept/decline, empty and failed states, pull to
refresh. Settings done — editable name with save confirmation and a
discard prompt, read-only email, sign out. New group done, landing on a
Group placeholder.

Next: the two-kinds-of-error treatment, then Group.
