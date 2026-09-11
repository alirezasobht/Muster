package app.muster.domain.repository

import app.muster.domain.model.Profile

// Implementations must throw DomainError only; the UI catches nothing else.
interface ProfileRepository {

    suspend fun getMyProfile(): Profile

    suspend fun updateName(name: String): Profile
}
