package app.muster.data.mapper

import app.muster.data.dto.FeatureFlagDto

internal fun List<FeatureFlagDto>.toFlagMap(): Map<String, Boolean> = associate { it.flag to it.enabled }
