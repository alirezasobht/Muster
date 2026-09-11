package app.muster.domain.usecase

import app.muster.domain.model.Profile
import app.muster.domain.repository.ProfileRepository

class UpdateNameUseCase(private val profiles: ProfileRepository) {
    // Trim only. Don't add a blank check here: the database CHECK is the rule.
    suspend operator fun invoke(name: String): Profile = profiles.updateName(name.trim())
}
