# Play Data safety

Answers for Play Console → App content → Data safety. They must match
`webApp/src/webMain/resources/privacy.html`: when one changes, check the
other.

Last checked against the policy: 26 September 2026.

## Links

Both on the prod web app, at prod's `WEB_APP_URL`:

- Privacy policy: `<WEB_APP_URL>/privacy`
- Account deletion: `<WEB_APP_URL>/delete-account`

## Data collection and security

| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | Yes |
| Is all of the user data collected by your app encrypted in transit? | Yes |
| Which methods of account creation does your app support? | Username and other authentication (email with a one-time code; no password) |
| Do you provide a way for users to request that their data is deleted? | Yes, in the app (Settings → Delete account) and at the account deletion link |

## Data types

Service providers acting for us (Supabase, Resend, Cloudflare) don't
count as sharing, so nothing is **shared**. Nothing is processed
ephemerally.

| Data type | Collected | Shared | Required or optional | Purposes |
|---|---|---|---|---|
| Personal info → Name | Yes | No | Required | App functionality, Account management |
| Personal info → Email address | Yes | No | Required | App functionality, Account management |
| Personal info → User IDs | Yes | No | Required | App functionality, Account management |
| App activity → Other user-generated content | Yes | No | Optional | App functionality |

- **Name:** entered on Set name, shown to members of the user's groups.
- **Email address:** sign-in codes and invitations. Covers the addresses
  an admin types in to invite someone.
- **User IDs:** the account ID behind each profile.
- **Other user-generated content:** group names, events, replies (in or
  out) and standby places. Optional: a user can belong to groups without
  creating or replying to anything.

Everything else is **not collected**: location, financial info, health
and fitness, messages, photos and videos, audio, files and docs,
calendar, contacts (device contacts; invite addresses are typed in),
web browsing, app info and performance (no crash logs or diagnostics),
device or other IDs, and other app activity (no analytics).

## Revisit when

- Crash reporting or analytics is added: App info and performance, and
  App activity → App interactions.
- Push notifications arrive: device or other IDs, for the push token.
- A new provider is added, or one starts using data for its own ends:
  that could turn "collected" into "shared".
