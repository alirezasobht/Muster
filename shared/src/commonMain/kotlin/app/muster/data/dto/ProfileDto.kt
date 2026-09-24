package app.muster.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ProfileDto(
    val id: String,
    val name: String? = null,
    val email: String,
    @SerialName("can_create_groups") val canCreateGroups: Boolean = false
)

@Serializable
internal data class SetProfileNameDto(
    val name: String
)
