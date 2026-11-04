package app.muster.data.local

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

// All flags are saved as one JSON string, so a flag deleted on the server is deleted here too.
// Web's StorageSettings doesn't implement ObservableSettings (it gives a Flow of changes), so the flags are kept in a StateFlow that replaceAll() updates.
internal class FeatureFlagLocalSource(private val settings: Settings) {

    private val state = MutableStateFlow(read())

    fun observe(flag: String): Flow<Boolean> = state.map { it[flag] ?: false }.distinctUntilChanged()

    // runCatching, not catch (e: Exception): on Wasm a localStorage error is a JsException, which is not an Exception.
    suspend fun replaceAll(flags: Map<String, Boolean>) {
        runCatching { settings.putString(KEY, Json.encodeToString(serializer, flags)) }
        // Saving fails only on web, when the browser blocks storage (some private modes). Set the state anyway, so flags still work until the app closes.
        state.value = flags
    }

    private fun read(): Map<String, Boolean> = runCatching {
        settings.getStringOrNull(KEY)?.let { Json.decodeFromString(serializer, it) }
    }.getOrNull() ?: emptyMap()

    private companion object {
        // Starts with "muster." because on web all keys share one localStorage.
        const val KEY = "muster.feature_flags"
        val serializer = MapSerializer(String.serializer(), Boolean.serializer())
    }
}
