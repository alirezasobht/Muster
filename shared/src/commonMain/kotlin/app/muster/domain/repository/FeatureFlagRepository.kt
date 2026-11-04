package app.muster.domain.repository

import app.muster.domain.model.FeatureFlag
import kotlinx.coroutines.flow.Flow

// Implementations must throw DomainError only; the UI catches nothing else.
interface FeatureFlagRepository {

    fun observe(flag: FeatureFlag): Flow<Boolean>

    suspend fun sync()
}
