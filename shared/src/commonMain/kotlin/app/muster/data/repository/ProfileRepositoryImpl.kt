package app.muster.data.repository

import app.muster.data.dto.ProfileDto
import app.muster.data.dto.SetProfileNameDto
import app.muster.data.mapper.mapErrors
import app.muster.data.mapper.toProfile
import app.muster.domain.error.DomainError
import app.muster.domain.model.Profile
import app.muster.domain.repository.ProfileRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc

internal class ProfileRepositoryImpl(private val client: SupabaseClient) : ProfileRepository {

    override suspend fun getMyProfile(): Profile = mapErrors {
        client.from(TABLE)
            .select { filter { eq("id", myId()) } }
            .decodeSingle<ProfileDto>()
            .toProfile()
    }

    override suspend fun updateName(name: String): Profile = mapErrors {
        client.postgrest.rpc(SET_PROFILE_NAME_FUNCTION, SetProfileNameDto(name))
            .decodeSingle<ProfileDto>()
            .toProfile()
    }

    // RLS also returns co-members' profiles, so the id filter is required.
    private fun myId(): String = client.auth.currentUserOrNull()?.id ?: throw DomainError.NotSignedIn()

    private companion object {
        const val TABLE = "profiles"
        const val SET_PROFILE_NAME_FUNCTION = "set_profile_name"
    }
}
