# Muster — Screen Design (v8)

Design of record for the app's screens. Open `muster-screens-v7.html`
in a browser: one canvas, pan and zoom. Turn 1 holds the twenty screens
(1a–1t); turn 2 the form-level error treatment (2a–2d); turn 3 the
wide-viewport treatment (3a–3c); turn 4, at the top, the event screen
re-layout (4a–4c), which supersedes 1l's layout. Frame ids are the reference
names — use them in issues and commits. (`muster-screens-v1.html`
through `-v7.html` are earlier snapshots, kept for reference. The app icon
has its own file — see *App icon* below.)

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
| 1s | Home — loading |
| 1t | Home — couldn’t load |
| 2a | Request code — form error (green ground) |
| 2b | New group — form error, terminal |
| 2c | New group — form error, retryable |
| 2d | Field error and form error together |
| 3a | Home, wide viewport |
| 3b | Home empty, wide viewport |
| 3c | Request code, wide viewport (green, no tint) |
| 4a | Event — admin, answered (supersedes 1l layout) |
| 4b | Event — admin, no one invited |
| 4c | Event — scrolled to standby |

## Wide viewports

The phone layout is unchanged — this is presentation around it, not a
responsive relayout. No two-column, no master-detail, no wider column.

**A flat tint and nothing else.** Above the column width the surround
becomes `#E4E7E4` and the content column stays white. That is the whole
treatment.

- **Tint**: `#E4E7E4` — the hairline colour, promoted to a surface. No
  new token. One value step from white: enough to give the column an
  edge, not enough to read as a frame or a backdrop.
- **Delineation**: none. No border, no shadow, no rounded corners. The
  tint *is* the hairline colour, so a hairline border would be invisible
  against it — the choice of tint retires the question rather than
  answering it. A shadow or radius would make the app a card floating on
  a page, which is the failure being fixed, not a fix for it.
- **The column is full height, edge to edge.** No margin above or below.
  Scrollable content reaches a real bottom edge, and the FAB has
  something to anchor to.
- **App bar stays inside the column.** Spanning the window would put
  Settings a mouse-length from the list it belongs to, and would make the
  bar the only element aware the window is wide while everything below it
  is still a phone.
- **FAB stays at the column's bottom-right**, not the window's. It acts
  on the list; out in the tint it belongs to nothing.
- **Screens that own a background colour keep it edge to edge.** 1a, 1q
  and 1r stay green across the whole window with no tint and no column
  edge; the column then governs only where the content sits. The surround
  exists to give a *white* screen an edge, and green already has one.
  Consequence: signing in is the one moment the fill narrows to a column.
  That is accepted — the app is a phone app and should look like one.
- **Applies above ~560 dp** in any direction-agnostic sense: desktop web,
  Android tablets, and phones in landscape all get it. Below that the
  column fills the window and there is no surround.
- Sizes are unchanged: 390 dp design width, column capped at 480 dp,
  centred.

Note for CONTEXT.md's "web is exploratory": nothing here is web-specific
and nothing here is load-bearing. If the web target is dropped, the same
rule still earns its place on tablets and landscape phones.

## Two kinds of error

Errors come in two shapes and get two treatments. The test is what the
message is about, not how bad it is.

**Field error — about the contents of a field.** Unchanged: bare text
under the field, 13px, `#9A3324`, no fill and no icon; the field's outline
turns `#9A3324`. On the green ground (1a) the text inverts to `#FBE9E7`.
Focus moves to the field. "Enter a name", "That code is only 5 digits",
"Keep it under 40 characters."

**Form error — about the request.** A filled block sitting immediately
above the screen's action button: `#FBE9E7` fill, 12px radius, 12/14
padding, 14px `#9A3324` text, a round 18px `#9A3324` outlined "!" mark at
the left, 12px clear between block and button. Fields are untouched — no
red outline, no focus move. "Couldn't reach Muster", "Too many attempts",
"You're not allowed to create groups."

The rules:

- **Placement is above the action button, always** — the last thing read
  before the control that failed. Not under the field, not a snackbar: a
  snackbar outlives the screen and can be missed, and these messages must
  stay until resolved.
- **Fill and mark are the distinguishing pair.** A field error is bare
  text with no mark; a form error is a filled block with one. Colour alone
  would not separate them, and the mark alone reads as decoration.
- **The block never carries its own button.** The screen's action button
  *is* the retry. A retryable error (network, timeout) leaves the button
  live; a terminal one (permission, rate limit) greys it, and for a timed
  error it re-enables and the block clears when the window passes.
- **On green (1a) the block keeps the Out red.** The block is its own
  surface, so the "no `#9A3324` on green" rule — which is about bare text
  — does not apply. Field errors on 1a still invert to `#FBE9E7`.
- **Both at once**: both show, neither suppressed — they answer different
  questions. Maximum two blocks per screen, one per slot. Normally
  impossible, since a client-side field error blocks the call; it happens
  only when the server rejects a field and the request also fails.
