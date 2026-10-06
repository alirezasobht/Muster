# Muster — Decisions

Closed decisions and why. The conclusions live where they apply, in
CONTEXT.md, ARCHITECTURE.md and SCHEMA.md; this file keeps the reasons so
they aren't relitigated. Reopening one is fine, after reading why it was
closed.

## Native over PWA

Push notifications are the core feature, and a missed one means a missing
player. iOS web push is unreliable: silent unsubscriptions, listeners
that stop after a restart, and push only after adding the site to the
home screen. Android PWA push is fine, but the weakest platform sets the
bar.

Rejected: a PWA, despite saving the Apple Developer fee and the app stores.

## Compose Multiplatform over Flutter

Existing Kotlin and Compose skills transfer directly, so a working app
comes sooner despite the thinner ecosystem.

Rejected: Flutter. More mature, but it means learning Dart, a new widget
system and new state management, which doesn't pay off at this size.

## Supabase over Firebase

The data is relational: standby queues need ordered SQL. Supabase also has
an official Kotlin Multiplatform SDK.

Rejected: Firebase. Firestore would mean maintaining queue positions by
hand, and KMP support relies on a community wrapper. FCM remains the push
path either way.

## Supabase over a self-hosted Ktor backend

Supabase enforces visibility with RLS in the database; with Ktor every
check is endpoint code that must not be forgotten. It also saves writing
auth, running Postgres, deployment and hosting.

Rejected: Ktor, despite shared models. Revisit only if standby logic
outgrows an Edge Function.

## Config via a generated Kotlin file, not a plugin

A Gradle task reads build values (Supabase URL, publishable key, web app
URL) from the environment, falling back to `local.properties`, and
generates `app.muster.SupabaseConfig` into `commonMain`. Any future
build-time value follows the same path. The web app's static files are
the one exception: plain HTML and JSON can't read Kotlin, so the web
build replaces their `@…@` placeholders by filtering the files.

Rejected: `BuildConfig` (Android-only), `expect`/`actual` (a copy per
target) and BuildKonfig (a third-party plugin whose Wasm support is one
more thing to verify).

The publishable key is not a secret: it ships in every build, and RLS is
the trust boundary. Keeping it out of git is rotation convenience, not
security. The secret key never reaches the client.

## Email codes, no passwords

Sign-up and sign-in are one flow: a six-digit code to the address.
`signInWith(OTP)` creates the account if the address is new, and
`verifyEmailOtp` with `OtpType.Email.EMAIL` covers both cases. Auth emails
carry `{{ .Token }}`, never a confirmation link, so there is no reset
flow and no deep links or redirect URLs on any target. A session also
proves the user receives mail at that address, which the invite model
relies on: invitations match on address alone.

Cost: email delivery is the only way in, with no fallback if Resend is
down. Sessions never expire, so this only matters on a new device or a
reinstall.

## Resend on a verified domain

Auth and invitation emails go through Resend from a sending subdomain,
with SPF and DKIM on a domain we control. Sign-in is code-only, so the
sender is critical: if it fails, nobody can sign in.

Rejected: Gmail SMTP. No SPF or DKIM under our control, a cap around
500/day, and the account can be disabled by the provider, taking sign-in
down with it.

The Supabase auth rate limit covers sign-in codes only; invitation emails
from Edge Functions never touch it.

## Sessions never expire

Time-boxing and inactivity timeout stay off. Only signing out or losing
local storage returns someone to sign-in.

supabase-kt stores the session unencrypted (`SharedPreferences`,
`NSUserDefaults`, not the Keychain). Accepted for a private app; a
Keychain-backed session store is a maybe, later.

## Inviter name via a security definer read

An invitee has no membership yet, so `profiles_select` hides the inviter.
`get_my_pending_invitations()` is `security definer` and reads the name
live; its own guard (`gi.email = my_email()`) is the access control, the
same pattern as `accept_group_invitation`.

Rejected: widening `profiles_select` (a new exception on the table where
every read rule is the trust boundary) and copying the name onto the
invitation (goes stale on rename).

## Email change keeps identity and memberships

A trigger copies Auth-side email changes into `profiles`, so memberships
survive and invite matching follows the new address. The app has no
email-change screen and `set_profile_name` changes only the name; the
trigger covers changes made in the dashboard or via the Auth API.

Rejected: treating a new email as a new user. Nothing could stop Auth
changing the address, and a stale `profiles` email would match invites
to an address the user no longer owns.

## The group invite email goes through pg_net

`invite_group_member_by_email` inserts, then calls
`send_group_invitation_email`, which queues the request with `pg_net`. The
send is visible in the code that causes it, can back a resend later, and
goes out only after commit.

Rejected: a Database Webhook. It fires on any insert, including from the
dashboard or a migration, and lives in dashboard config, not the repo.

Cost: a missing Vault entry fails the invite itself. Accepted: a failed
invite is easier to debug than a silently skipped email.

## The event invite email skips on missing config

Event invitations are also created by standby promotion, inside another
member's RSVP change. Raising there would stop members dropping out, so
`send_event_invitation_email` logs a warning and returns instead.

It can't check for an admin caller, because a member's RSVP calls it; it
has no grants, so only the database's own functions reach it. One request
per add or promotion, not per player: Resend limits requests per second
and nothing retries throttled ones.

## Writes go through RPCs

Table writes move behind definer RPCs, one at a time, and then the
table's write grants are revoked. An RPC checks the caller, normalises
input and does related work in one transaction: `invite_group_member_by_email`
checks the caller is an admin of a live group, normalises the address,
sets `invited_by` itself and queues the email. SCHEMA.md → Grants has the
order the revoke step needs.

Rejected: direct inserts guarded only by RLS, which can check a row but
can't normalise input or queue the email in the same step.

