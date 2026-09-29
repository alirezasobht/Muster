# Muster — Architecture

How the code is organised. Read this before writing any. Product rules
are in CONTEXT.md, the database in SCHEMA.md, the reasoning behind closed
choices in DECISIONS.md.

## Project setup

Created via the Kotlin Multiplatform wizard — **not** a standard Android
project. Targets: Android, iOS with **Share UI**, and Web.

```
shared/              shared module — UI + logic
  src/commonMain/    shared Compose UI, models, logic
  src/androidMain/ iosMain/ webMain/ jsMain/ wasmJsMain/
  src/commonTest/    + androidHostTest (JVM unit tests),
                     androidDeviceTest (Compose UI tests), iosTest, webTest
androidApp/          Android host — MainActivity only
iosApp/              Xcode project, thin SwiftUI wrapper
webApp/              Web host — Main.kt, index.html, styles.css, and the
                     static delete-account.html and privacy.html
```

Package: `app.muster`.

## On the Web target

It ships as the prod web app. It's the Wasm canvas target, with a JS
fallback for browsers without Wasm GC — heavy initial load, weak on
Safari — so it is not the path to a lightweight public invite page.

The cost of keeping it: `wasmJs` constrains `commonMain`. Every shared
dependency must support Wasm or it has to move into platform-specific
source sets. Supabase's Kotlin SDK does support wasmJs; smaller libraries
often won't.

Rule: if a library needed for Android/iOS lacks Wasm support, drop the
web target rather than the library.

## PhoneWidth

Every screen's content goes through `PhoneWidth`, and it owns three
things so no screen has to remember them:

- **The width cap.** 480 dp, centred. Screens are drawn at 390.
- **The safe-drawing inset.** Applied here rather than per screen, so it
  also covers content anchored to an edge — a FAB aligned to the bottom
  of a `Box` would otherwise sit under the three-button nav bar. Compose
  consumes insets it applies, so a screen that still calls
  `safeDrawingPadding()` is a harmless no-op; don't add new ones.
- **The wide-viewport surround.** Above 560 dp the surround is painted
  `Hairline` and the column white — DESIGN.md → "Wide viewports". It is
  not web-specific: Android tablets and landscape phones get it too.
  Screens that own a background colour pass
  `surround = Color.Transparent`, since their own `Surface` sits outside
  `PhoneWidth` and already fills the window. Only the green screens do
  — 1a, 1q, 1r.

## Layers

In `shared/src/commonMain/kotlin/app/muster/`:

```
data/
  di/           dataModule — client, repositories
  supabase/     createClient(), session storage
  dto/          @Serializable table shapes
  mapper/       dto -> model, and error mapping
  repository/   *RepositoryImpl
  fake/         Fake*Repository — in-memory, for ViewModel tests
domain/
  di/           domainModule — use cases
  model/        Profile, Group, GroupInvitation, Member, Event,
                EventDetail, RsvpStatus, ...
  error/        DomainError — one case per predictable DB rejection
                (InvalidName, InvitationNotPending, ...)
  repository/   interfaces
  usecase/      one class per operation, named *UseCase
ui/
  MusterApp.kt  entry point — theme + NavGraph
  di/           uiModule — ViewModels
  navigation/   NavGraph.kt, Screen.kt
  screens/      one folder per screen: XScreen.kt, XUiState.kt, XViewModel.kt
                launch/ signin/ setname/ home/ settings/ privacy/
                newgroup/ group/ addbyemail/ newevent/ event/ addplayers/
  common/       ErrorMessages.kt, components/
  platform/     expect/actual pieces a screen needs from its platform
  theme/
```

`data`, `domain` and `ui` are treated as separate modules, each owning its
own DI. There is no shared DI package and nothing at the root besides the
entry points. They are packages inside `:shared`, not Gradle modules, so
the compiler does not enforce the boundaries — keep imports pointing
inward: `ui` -> `domain` <- `data`.

`ui` follows the same file naming throughout: a screen's composable, its
UI state and its ViewModel share one name (`SetNameScreen`,
`SetNameUiState`, `SetNameViewModel`) and sit together in
`screens/<name>/`. One ViewModel per screen, not per flow. Anything two
screens share moves to `common/`, never into one screen's folder.

A tab inside a screen gets its own ViewModel once its own state outgrows
the shell's — Group's Members tab is the first case. The shell's
`XScreen` takes the tab body as a `@Composable () -> Unit` slot rather
than resolving the tab's ViewModel itself, so the shell stays Koin-free
and its previews keep working; `XRoute` fills the slot.

A screen's `XRoute` composable lives in the same file as `XScreen`. The
exception is Launch, because `LaunchViewModel` is hoisted above the
`NavHost` and shared rather than resolved per-route.

