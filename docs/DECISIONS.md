# Muster — Decisions

Closed decisions and why. Nothing routine needs this file — the
conclusions live where they apply, in CONTEXT.md, ARCHITECTURE.md and
SCHEMA.md. This is the argument behind them, kept so they aren't
relitigated every few chats.

Reopening one is fine. Reopening it without reading the reason it was
closed is not.

## Native over PWA

A PWA was seriously considered: it would have avoided the $99/yr Apple
Developer fee and covered Android, iOS and web from one build with no app
stores at all.

Rejected because iOS web push is unreliable — silent unsubscriptions,
listeners failing to fire after a restart, and push only working at all
once the user has added the site to their home screen through Safari's
share menu. Notifications are the core feature of the app; a missed one
means a missing player. Android PWA push is solid, but the weakest
platform sets the bar.

## Compose Multiplatform over Flutter

Flutter is the more mature cross-platform option — bigger plugin
ecosystem, longer-settled iOS story. It doesn't pay off at this size, and
the cost is learning Dart, a new widget system and new state management.

Existing Kotlin and Compose skills transfer directly, so time to a working
app is shorter despite the thinner ecosystem.

## Supabase over Firebase

The data is relational. Standby queues need ordered SQL; Firestore would
mean maintaining position numbers by hand on every document and rewriting
them whenever someone drops out.

Supabase also has an official Kotlin Multiplatform SDK. Firebase on KMP
relies on GitLive's community wrapper — one more dependency outside our
control.

FCM is still the push path when push lands, so Firebase's bundling
advantage was never really on the table.

## Supabase over a self-hosted Ktor backend

Ktor would give shared models across client and server, but means writing
auth, running Postgres, migrations, deployment, and hosting (~$5/mo).

More importantly, Supabase enforces visibility in RLS at the database;
with Ktor every check is code that must not be forgotten in any endpoint.

Revisit only if the standby logic outgrows an Edge Function.

## Config injected via a generated Kotlin file, not a plugin

A Gradle task in `shared/build.gradle.kts` reads `SUPABASE_URL` and
`SUPABASE_PUBLISHABLE_KEY` from the environment, falling back to
`local.properties` (gitignored), and generates `app.muster.SupabaseConfig`
into `commonMain`.

`BuildConfig` is Android-only and `expect`/`actual` would mean four copies
of two strings. BuildKonfig would work but is a third-party plugin whose
Wasm support is one more thing to verify — exactly what the web-target
rule in ARCHITECTURE.md exists to avoid.

Any future build-time config value follows the same path rather than
adding a plugin.

The publishable key is not a secret — it ships in every APK and in the web
bundle. Keeping it out of git is rotation convenience, not security; RLS
is the trust boundary. The secret key never reaches the client.

## Email codes, no passwords

Signup and sign-in are one flow: a six-digit code sent to the address.
`signInWith(OTP)` creates the account if the address is new;
`verifyEmailOtp` with `OtpType.Email.EMAIL` covers both the new and
existing cases.

No password means no reset flow to build, and every auth email carries
`{{ .Token }}` rather than `{{ .ConfirmationURL }}`, so there are no deep
links or redirect URLs on any of the four targets.

It also hardens the invite model. SCHEMA.md lists "sign-in must prove the
email is yours" as a known limit, because invitations match on address
alone. With code-only sign-in a session is unreachable without receiving
mail at that address, so confirmation stops being a toggle that could be
turned off.

Cost: email delivery is the only way in, with no fallback if Resend is
down. Sessions never expire, so this only bites on a new device or a
reinstall.

## Resend on a domain, after Gmail SMTP

Auth emails go through Resend from `send.musterapp.fyi`, with SPF and
DKIM on a domain we control (`musterapp.fyi`, about $5.66/yr). Event
invitations will go the same way, from an Edge Function.

This is the second answer. Resend came first and was dropped: its free
tier only delivers from a verified domain, and without one it falls back
to a shared test sender that reaches nobody but the account holder. Gmail
SMTP needed no domain, no DNS and no money, so it won — with two known
costs recorded at the time: no SPF or DKIM under our control, and a
sending cap around 500/day.

That ended when Google disabled the dedicated account the app sent
through. Sign-in is code-only, so a disabled sender is a total outage —
nobody can sign in at all. A personal mailbox turned out to be the wrong
foundation for the one thing the whole app depends on, and a domain at
$5.66/yr was never the real obstacle.

What the domain also buys: deliverability that does not depend on a
consumer provider's spam heuristics, a sender address that is not
someone's Gmail, and sending limits that fit a transactional provider
rather than a personal account.

Note the Supabase auth rate limit covers **sign-in codes only**.
Invitations sent from an Edge Function never touch it.

