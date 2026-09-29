package app.muster.domain.model

@JsModule("@js-joda/timezone")
private external object JsJodaTimeZoneModule

internal actual fun loadTimeZoneDatabase() {
    @Suppress("unused")
    val forceImport = JsJodaTimeZoneModule
}