`XRoute` calls `XScreen` exactly once, passing the raw `XUiState` plus an
`XActions` bundle. `XScreen` does its own `when` over the state to pick
which branch to render, delegating to private composables per branch.
`HomeScreen` predates this — `HomeRoute` dispatches straight to
`HomeLoadingScreen` / `HomeFailedScreen` / `HomeScreen`. Treat that as
the one exception to fix opportunistically, not a second valid shape.

Two shapes for `XUiState`: a flat data class for a screen with one layout
and inline affordances — most screens. A sealed interface (`Loading` /
`Success` / `Error`) only when the screen has genuinely different
full-screen layouts per state. In-place cases (a refresh, an in-flight
action) stay as fields on `Success`, not further branches.

When a screen's callback list gets long, bundle them into one `XActions`
data class (`HomeActions`).

The layering is kept even where it looks like overhead — interfaces in
`domain`, implementations in `data`, a use case per operation, fakes
behind the same interfaces.

`domain` is unusually thin, because capacity, standby promotion, the
last-admin invariant and the freeze all live in Postgres. Use cases
mostly delegate. Do not re-implement those rules in Kotlin: the client
cannot enforce them, and a second copy would drift.

`DomainError` is where the layering earns its keep. The database rejects
things the client cannot predict — last admin, full event, started event
— so `data/mapper` turns Postgres error codes into typed errors in one
place. Without it the UI shows raw `PostgrestRestException` strings.

There is no network module. supabase-kt *is* the client: Postgrest builds
the REST calls, Auth handles sessions and refresh, Ktor is the engine
underneath. Repositories call `supabase.from("groups")` directly — no API
interface, no manual JSON, and no `where user_id = ...`, since RLS
decides what comes back. Retrofit would not work here anyway; it is
JVM-only and cannot live in `commonMain`.

## DI: Koin

One module per layer, each in that layer's `di/` package: `dataModule`
(client, repositories), `domainModule` (use cases), `uiModule`
(ViewModels). A layer's module appears once it has something to declare.
Plus a platform module per target for anything platform-specific.

`initKoin()` loads them all. It lives at the root (`app/muster/Koin.kt`),
as an entry point rather than a DI package — the one place that sees every
layer. Each host calls it before any UI: `MusterApplication` on Android,
`MainViewController` on iOS, `Main.kt` on web.

Android calls it from an `Application` subclass, not `MainActivity`:
`onCreate` of an activity re-runs on every rotation and configuration
change, and Koin throws if started twice. `initKoin()` also guards against
an already-started Koin, because iOS may call `MainViewController()` more
than once.

The `SupabaseClient` is a Koin `single` built by a `createClient()`
factory, not a top-level `val`, so the fakes can replace it. Repositories
are `single`; use cases are `factory`, being stateless and cheap.
ViewModels use `viewModelOf`. One exception: `EnterCodeViewModel` takes
the address as a route argument, so it is declared as
`viewModel { (email: String) -> ... }` and resolved with `parametersOf`.

## Navigation

JetBrains' `navigation-compose`, with type-safe routes: `@Serializable`
classes in `ui/navigation/Screen.kt`, one `NavHost` in `NavGraph.kt`.
It gives a real back stack, so the Android system back button and the
browser's back button both work without per-screen handling.

Routing is driven by the session, not by the screens. `LaunchViewModel`
is resolved above the `NavHost` — one instance, surviving every
navigation — and observes `SessionState`. A change navigates and clears
the stack with `popUpTo(0)`: there is no going back to a screen that
belonged to a different sign-in state. `Loading` and `Failed` navigate
nowhere, leaving whatever is on screen in place, which is what
`SessionState.Unreachable` requires mid-session.

Screens never navigate on their own. They expose a result on their UI
state — `RequestCodeUiState.sentTo`, `SetNameUiState.saved` — and the
graph acts on it, then calls back to clear it so returning to the screen
does not navigate again.

## What goes on UI state, what goes in the ViewModel

A **derived property** belongs on the UI state:
`SettingsUiState.isSameName`, `NewGroupUiState.canCreate`. The test is
narrow — `val x get() = <expression over this state's own fields>`, no
dependencies, no side effects. Keeping it there means the state cannot
contradict itself: a button cannot be enabled under conditions the state
does not reflect. Recomputing it in the ViewModel would mean a field
every `copy` has to remember to update, which is how a button ends up
live when it should not be.

Anything needing something **outside** the state is ViewModel work:
calling a use case, deciding what to do with a failure, the in-flight
guard. `SettingsViewModel.onSave` reads `isSameName` but decides there —
that is the split working.

Don't put classification logic in the state class either. `canCreate`
asks `error?.isTerminal != true`; the rule behind `isTerminal` lives in
`ui/common/ErrorPresentation.kt`, once, not inlined as a cast in each
state.

