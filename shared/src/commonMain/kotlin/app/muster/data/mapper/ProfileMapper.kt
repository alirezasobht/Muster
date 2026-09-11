package app.muster.data.mapper

import app.muster.data.dto.ProfileDto
import app.muster.domain.model.Profile

internal fun ProfileDto.toProfile() = Profile(
    id = id,
    name = name,
    email = email,
    canCreateGroups = canCreateGroups
)
