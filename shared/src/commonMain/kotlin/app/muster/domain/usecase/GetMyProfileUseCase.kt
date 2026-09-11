package app.muster.domain.usecase

import app.muster.domain.model.Profile
import app.muster.domain.repository.ProfileRepository

class GetMyProfileUseCase(private val profiles: ProfileRepository) {
    suspend operator fun invoke(): Profile = profiles.getMyProfile()
}
