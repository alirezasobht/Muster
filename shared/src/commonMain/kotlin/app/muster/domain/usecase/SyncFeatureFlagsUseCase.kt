package app.muster.domain.usecase

import app.muster.domain.repository.FeatureFlagRepository

class SyncFeatureFlagsUseCase(private val featureFlags: FeatureFlagRepository) {
    suspend operator fun invoke() = featureFlags.sync()
}
