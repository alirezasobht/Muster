package app.muster.domain.model

import kotlinx.datetime.TimeZone

// JVM and Darwin carry IANA tz data built in; JS and wasmJs need a shim
// loaded first, or TimeZone.of throws IllegalTimeZoneException at runtime
// despite compiling fine everywhere. See each platform's actual.
internal expect fun loadTimeZoneDatabase()

// Every group plays in Sydney for now (CONTEXT.md, "Time zones"). Never
// TimeZone.currentSystemDefault(): an admin creating or reading an event
// while travelling must see the pitch's kickoff time, not their own.
val MusterTimeZone: TimeZone by lazy {
    loadTimeZoneDatabase()
    TimeZone.of("Australia/Sydney")
}
