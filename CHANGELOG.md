# Changelog

One entry per release, newest first. Each lists what changed for users,
then the backend and tooling, and the platforms it shipped to.

## 1.0.7 — unreleased

Platforms: backend, web, Android, iOS.

- Admins can lock an event before it starts: no RSVP changes, invites,
  edits or queue changes for anyone until an admin unlocks it. Unlocking
  fills any free slots from standby.
- Admins can delete an event, including a locked or started one.
- "Can't make it" wraps and stays centred on narrow screens.
- Backend: `set_event_locked` and `delete_event`; the freeze triggers and
  roster RPCs reject writes on a locked event, and promotion waits for
  unlock.
- Tooling: navigation split into per-screen graphs with nav graph tests;
  screen UI tests run on Robolectric.

## 1.0.6 — 2026-10-15

Platforms: to confirm.

- Admins can edit a group's name and an event's title, time and
  location.
- Members tab counts members and pending invitations.
- Tooling: prod deploys run by hand with a checkbox per platform; new
  RUNBOOK.md; README rewritten with screenshots.

## 1.0.5 — 2026-10-11

Platforms: iOS (App Store and TestFlight), Android (Play internal testing).

- No user-facing changes.
- Tooling: CI on every PR, dev and prod deploy workflows, release
  prepare and finish workflows, version check and guard on `main` PRs.

## 1.0.4 — 2026-10-06

Platforms: backend only.

- Anyone can create one live group; archived groups don't count. Addresses
  on the allowlist have no limit.
- Deleting an account removes its address from the allowlist.

## 1.0.3 — 2026-10-05

Platforms: iOS (first release), web, backend.

- iOS app, iPhone only.
- `/open` opens the iOS app through a Universal Link, or sends iPhones to
  the App Store.
- New support page. Web pages show the app link only on devices that can
  install it.
- Fixed: on iOS web, inputs stayed hidden after returning to the browser.
- Backend: group-creation rights move to the `group_creator_emails`
  allowlist table; the app reads its profile through `get_my_profile`.
- Tooling: database backup script.

## 1.0.2 — 2026-10-01

Platforms: Android.

- Tooling: the prod deploy builds the Android bundle.

## 1.0.1 — 2026-09-30

Platforms: Android (first release), web, backend.

First public release.

- Sign-in with email codes, no passwords.
- Groups: create, invite members by email, resend or revoke invitations,
  promote admins, remove members, leave, archive.
- Events: create with a capacity, invite players, RSVP, standby queue
  with automatic promotion and drag-to-reorder, disinvite, resend
  invitations.
- Invitation emails for groups and events.
- Account deletion, in the app and on the web.
- Privacy policy screen.
