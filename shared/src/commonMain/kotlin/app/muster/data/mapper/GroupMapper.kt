package app.muster.data.mapper

import app.muster.data.dto.GroupDto
import app.muster.data.dto.GroupMemberRoleDto
import app.muster.data.dto.GroupMembershipDto
import app.muster.data.dto.PendingInvitationDto
import app.muster.domain.model.Group
import app.muster.domain.model.GroupInvitation
import app.muster.domain.model.GroupRole

internal fun GroupDto.toGroup() = Group(id = id, name = name)

internal fun GroupMembershipDto.toGroup() = groups.toGroup()

internal fun GroupMemberRoleDto.toGroupRole() = if (role == "admin") GroupRole.Admin else GroupRole.Member

internal fun PendingInvitationDto.toGroupInvitation() = GroupInvitation(
    id = invitationId,
    group = Group(id = groupId, name = groupName),
    invitedByName = inviterName
)
