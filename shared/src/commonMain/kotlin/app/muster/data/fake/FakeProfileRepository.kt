package app.muster.data.fake

import app.muster.domain.error.DomainError
import app.muster.domain.model.Profile
import app.muster.domain.repository.ProfileRepository
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

class FakeProfileRepository(
    name: String? = null,
    canCreateGroups: Boolean = false,
    var getError: DomainError? = null,
    var updateError: DomainError? = null,
    private val latency: Long = FAKE_LATENCY_MS
) : ProfileRepository {

    var profile = Profile(
        id = FAKE_USER_ID,
        name = name,
        email = FAKE_EMAIL,
        canCreateGroups = canCreateGroups
    )
        private set

    override suspend fun getMyProfile(): Profile {
        delay(latency.milliseconds)
        getError?.let { throw it }
        return profile
    }

    override suspend fun updateName(name: String): Profile {
        delay(latency.milliseconds)
        updateError?.let { throw it }
        // Stands in for the database CHECK, which the real path relies on.
        if (name.isBlank()) throw DomainError.InvalidName()
        profile = profile.copy(name = name)
        return profile
    }
}
