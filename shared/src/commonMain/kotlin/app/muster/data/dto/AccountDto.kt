package app.muster.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class DeleteAccountDto(
    val force: Boolean
)

// delete_account() RPC result: a group the caller is the only admin of.
@Serializable
internal data class SoleAdminGroupDto(
    @SerialName("group_id") val groupId: String,
    val name: String
)