The same split applies to permission flags. A Members-tab row's
`canPromote`/`canDemote`/`canRemove`/`canRevokeInvitation` (`MemberRow`)
are booleans `MembersViewModel` sets once, from the viewer's role and
whether the row is their own. The composable only ever reads them — it
never takes an `isAdmin` and re-derives who can do what, which would put
the same role check in two places and let them disagree.

## Two kinds of error

DESIGN.md → "Two kinds of error" has the visual treatment. In code, the
classification lives in `ui/common/ErrorPresentation.kt` and nowhere
else:

- `DomainError.presentation` returns `Field` or `Form(severity)`.
  `Field` renders under the input it is about; `Form` renders as a filled
  block above the action button.
- `Severity.Terminal` means retrying the same request fails the same way
  — permission, rate limit — so the action stops being offered.
  `Retryable` — offline, unknown — leaves it live.
- `DomainError.isTerminal` is the question screens actually ask, and what
  a state's `canX` should call.

A screen keeps the two on separate fields (`nameError` and `error`,
`emailError` and `error`) rather than one, because both can be present at
once. Editing any field clears the form error; pressing the action clears
both before the call.

This is for failed **writes**. A failed load keeps the full-screen
treatment instead — `HomeFailedScreen`, frame 1t.

## Data changes

`DataChanges` (`domain/event/`) is a broadcast bus for writes another
screen's data depends on. **Repositories notify, ViewModels subscribe** —
notifying from the ViewModel means a new call site can forget to.

`SharedFlow`, not `Channel`: it is a broadcast, and an emission with no
listener should be dropped, since a screen that does not exist yet loads
fresh when created.

Add a `DataChange` case when a screen needs one, not before. A ViewModel
that subscribes should not also refetch inline after its own write — the
repository already announced it, and doing both fetches twice.

This sits alongside refresh-on-resume, which stays the safety net.

## One-shot events

A thing that should happen **once** — a "Saved" confirmation, a toast —
is an event, not state. It goes on the ViewModel as a private `Channel`
exposed as a `Flow` (`SettingsViewModel.saved`), collected in `XRoute`.

A boolean on the UI state does not work: state survives backgrounding,
so the confirmation reappears when the screen comes back, long after the
save. `Channel` rather than `MutableSharedFlow` because a SharedFlow with
no replay drops an emission when nothing is collecting; a buffered channel
holds it and delivers it exactly once.

The transient display state — how long "Saved" stays up — belongs in the
route, not the ViewModel: collect, set a local flag, `delay`, clear.

This is only for events. Anything the screen should still be showing
after a rotation or a return from background is state.

## Kotlin style

- **No trailing comma** after the last argument in a call, a parameter
  list, or a collection literal. Android Studio's Kotlin formatter adds
  them by default — turn off Settings -> Editor -> Code Style -> Kotlin ->
  Other -> "Use trailing comma", or it will put them back on reformat.
- **ktlint enforces the style.** Rules live in `.editorconfig` (same as
  Vela's) and in the `:ktlint-rules` module, which holds two custom rules
  that replace the disabled `multiline-expression-wrapping`. Run
  `./gradlew ktlintFormat` to fix, `./gradlew ktlintCheck` to verify. The
  plugin is applied to every module except `:ktlint-rules`, which gets it
  from its own build file. `MainViewController.kt` is exempted from
  `function-naming` because Swift calls it by that name.
- **Comments are the exception, not the default.** Write one only for a
  detail that is not visible in the code: an edge case, a constraint from
  outside the file, or something that silently breaks if changed. No
  restating what the line does, and no KDoc on every declaration.

## Testing

ViewModel tests live in **`androidHostTest`**, not `commonTest`. They
need `Dispatchers.setMain`, which is flaky on JS and Wasm, and
`commonTest` runs on every target. The ViewModels are common code, so
testing them once on the JVM covers all four. Every test needs
`MainDispatcherRule` (`androidHostTest`, `app/muster/testing/`).

ViewModels are tested against the **fakes** in `data/fake`, never against
mocks. Each fake takes an error per method so failure paths can be
driven. Where a fake has no call counter, prove a call did not happen by
arming its error and asserting nothing surfaced. They are `commonMain`,
not `commonTest`, so they ship in the release binary — accepted for a
private app.

**Set a ViewModel's in-flight flag before `launch`, never inside it.**
The guard reads the flag synchronously. Setting it inside the coroutine
works on device only because `viewModelScope` uses `Main.immediate`;
under a standard test dispatcher two taps in one frame both got through,
and sent two codes.

`androidDeviceTest` holds a handful of Compose UI tests over the
stateless screens — enabled and disabled states, callbacks, error
rendering. Not full flows. Screens are driven with literal state, so no
Koin and no `NavHost` is involved. One test file per screen, covering all
its state composables.
