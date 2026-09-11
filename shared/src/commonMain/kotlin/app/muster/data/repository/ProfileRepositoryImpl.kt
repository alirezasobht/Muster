package app.muster.data.repository

import app.muster.data.dto.ProfileDto
import app.muster.data.dto.ProfileNameUpdateDto
import app.muster.data.mapper.mapErrors
import app.muster.data.mapper.toProfile
import app.muster.domain.error.DomainError
import app.muster.domain.model.Profile
import app.muster.domain.repository.ProfileRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from

internal class ProfileRepositoryImpl(private val client: SupabaseClient) : ProfileRepository {

    override suspend fun getMyProfile(): Profile = mapErrors {
        client.from(TABLE)
            .select { filter { eq("id", myId()) } }
            .decodeSingle<ProfileDto>()
            .toProfile()
    }

    override suspend fun updateName(name: String): Profile = mapErrors {
        client.from(TABLE)
            .update(ProfileNameUpdateDto(name)) {
                select()
                filter { eq("id", myId()) }
            }
            .decodeSingle<ProfileDto>()
            .toProfile()
    }

    // RLS also returns co-members' profiles, so the id filter is required.
    private fun myId(): String =
        client.auth.currentUserOrNull()?.id ?: throw DomainError.NotSignedIn()

    private companion object {
        const val TABLE = "profiles"
    }
}
