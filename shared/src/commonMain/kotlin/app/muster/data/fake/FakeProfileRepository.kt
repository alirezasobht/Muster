package app.muster.data.fake

import app.muster.domain.error.DomainError
import app.muster.domain.model.Profile
import app.muster.domain.repository.ProfileRepository
import kotlinx.coroutines.delay

class FakeProfileRepository(name: String? = null) : ProfileRepository {

    private var profile = Profile(
        id = FAKE_USER_ID,
        name = name,
        email = FAKE_EMAIL,
        canCreateGroups = false
    )

    override suspend fun getMyProfile(): Profile {
        delay(FAKE_LATENCY_MS)
        return profile
    }

    override suspend fun updateName(name: String): Profile {
        delay(FAKE_LATENCY_MS)
        // Stands in for the database CHECK, which the real path relies on.
        if (name.isBlank()) throw DomainError.InvalidName()
        profile = profile.copy(name = name)
        return profile
    }
}
