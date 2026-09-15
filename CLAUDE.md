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
| `docs/design/muster-screens-v5.html` | The frames — open in a browser. v1–v4 superseded, kept as history |

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

Backend built and tested. Auth done — Koin wired, sign-in by email code,
set-name gate. Home done (1d/1e/1s/1t): group cards, pending invitations
with accept/decline, first-load spinner, empty and failed states, pull to
refresh. Settings done (1f): editable name with save, save confirmation
and a discard prompt on unsaved edits, read-only email, sign out,
loading/failed states.

A fifth migration, `get_my_pending_invitations()`, is on disk
(`supabase/migrations/20260914090000_*.sql`) — confirm it's been pushed
before relying on invitation data loading.

`DataChanges` (`domain/event/`) is a broadcast bus for writes other
screens depend on. Repositories notify, ViewModels subscribe. Only
`DataChange.MyGroups` exists so far — `GroupRepositoryImpl` fires it on
create and accept, and Home refreshes with its indicator. Add cases when
a screen needs one, not before. It sits alongside refresh-on-resume,
which stays the safety net.

New group (1g) done: screen, `CreateGroupUseCase`, insert wired through
`GroupRepository`, navigates to a `Group` placeholder on success. Needed
two more migrations to actually work — `groups_select` must let a creator
see their own row before `on_group_created` (an `after insert` trigger)
has written their membership, or every create rolls back with the same
error `can_create_groups() = false` produces. Migration 6
(`20260915100000_*.sql`) tried a `group_is_live(id)` call, which doesn't
work: that helper is `stable` and can't see the row its own statement is
still inserting. Migration 7 (`20260915110000_*.sql`) corrects it with a
plain `archived_at is null` column check, and is what's actually on the
remote database now (applied by hand, then written up as a migration —
see `docs/SCHEMA.md` rule 1). Both are pushed.

Every screen currently renders every error in the field's error slot,
including ones that aren't about the field — "Can't connect", "You're
not allowed to create groups". The two-kinds-of-error treatment
(DESIGN.md → "Two kinds of error", frames 2a–2d) fixes this and is
still outstanding across 1a, 1b, 1c, 1f and 1g.

Group shell done (1h/1i/1j's app bar and tabs, no designed 1s/1t of its
own yet): `GroupScreen`/`GroupUiState`/`GroupViewModel`, back + group
name + admin-only overflow (⋮, stubbed — Archive and Leave group come
with Members), Events/Members tabs with naming placeholder bodies.
Loading and failed states reuse Home's shapes; the app bar can only
show the back button there, since the group name and role — what the
rest of the bar needs — are exactly what's loading. Added
`GroupRepository.getGroup`/`getMyRole`, `GetGroupUseCase`,
`GetMyGroupRoleUseCase`, `GroupRole`. Pulled Home's dashed-icon message
state out into `ui/common/components/MessageState.kt` since Group now
needs the same shape — Event's load failure will too. Replaces
`GroupPlaceholderRoute` in `NavGraph`.

Next: the members list (1i/1j), add by email, promote/demote/remove,
leave group. Then events. The overflow's Archive/Leave and the 2a–2d
error treatment above are both still open.
