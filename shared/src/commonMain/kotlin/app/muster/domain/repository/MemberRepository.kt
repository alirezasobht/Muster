package app.muster.domain.repository

import app.muster.domain.model.MemberListing
import app.muster.domain.model.TagLayoutEntry

// Implementations must throw DomainError only; the UI catches nothing else.
interface MemberRepository {

    suspend fun listMembers(groupId: String): MemberListing

    suspend fun inviteByEmail(
        groupId: String,
        email: String
    )

    suspend fun promote(
        groupId: String,
        profileId: String
    )

    suspend fun demote(
        groupId: String,
        profileId: String
    )

    suspend fun remove(
        groupId: String,
        profileId: String
    )

    suspend fun revokeInvitation(
        groupId: String,
        invitationId: String
    )

    suspend fun resendInvitation(
        groupId: String,
        invitationId: String
    )

    suspend fun createTag(
        groupId: String,
        name: String,
        memberIds: List<String> = emptyList()
    )

    suspend fun renameTag(
        groupId: String,
        tagId: String,
        name: String
    )

    suspend fun deleteTag(
        groupId: String,
        tagId: String
    )

    // Replaces the members of every listed tag whole; tags left out keep theirs.
    suspend fun setTagLayout(
        groupId: String,
        layout: List<TagLayoutEntry>
    )

    suspend fun setTagOrder(
        groupId: String,
        tagIds: List<String>
    )

    // Not wired to any screen yet: Leave group belongs in the Group app bar
    // overflow (SCREENS.md), which is still a stub.
    suspend fun leave(groupId: String)
}
