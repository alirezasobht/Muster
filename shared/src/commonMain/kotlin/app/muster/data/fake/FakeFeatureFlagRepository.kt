package app.muster.data.fake

import app.muster.domain.error.DomainError
import app.muster.domain.model.FeatureFlag
import app.muster.domain.repository.FeatureFlagRepository
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class FakeFeatureFlagRepository(
    initial: Map<FeatureFlag, Boolean> = emptyMap(),
    // What sync() saves. It replaces all flags, like the real one.
    var remote: Map<FeatureFlag, Boolean> = initial,
    var syncError: DomainError? = null,
    private val latency: Long = FAKE_LATENCY_MS
) : FeatureFlagRepository {

    private val state = MutableStateFlow(initial)

    var syncCalls: Int = 0
        private set

    override fun observe(flag: FeatureFlag): Flow<Boolean> = state.map { it[flag] ?: false }.distinctUntilChanged()

    override suspend fun sync() {
        syncCalls++
        delay(latency.milliseconds)
        syncError?.let { throw it }
        state.value = remote
    }
}
