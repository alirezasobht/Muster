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
| `docs/design/DESIGN.md` | Visual design of record, frames 1a–1p |
| `docs/design/muster-screens-v1.html` | The frames — open in a browser |

Keep them updated when decisions change.

## Stack

Compose Multiplatform (Android + iOS + Web/wasm), Supabase (Postgres,
Auth, RLS, Edge Functions), Koin for DI, Gmail SMTP for email.
Package `app.muster`. Sign-in is email codes, no passwords. Push
notifications deferred past MVP.

## Commands

```
./gradlew :androidApp:assembleDebug            build Android
./gradlew :webApp:wasmJsBrowserDevelopmentRun  run web
./gradlew :shared:testAndroidHostTest          unit + ViewModel tests
./gradlew :shared:iosSimulatorArm64Test        iOS tests
```

iOS builds from Xcode: open `iosApp/`.

Migrations in `supabase/migrations/`, applied with the Supabase CLI.

## How to work

- **Step by step.** One step at a time, show the work, wait for a go
  before the next. Never batch a multi-step task into one pass.
- **Screens first**, then domain, then data, then wire up.
- Understand the architectural implications before writing code.
- Report differences rather than silently replacing files.
- Be concise. Don't over-explain. Ask rather than listing every option.

## Rules that are not preferences

- **Enforce rules in the database** — RLS, constraints, triggers. Never
  in the client. The app talks to Supabase directly, so the client is not
  a trust boundary and the publishable key ships in every build.
- **Don't re-implement database rules in Kotlin.** Capacity, standby
  promotion, the last-admin invariant and the event freeze all live in
  Postgres. A second copy drifts and the client cannot enforce it anyway.
- **Never commit secrets.** `local.properties` is gitignored and holds the
  Supabase URL and publishable key. The service role key belongs nowhere
  in this repo.

## Code style

- **No trailing comma** after the last argument in a call or parameter
  list. Android Studio's formatter adds them — the setting is off, keep
  it off.
- **Comments are the exception, not the default.** Only for an edge case,
  a constraint from outside the file, or something that silently breaks
  if changed. Never restate what the line does.
- Commit messages: max 10 words, as short as possible, no body.

## State

Backend built and tested, four migrations pushed. Auth done — Koin wired,
sign-in by email code, set-name gate, placeholder home. Next is Home:
group cards and pending invitations.
