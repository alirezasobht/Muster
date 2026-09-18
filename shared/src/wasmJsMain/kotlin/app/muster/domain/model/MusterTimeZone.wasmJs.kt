package app.muster.domain.model

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
@JsModule("@js-joda/timezone")
private external object JsJodaTimeZoneModule

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
internal actual fun loadTimeZoneDatabase() {
    @Suppress("unused") val forceImport = JsJodaTimeZoneModule
}
