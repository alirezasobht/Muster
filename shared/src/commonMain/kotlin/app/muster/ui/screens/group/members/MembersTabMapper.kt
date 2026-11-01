package app.muster.ui.screens.group.members

import app.muster.domain.model.GroupRole
import app.muster.domain.model.Member
import app.muster.domain.model.MemberListing
import app.muster.domain.model.PendingInvitation

internal fun MemberListing.toSuccessState(
    myProfileId: String,
    isAdmin: Boolean
): MembersTabUiState.Success {
    val untagged = if (isAdmin) members.filter { it.tagId.isNullOrEmpty() } else members
    // Pending rows are readable by every member (group_invitations_select),
    // but visible to admins only
    return MembersTabUiState.Success(
        tags = if (isAdmin) buildTagSections(this, myProfileId, isAdmin) else emptyList(),
        members = buildMemberRows(untagged, myProfileId, isAdmin),
        pendings = if (isAdmin) buildPendingRows(pendingInvitations) else emptyList(),
        canAddMembers = isAdmin,
        canManageTags = isAdmin
    )
}

private fun buildTagSections(
    listing: MemberListing,
    myProfileId: String,
    isAdmin: Boolean
): List<TagSection> = listing.memberTags.mapIndexed { index, tag ->
    TagSection(
        id = tag.id,
        name = tag.name,
        rows = buildMemberRows(
            members = listing.members.filter { it.tagId == tag.id },
            myProfileId = myProfileId,
            isAdmin = isAdmin
        ),
        reorder = Reorder(
            canMoveUp = index != 0,
            canMoveDown = index != listing.memberTags.size - 1
        )
    )
}

private fun buildMemberRows(
    members: List<Member>,
    myProfileId: String,
    isAdmin: Boolean
): List<MemberRow> = sortRows(
    members.map { member ->
        val isSelf = member.profileId == myProfileId
        MemberRow(
            id = member.profileId,
            displayName = member.name,
            status = if (member.role == GroupRole.Admin) MemberStatus.Admin else MemberStatus.Member,
            isSelf = isSelf,
            tagId = member.tagId,
            canMoveToTag = isAdmin,
            canPromote = isAdmin && !isSelf && member.role == GroupRole.Member,
            canDemote = isAdmin && !isSelf && member.role == GroupRole.Admin,
            canRemove = isAdmin && !isSelf
        )
    }
)

private fun buildPendingRows(pendingInvitations: List<PendingInvitation>): List<MemberRow> = pendingInvitations.map { invitation ->
    MemberRow(
        id = invitation.id,
        displayName = invitation.email,
        status = MemberStatus.Pending,
        tagId = null,
        canRevokeInvitation = true,
        canResendInvitation = true
    )
}.sortedBy { it.displayName.lowercase() }

// Self first, then admins A-Z, then members A-Z.
internal fun sortRows(memberRows: List<MemberRow>): List<MemberRow> {
    val (selfRows, otherRows) = memberRows.partition { it.isSelf }
    val admins = otherRows.filter { it.status == MemberStatus.Admin }
        .sortedBy { it.displayName.lowercase() }
    val members = otherRows.filter { it.status == MemberStatus.Member }
        .sortedBy { it.displayName.lowercase() }
    return selfRows + admins + members
}

internal fun MembersTabUiState.Success.withMemberMoved(row: MemberRow, tagId: String?): MembersTabUiState.Success {
    val moved = row.copy(tagId = tagId)
    return copy(
        tags = tags.map { section ->
            val rows = section.rows.filter { it.id != row.id }
            section.copy(rows = if (section.id == tagId) sortRows(rows + moved) else rows)
        },
        members = members.filter { it.id != row.id }.let { rest ->
            if (tagId.isNullOrEmpty()) sortRows(rest + moved) else rest
        },
        movingProfileId = row.id,
        actionTargetId = row.id,
        actionError = null,
        failedActionId = null,
        moveError = null
    )
}
