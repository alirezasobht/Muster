package app.muster.data.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class FeatureFlagDto(
    val flag: String,
    val enabled: Boolean
)

@Serializable
internal data class GetFeatureFlagsDto(
    val platform: String,
    val version: String
)