- **Clearing.** Both are transient. Editing any field clears the form
  error (a keystroke makes the request stale) and its own field error;
  pressing the action button clears both before the call goes out. Neither
  survives navigation.
- **Applies to** 1a, 1b, 1c, 1f and 1g — every screen whose primary action
  is a write. Screens whose *load* fails use the 1t empty-state frame
  instead; the form block is for a failed write, 1t for a failed read.

## Event screen layout (4a–4c)

Same content and behaviour as 1l; the layout changes so each band reads as
a different kind of thing.

- **App bar**: group name as a small uppercase overline, event title below
  at 19px bold, ⋮ at right. A hairline appears only once content scrolls
  under it.
- **When / where**: date and time at 18px semibold, place muted beneath.
- **Slots panel**: the only filled surface (`#F5F7F5`, 14px radius). Large
  "6 of 10 slots" numerals, "4 open" at right, a proportional bar (in →
  pending → grey track), and a legend: in, pending, out (hollow dot —
  out holds no slot so it has no bar fill). The bar is continuous, never
  one segment per slot, so it holds up at 30+ slots; the numerals carry
  the exact figure. Empty event: grey track, no legend.
- **RSVP card**: the only outlined surface — "You're in", when answered,
  and Change. Absent when the viewer isn't on the roster.
- **Players**: section header "Players" + count, with a tonal "+ Add"
  button (36dp drawn, 48dp touch target, admins only). Rows are 56dp with
  hairline dividers and no per-row boxes; status is a tinted chip.
- **Empty roster**: the header Add disappears and the empty state carries
  the single filled "Add players" button — never two Adds on screen.
- **Standby**: sits on the same tint as the slots panel (it is that panel's
  overflow); numbered discs for queue position; drag handles as before.

## App icon

Chosen mark: **K, “Roster on the card”** — a white date card with two
binding rings on Muster green, the M and a row of five dots (four in, one
open) cut through it. Explorations and platform previews live in
`Muster App Icon.dc.html` (turns 1–4, A–L, K1–K4); shipping files in
`exports/app-icon/`, with a README mapping each to its KMP location.

Why this one:

- **Any event, not football.** The card says “event”, the dots say “who's
  coming” — the app's actual job. Pitch imagery (D) was rejected for tying
  the brand to one sport.
- **One object.** A single silhouette inside Android's 66dp safe circle, so
  every launcher mask (circle, squircle, teardrop) frames it the same way.
  The alternative with dots below the card (L) crowded circle masks.
- **Themes cleanly.** The card is the only filled shape; M, dots and ring
  gaps are cut-outs. Android monochrome and iOS tinted are the same path
  in one colour.

Rules:

- **Two drawings, never more.** The full mark, and a small mark for 32px
  and below (card, M, and a bar replacing the dots — dots at that size
  are texture). Everything else is a recolour or a mask of these two.
- **The M is a drawn path**, not DM Sans set as text — no font dependency
  in any build.
- **Colours.** Light: ground `#2F7D4F`, card `#FFFFFF`. Dark (iOS): ground
  `#14171A`, card `#5BAF7A`. Tinted (iOS): white on black, system applies
  the hue. Themed (Android): monochrome layer, launcher-coloured.
- **Corners belong to the platform.** iOS and Apple touch icons ship square
  and opaque; the web favicon keeps a small radius; PWA ships a rounded
  “any” icon and a full-bleed “maskable” one with the mark at 90%.
- **Adaptive icon**: background is solid green, foreground is the card on a
  transparent layer at 72% of the 108dp canvas, so the bleed zone is plain
  green and parallax never reveals an edge.

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
- **Home loading** (1s) is first load only, and only after ~400 ms — a cached
  list renders straight into 1d. The app bar, wordmark and Settings hold
  position so nothing shifts when the list arrives; the New group button is
  absent until `can_create_groups` is known, rather than appearing and
  possibly vanishing.
- **Home failure** (1t) reuses the empty state's frame — dashed mark, title,
  one line, one action — so the two read as one family. Title:
  "Couldn't load your groups"; line: "Check your connection. Your groups are
  safe — nothing has changed." No email address here: the session is fine,
  the fetch is not, and showing an address invites the wrong fix. *Try again*
  is outlined, not filled — retry is a repair, not the screen's intent — and
  returns to 1s.
- **Busy states update in place.** Beyond first load, a refresh or a pending
  write never swaps the screen for a loader: the list or roster stays on
  screen and the affected card carries its own spinner. 1s and 1r are the
  only full-screen waits in the app.
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
- Offline behaviour beyond the launch and home failure states.
- Whether the tint should darken slightly on very wide windows (>1400 dp),
  where the surround is most of the screen. Assumed no — one value.
- Form-error copy is not yet written for 1b, 1c and 1f; the treatment is
  fixed, the strings are not.
- Whether the form block should be announced to screen readers as an
  assertive live region on appearance — assumed yes, unverified.
- Group and event screens have no designed loading or failure frames yet;
  they should follow 1s/1t (in-place spinner, empty-state-shaped failure).
