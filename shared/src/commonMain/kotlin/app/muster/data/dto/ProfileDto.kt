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

// Only `name` is granted for update. Sending any other column is refused
// by the grant, so this must never grow a second field.
@Serializable
internal data class ProfileNameUpdateDto(
    val name: String
)
