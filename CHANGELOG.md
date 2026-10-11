# Changelog

One entry per release, newest first. Each lists what changed for users,
then the backend and tooling, and the platforms it shipped to.

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
