# Muster — Architecture

How the code is organised. Read this before writing any. Product rules
are in CONTEXT.md, the database in SCHEMA.md, the reasoning behind closed
choices in DECISIONS.md.

## Project setup

Created via the Kotlin Multiplatform wizard (JetBrains KMP plugin in
Android Studio, or kmp.jetbrains.com) — **not** a standard Android
project.

Targets: Android, iOS with **Share UI**, and Web. Desktop and Server
unchecked.

```
shared/              shared module — UI + logic
  src/commonMain/    shared Compose UI, models, logic
  src/androidMain/
  src/iosMain/
  src/jsMain/
  src/wasmJsMain/
  src/commonTest/    + androidHostTest (JVM unit tests),
                     androidDeviceTest (Compose UI tests), iosTest, webTest
androidApp/          Android host — MainActivity only
iosApp/              Xcode project, thin SwiftUI wrapper
webApp/              Web host — main.kt, index.html, styles.css
```

Package: `app.muster`.

Xcode must be installed; the plugin's preflight checks will flag it
otherwise.

## On the Web target

Included to experiment with, not committed to. It's the Wasm canvas
target — heavy initial load, weak on Safari — so it is not the path to a
lightweight public invite page.

The cost of keeping it: `wasmJs` constrains `commonMain`. Every shared
dependency must support Wasm or it has to move into platform-specific
source sets. Supabase's Kotlin SDK does support wasmJs; smaller libraries
often won't.

Rule: if a library needed for Android/iOS lacks Wasm support, drop the
web target rather than the library.

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
  model/        Profile, Group, GroupInvitation, ... (Member, Event, Rsvp,
                StandbyEntry arrive with Group/Event screens)
  error/        DomainError — one case per predictable DB rejection
                (InvalidName, InvitationNotPending, ...); more arrive as
                Group/Event screens do
  repository/   interfaces
  usecase/      one class per operation, named *UseCase
ui/
  MusterApp.kt  entry point — theme + NavGraph
  di/           uiModule — ViewModels
  navigation/   NavGraph.kt, Screen.kt
  screens/      one folder per screen: XScreen.kt, XUiState.kt, XViewModel.kt
                launch/ signin/ setname/ home/ group/ event/ settings/
  common/       ErrorMessages.kt, components/
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

A screen's `XRoute` composable (resolves the ViewModel via
`koinViewModel()`, collects state, forwards plain navigation callbacks
like `onBack`/`onNameSet` up to `NavGraph`) lives in the same file as
`XScreen`, not a separate file — every screen does this except Launch
(`LaunchRoute.kt` is separate because `LaunchViewModel` is hoisted above
the `NavHost` and shared, not resolved per-route).

Two shapes for `XUiState`, pick by what the screen actually needs: a flat
data class with nullable/boolean fields for a screen with one layout and
inline affordances (a button disables, a label swaps for a spinner,
error text appears below a field) — most screens. A sealed interface
(`HomeUiState`: `Loading` / `Success` / `Error`) only when the screen has
genuinely different full-screen layouts per state, per SCREENS.md
"Staying current" and the loading/failure frames in DESIGN.md — the
in-place cases (a refresh, an in-flight action) still live as fields on
the `Success` case, not further sealed branches, since the list stays on
screen either way.

When a screen's callback list gets long, bundle them into one `XActions`
data class (`HomeActions`) instead of listing five-plus lambda params —
`XScreen` takes `actions: XActions` alongside its data/state params.

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
`MainViewController` on iOS, `main.kt` on web.

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

## Kotlin style

- **No trailing comma** after the last argument in a call, a parameter
  list, or a collection literal. Android Studio's Kotlin formatter adds
  them by default — turn off Settings -> Editor -> Code Style -> Kotlin ->
  Other -> "Use trailing comma", or it will put them back on reformat.
- **Comments are the exception, not the default.** Write one only for a
  detail that is not visible in the code: an edge case, a constraint from
  outside the file, or something that silently breaks if changed. No
  restating what the line does, and no KDoc on every declaration.

## Testing

ViewModel tests live in **`androidHostTest`**, not `commonTest`. They
need `Dispatchers.setMain`, which is solid on JVM and Native but flaky on
JS and Wasm, and `commonTest` runs on every target. The ViewModels are
common code, so testing them once on the JVM still covers all four.

Every ViewModel test needs `MainDispatcherRule` (in `androidHostTest`,
`app/muster/testing/`): `viewModelScope` runs on `Dispatchers.Main`, which
has no implementation off Android, so without it the first `launch {}`
fails.

ViewModels are tested against the **fakes** in `data/fake`, never against
mocks. Each fake takes an error per method (`requestError`, `getError`,
...) so failure paths can be driven, and counts calls so "sent exactly
one code" is checkable. They are `commonMain`, not `commonTest`, so they
ship in the release binary — accepted for a private app.

**Set a ViewModel's in-flight flag before `launch`, never inside it.**
`sending`, `verifying` and `saving` guard against a second tap, and the
guard reads the flag synchronously. Setting it inside the coroutine works
on device only because `viewModelScope` uses `Main.immediate`; under a
standard test dispatcher two taps in one frame both got through, and sent
two codes. The guard must not depend on which dispatcher is in play.

`androidDeviceTest` holds a handful of Compose UI tests over the
stateless screens — enabled and disabled states, callbacks, error
rendering. Not full flows: the routing they would exercise is already
covered by `LaunchViewModelTest`. Screens are driven directly with
literal state, so no Koin and no `NavHost` is involved. A screen with
several sealed-state composables (`HomeScreen`, `HomeLoadingScreen`,
`HomeFailedScreen`) gets one test file covering all of them, not one per
composable.

Find a text field with `hasSetTextAction()`, not `onNodeWithText(label)`:
the label is a separate node above the field and has no focus action, so
text input against it fails.