## Sessions never expire

Time-boxing and inactivity timeout stay off in Auth → Sessions. Only
signing out or losing local storage returns someone to the login screen.

supabase-kt keeps the session in `SharedPreferences` on Android and
`NSUserDefaults` on iOS — not Keychain, not encrypted. Accepted for a
private app; a Keychain-backed `SessionManager` is a maybe, later.

## Inviter name on invitations: a security definer read, not a wider policy

Home's invitation card names who invited you (DESIGN.md 1d). `profiles_select`
only allows reading your own profile or a co-member's — an invitee has no
`group_members` row yet, so a plain join from `group_invitations` to
`profiles` returns nothing.

Two other fixes were on the table: widen `profiles_select` to cover this
case, or denormalize the inviter's name onto `group_invitations` at insert
time. Widening the policy means a new, narrower exception on `profiles` —
the one table where every read rule doubles as the trust boundary, so
loosening it wants real caution. Denormalizing avoids touching RLS but
leaves a stored copy that goes stale if the inviter renames themselves
later.

`get_my_pending_invitations()` reads live and touches no policy: `security
definer` means it runs as its owner, so it can join to `profiles`
regardless of the caller's own row visibility, and its own guard
(`gi.email = my_email()`) is the entire access control — same discipline
as `accept_group_invitation`, so no new pattern in the schema.

## Email change: identity and memberships survive

An earlier rule said an email change meant a new user. It was
unenforceable: nothing in the schema could stop Auth from changing the
address, and leaving `profiles` stale would have pointed invitation
matching at an address the user no longer owns.

So a trigger copies any Auth-side change into `profiles`. Identity and
memberships survive; only the invite lookup key moves.

The app still builds no email-change screen, and the column grant on
`profiles` allows only `name`. The trigger exists for changes made from
the Supabase dashboard or the Auth API — both outside these tables.

## The invite email goes through pg_net, not a database webhook

The first design was a Database Webhook on `group_invitations` insert.
Replaced with an explicit call: `invite_group_member_by_email` inserts,
then calls `send_group_invitation_email`, which queues the request with
`pg_net`.

An explicit call makes the send visible in the code that causes it, and
the same function can back a resend action later. A webhook fires on any
insert however it happens — including from the dashboard or a future
migration — and lives in dashboard configuration rather than the repo.

`pg_net` sends only after the transaction commits, so a rolled-back
invite sends nothing.

Cost: the invite now depends on the email's configuration. With either
Vault entry missing the function raises and takes the invitation with
it. Accepted, and made a hard setup step rather than softened, because a
silently skipped email is worse to debug than a failed invite.

## Invitations are an RPC, not a direct insert

The app used to insert into `group_invitations` directly, relying on RLS.
`invite_group_member_by_email` replaced that: it checks the caller is an
admin of a live group, normalises the address, sets `invited_by` itself,
and queues the email in the same transaction.

Part of a wider direction — moving writes behind RPCs one at a time, then
revoking the broad table grants. SCHEMA.md → Grants has why that last
step needs care.

## Functions revoke PUBLIC themselves

Postgres grants `EXECUTE` on every new function to `PUBLIC`, which
`anon` inherits. A hardening migration revoked it everywhere, and every
function since revokes it in its own migration.

Revoking from `anon` alone looks right and isn't — the `PUBLIC` grant
still reaches it.

The six RLS helpers also moved to a `private` schema PostgREST doesn't
expose, so they stop appearing as callable RPCs. `authenticated` keeps
`EXECUTE` on them, since RLS evaluates them as the caller.

The four existing `security definer` RPCs stay that way: each needs
access ordinary RLS doesn't give, and each has reviewed checks of its
own.

## Two repos: Muster and Muster-env

Everything non-secret is in Muster — migrations, Edge Function source,
`config.toml`, setup docs, scripts. Real values are in a separate local
repo, `Muster-env`, one folder per environment.

Each environment has two files: `env` for the build and your own records,
`edge.env` for Edge Function secrets only. Separate because `supabase
secrets set --env-file` uploads every line it's given, and the database
password has no business being a function secret.

Every name carries its environment's prefix — `DEV_RESEND_API_KEY`,
`PROD_RESEND_API_KEY`. The scripts refuse any line with the wrong one, so
a prod value pasted into the dev file is caught before it's uploaded.

The scripts take the project ref from the same folder as the values
rather than as an argument. Typing the ref is where the mistake would
happen — the dev file uploaded to the prod ref — and a check afterwards
would catch it only once the damage was done. Removing the second input
makes it impossible instead.

A prefix-checking Edge Function was considered and rejected: secrets in
two Supabase projects are already fully separate stores, so it would have
added detection, not isolation, and left stray values behind.
