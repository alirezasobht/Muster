# Muster

A football-team organizing app, built to replace Teamer. Create events,
invite players, keep an ordered standby queue, and see who is actually
turning up on Sunday.

Personal project, not a product. Android, iOS and web from one Kotlin
codebase.

## How it works

**Groups** are squads. You belong to as many as you like, and each has
its own members and events. Anyone can sign up, but creating a group is
allowlisted — a flag on your profile, set by hand. Everyone else joins by
invitation.

**Invitations** are by email address, whether or not that person has an
account yet. They get an email saying they've been invited; the
invitation itself lives in the app and waits for them to sign in. Nothing
is visible to them until they accept.

**Roles** are admin or member. The group's creator is its first admin,
admins can promote others, and a group can never be left without one.
Admins create events, invite and remove people, and can change anyone's
RSVP. Members can see everything and change only their own.

**Events** have a capacity. Admins invite up to that many players;
anyone beyond it joins an ordered standby queue. When someone drops out,
the player at the front of the queue is invited automatically — no admin
action, and it happens in the database, so it works even when nobody has
the app open. The queue can be reordered, which is how an admin decides
who gets the next free spot.

Once an event's start time passes it freezes: no RSVP changes, no
promotions, no new invites.

**Signing in** is a six-digit code emailed to you. No passwords, so
nothing to reset and no separate signup step — the first code creates the
account.

Groups are archived rather than deleted, and archiving hides and freezes
everything without losing it.

## Getting started

You need JDK 17+, Android Studio with the Kotlin Multiplatform plugin,
and the [Supabase CLI](https://supabase.com/docs/guides/cli). Xcode as
well, if you want to build for iOS.

**1. Set up a Supabase environment.** Project, schema, email, Vault,
Edge Function secrets and Auth — all in
[supabase/docs/environment_setup.md](supabase/docs/environment_setup.md).
Invitations won't work until every step there is done.

**2. Add your build values** to `local.properties` (gitignored):

```
SUPABASE_URL=https://<project>.supabase.co
SUPABASE_PUBLISHABLE_KEY=<publishable key>
WEB_APP_URL=<where the web app is served>
CONTACT_EMAIL=<support address; needed for web build only>
```

The same names as environment variables take precedence;
`. scripts/set-env-vars.sh <env>` exports them from that environment's
`Muster-env` folder. Without them, the build fails and tells you which is
missing.

The publishable key ships in every build and is meant to — RLS is what
protects the data. The service role key belongs nowhere near this repo.

**3. Allow yourself to create groups.** Sign in once so your profile
exists, then in the Supabase table editor set `can_create_groups` to true
on your row in `profiles`. Without it you can accept invitations but not
start a group, and the app will look empty.

## Running

| | |
|---|---|
| Android | `./gradlew :androidApp:assembleDebug`, or run from Android Studio |
| Web (wasm) | `./gradlew :webApp:wasmJsBrowserDevelopmentRun` |
| iOS | open `iosApp/` in Xcode and run |

Tests: `./gradlew :shared:testAndroidHostTest` covers the shared logic
and every ViewModel. `:shared:iosSimulatorArm64Test` and
`:shared:wasmJsTest` run the same common tests on those targets.
`:shared:connectedAndroidTest` needs a running emulator.

## Deploying

Backend changes go out with `supabase/scripts/deploy-backend.sh <env>`;
a full production deploy is `scripts/deploy-prod.sh`, then a manual upload
of the folder it prints. Every script, and how the Supabase CLI is
authorised: [supabase/docs/deploying.md](supabase/docs/deploying.md).

## Layout

```
shared/       Compose UI, domain, data — nearly all the code
androidApp/   Android host, MainActivity only
iosApp/       Xcode project, thin SwiftUI wrapper
webApp/       web host
supabase/     migrations, Edge Functions, deploy scripts and docs
scripts/      app build environment and the prod deploy
buildSrc/     build-time Supabase config generation
docs/         see below
```

Rules live in the database — RLS policies, constraints and triggers —
never in the client. The app talks to Supabase directly, so the client is
not a trust boundary: anything it can ask for, a hand-written request
could ask for too.

## Docs

| File | What it covers |
|---|---|
| [docs/CONTEXT.md](docs/CONTEXT.md) | Scope, roles, product rules |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Layers, DI, navigation, conventions, testing |
| [docs/SCHEMA.md](docs/SCHEMA.md) | Tables, RLS, triggers — what the database enforces |
| [docs/SCREENS.md](docs/SCREENS.md) | Screen map and per-screen behaviour |
| [docs/DECISIONS.md](docs/DECISIONS.md) | Why closed choices were closed |
| [docs/design/DESIGN.md](docs/design/DESIGN.md) | Visual design of record |
| [docs/design/muster-screens-v6.html](docs/design/muster-screens-v6.html) | The frames — open in a browser |
| [supabase/docs/environment_setup.md](supabase/docs/environment_setup.md) | Bringing up an environment |
| [supabase/docs/deploying.md](supabase/docs/deploying.md) | Deploy scripts and how they are authorised |

[CLAUDE.md](CLAUDE.md) at the root holds the same map plus conventions
and current state, read automatically by Claude Code.
