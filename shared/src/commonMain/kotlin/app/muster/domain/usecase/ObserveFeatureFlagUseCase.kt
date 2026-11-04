package app.muster.domain.usecase

import app.muster.domain.model.FeatureFlag
import app.muster.domain.repository.FeatureFlagRepository
import kotlinx.coroutines.flow.Flow

class ObserveFeatureFlagUseCase(private val featureFlags: FeatureFlagRepository) {
    operator fun invoke(flag: FeatureFlag): Flow<Boolean> = featureFlags.observe(flag)
}