## Functions revoke PUBLIC themselves

Postgres grants `EXECUTE` on every new function to `PUBLIC`, which `anon`
inherits, so every migration that creates a function revokes it there.
Revoking from `anon` alone isn't enough.

RLS helpers live in a `private` schema PostgREST doesn't expose;
`authenticated` keeps `EXECUTE` on them because RLS evaluates them as the
caller.

`security definer` is used only where a function needs access ordinary
RLS doesn't give, and each such function carries its own checks.

## Two repos: Muster and Muster-env

Everything non-secret is in Muster. Real values live in a separate local
repo, `Muster-env`, one folder per environment, with `env` for build
values and records and `edge.env` for Edge Function secrets only, since
`supabase secrets set --env-file` uploads every line.

Every name carries its environment's prefix (`DEV_`, `PROD_`), and the
scripts refuse a mismatched line. They also take the project ref from the
same folder as the values, so the dev file can't be pushed to prod.

Rejected: a prefix-checking Edge Function. Two Supabase projects are
already separate stores; it would add detection, not isolation.

## Account deletion: cascade, not a tombstone

Play requires account deletion in the app and via a web link. Settings
has Delete account; the web link is a static `/delete-account` page.

One RPC, `delete_account(force)`. Without `force` it returns the groups
where the caller is the only admin and deletes nothing if there are any;
the app lists them and asks to confirm. With `force`, or when there are
none, it archives those groups, deletes invitations sent to the caller's
address, and deletes the `auth.users` row. Cascades remove the profile,
memberships and RSVPs; `created_by` and `invited_by` columns are set to
null.

`ensure_admin_remains` skips archived groups so the cascade can pass; a
group restored from the dashboard needs an admin set by hand. The event
freeze lets an update through when only `created_by` changes, or deleting
anyone who created a past event would fail. `get_my_pending_invitations`
left-joins the inviter, so an invitation from a deleted admin still
shows.

Rejected: a tombstone profile. It needs cleanup the cascades already do
and a placeholder email, and it disguises data instead of removing it.
Nothing needs the author kept: there is no history, and the one inviter
name shown has a fallback.

## Web text input uses an HTML `<input>` on Apple platforms

Compose for Web can't open the software keyboard in iOS 27 Safari, and a
typing problem was reported on a Mac too. On Apple platforms (iPhone,
iPad, Mac, any browser), the email, code and name fields render a real
`<input>` through `HtmlElementView`; every other platform keeps the
Compose field. The input is hidden while its screen isn't `RESUMED`, or
it floats over the next screen during transitions.

A workaround: remove it (`Adapted*` components) once Compose fixes the
bug.

## The privacy policy has one source

The policy is `privacy.html`, served by the web app at `/privacy`, which
is also the URL Play asks for. The app's Privacy policy screen embeds
that page in a web view, so there is one copy to keep current.

Rejected: a second copy in the app (drifts), an in-app browser sheet
(leaves the app on some devices), and a Markdown source with generated
outputs (two converters to maintain).

## The web app ships as a compatibility build

`composeCompatibilityBrowserDistribution` packs the Wasm and JS builds;
browsers without Wasm GC (Safari before 18.2) fall back to JS.

Rejected: a Wasm-only build, which shows those browsers a spinner forever.

## Android app ids per environment

Prod is `app.muster.prod`, dev is `app.muster.dev`, picked by `MUSTER_ENV`
from `set-env-vars.sh`, not by build type. With no `MUSTER_ENV` the build
falls back to dev. The prod id is fixed: Play records it when the app is
created.

## iOS environment follows the Xcode configuration

Debug builds dev, Release builds prod; Archive uses Release. Xcode's
Run and Archive never see a shell's exports, so `MUSTER_ENV` is set per
configuration in `Config.xcconfig` and the Kotlin build phase sources
`set-env-vars.sh` with it. The phase fails if no values load, so a
Release build can't fall back to `local.properties`. Bundle ids follow,
`app.muster.dev` and `app.muster.prod`, as on Android.

The Team ID comes from `Muster-env/<env>/ios.xcconfig`: xcconfig reads
`//` as a comment, so it can't include `env`.

Rejected: Android's shell-only switch, which Xcode's buttons can't reach.

## Android release: Play App Signing, no R8, no backup

Release builds are signed with an upload key kept in `Muster-env/prod/`;
Play re-signs with its own key. Minify stays off until crash reporting
exists, since a stripped class only fails at runtime. `allowBackup` is off
so a session or push token never moves to another device.

## One version for all clients, bumped on dev

Android, iOS and web are one codebase, so they share one `x.y.z` in
`version.properties`. Build numbers are derived from it, never set by
hand. iOS reads the file through an xcconfig `#include`, so no script
or generated file sits in between. The bump is the last commit on `dev`
before the release PR, so `main`'s version-bumped check can see it.

Rejected: a version per platform (three numbers for one codebase), a
version from git tags (the file is already what every build reads), and
a CI bump on `main` after the merge (needs a bot that bypasses `main`'s
protection, and the PR can't show the bump).

## Group creation: one live group, the allowlist lifts the limit

Anyone can have one live group they created; archived groups don't
count. Addresses on `private.group_creator_emails` have no limit and are
added on request via support. The limit is enforced in `create_group`.
An allowlist alone made the app invite-only for most new accounts, which
risks App Store guideline 3.2 (apps for specific organisations). If spam
shows up, the limit can drop to zero without an app change.

Deleting an account removes its address from the list, so a re-signup
starts at the limit.

Rejected: open creation with no limit (nothing stops spam groups and
invitation emails), and access that differs by app version (the version
is client-reported, and reviewers would see different behaviour from
live users).
