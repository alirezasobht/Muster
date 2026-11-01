package app.muster.data.mapper

import app.muster.data.dto.GroupInvitationEntryDto
import app.muster.data.dto.GroupMemberEntryDto
import app.muster.data.dto.GroupMemberTagEntryDto
import app.muster.data.dto.GroupMembersDto
import app.muster.domain.model.Member
import app.muster.domain.model.MemberListing
import app.muster.domain.model.MemberTag
import app.muster.domain.model.PendingInvitation

internal fun GroupMemberEntryDto.toMember() = Member(
    profileId = profileId,
    name = name.orEmpty(),
    role = role.toGroupRole(),
    tagId = tagId
)

internal fun GroupInvitationEntryDto.toPendingInvitation() = PendingInvitation(
    id = invitationId,
    email = email
)

internal fun GroupMemberTagEntryDto.toTag() = MemberTag(
    id = id,
    name = name
)

internal fun GroupMembersDto.toMemberListing() = MemberListing(
    members = members.map { it.toMember() },
    pendingInvitations = pending.map { it.toPendingInvitation() },
    memberTags = tags.map { it.toTag() }
)
