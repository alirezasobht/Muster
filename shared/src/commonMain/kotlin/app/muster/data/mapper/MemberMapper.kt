package app.muster.data.mapper

import app.muster.data.dto.GroupInvitationRowDto
import app.muster.data.dto.GroupMemberDto
import app.muster.domain.model.Member
import app.muster.domain.model.PendingInvitation

internal fun GroupMemberDto.toMember() = Member(
    profileId = profileId,
    name = profiles.name.orEmpty(),
    role = role.toGroupRole()
)

internal fun GroupInvitationRowDto.toPendingInvitation() = PendingInvitation(
    id = id,
    email = email
)
