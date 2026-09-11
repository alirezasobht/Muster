# Muster — Screen Design (v3)

Design of record for the app's screens. Open `muster-screens-v3.html`
in a browser: one canvas, eighteen frames, pan and zoom. Frame ids (1a–1r)
are the reference names — use them in issues and commits.
(`muster-screens-v1.html` and `-v2.html` are earlier snapshots, kept for reference.)

Companion to `SCREENS.md` (screen map, navigation) and `CONTEXT.md`
(product decisions). Where this file and a screenshot disagree, this file wins.

## Visual direction

- Clean and light. Neutral surfaces, one accent: pitch green `#2F7D4F`.
- Accent is used only for primary buttons, selected states and the In status.
  Nothing else gets colour.
- Warning tint `#FBF2DE` / `#7A5A16` only for "event is full" notices.
- Ink `#14171A`, secondary `#4A524D`, muted `#6E7671`, hairline `#E4E7E4`,
  quiet surface `#F5F7F5`.
- Outline `#D7DCD8` for field and outlined-button borders; hint `#8A918C`
  for footnotes and hints under fields (1a footer, 1b resend line).
- Error text (wrong code, rate limit, failed save) uses the Out red
  `#9A3324`. Destructive *buttons* stay ink — see Archive group below.
- Light only. No dark theme is designed.
- **Frames are drawn at 390 px.** On anything wider — a desktop browser, a
  tablet — the content column is capped at 480 dp and centred, while the
  screen's background still fills the window, so 1a and 1q stay green edge
  to edge. `PhoneWidth` in `ui/common/components`, applied per screen.
- **Green ground** is used on exactly two screens: launch (1q/1r) and
  request code (1a). Inverted tokens there: white field with no outline,
  `#F0F7F2` labels and body, `#E2EFE6` footnotes, white primary button with
  a `#1F5C39` label, `#FBE9E7` error text (the Out red is unreadable on
  green). Everywhere else the surface is white or `#F5F7F5`.
- Type: DM Sans (UI), DM Mono (overlines, timestamps, capacity strings).
- Material-ish, not slavish: no bottom nav, no elevation theatre, no FAB
  shadow beyond the two extended FABs.

## Status badges

| Status | Fill | Text |
|---|---|---|
| In | `#E8F2EB` | `#1F5C39` |
| Pending | `#ECEFF4` | `#3F4C63` |
| Out | `#FBE9E7` | `#9A3324` |
| Invited (group member list) | none, `#D7DCD8` outline | `#6E7671` |
| No reply (frozen event) | none, `#D7DCD8` outline | `#6E7671` |

Admins get a chevron on roster badges — tap opens a status menu.
Members see the same badges, read-only.

## Frames

| id | Screen |
|---|---|
| 1a | Request code (green ground) |
| 1b | Enter code |
| 1c | Set name |
| 1d | Home |
| 1e | Home — empty |
| 1f | Settings |
| 1g | New group |
| 1h | Group — Events (admin) |
| 1i | Group — Members (admin) |
| 1j | Group — Members (member) |
| 1k | New event |
| 1l | Event (admin) |
| 1m | Event (member, unanswered) |
| 1n | Event — started (frozen) |
| 1o | Add players |
| 1p | Archive group (admin) |
| 1q | Launch |
| 1r | Launch — couldn’t start |

## Decisions this design fixes

- **Navigation** is a plain stack. No bottom nav. Group uses two tabs:
  Events and Members.
- **Event layout**: app bar with the group name as an overline above the
  event title, then details, then your own RSVP as its own block, then the
  roster, then the numbered standby queue.
- **Slots line** reads `10 of 10 slots · 7 in, 3 pending` — capacity counts
  invitations, not confirmations.
- **Roster** is one list sorted In, then Pending, then Out. Not grouped.
- **RSVP is a submission, not a toggle.** Unanswered: two buttons, *I'm in* /
  *Can't make it*, nothing preselected — you sit as Pending on the roster
  until you answer. Answered: the block collapses to your answer plus
  *Change*, which reopens the two buttons. Changing is allowed until
  `starts_at`.
- **Standby** is drag-to-reorder for admins (long-press the handle; drop
  commits the whole queue via `set_standby_order`). Moving someone to the
  front with a slot free promotes them immediately.
- **Add players** is a multi-select member list; each pick shows where it
  lands ("Invited", or "Standby #4" on a full event). There is no
  invite-or-bench choice.
- **New event** capacity stepper hint: "Invites count toward this, answered
  or not. Can't be changed later."
- **Members tab**: pending rows show an email address, a dashed mail avatar
  and an "Invited" badge, and are visible to admins only. Row actions (⋮):
  promote, demote, remove. "Add by email" sits at the top of the tab.
- **Archive group** lives in the Group app bar ⋮, admins only, above Leave
  group. Confirm dialog says it can't be undone in the app. The destructive
  button is ink, not accent and not red — there is no destructive colour in
  the palette.
- **Home empty state**: "No groups yet", a line saying invitations show up
  here, and the address they'll be sent to. New group button only if
  `can_create_groups`.
- **Frozen event** (past `starts_at`): a "Started at 7:00 pm · no more
  changes" strip under the app bar. RSVP collapses to a static badge, every
  control disappears (no handles, no add, no menus, no ⋮), Pending reads
  "No reply", the standby header reads "Standby · not called up". Identical
  for admins and members.
- **Set name** reuses the Settings name field and has no back button.
- **Launch** is the one screen where accent fills the surface — it reads as
  the app icon, not UI. On Android it is the splashscreen theme
  (`windowBackground` `#2F7D4F`, the M as the icon), so there is no second
  splash after the system one. The spinner appears after ~400 ms so a warm
  start never flashes it.
- **Launch failure** is the same screen, not a second one: the mark and
  wordmark hold position and the 260px zone below them swaps the spinner
  for a message plus *Try again* and *Sign out*. Sign out clears the session
  and goes to 1a — the escape hatch for a session the server rejects; it is
  hidden when there is no session, leaving Try again alone.

## Still open

- Past events tab.
- Notification preferences in Settings, once push lands.
- No destructive colour is defined; Out is the only red in the app.
- Offline behaviour beyond the launch failure state.
