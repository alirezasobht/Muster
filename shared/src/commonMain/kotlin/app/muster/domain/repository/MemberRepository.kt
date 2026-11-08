package app.muster.domain.repository

import app.muster.domain.model.MemberListing

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
        memberId: String? = null
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

    // A null tagId removes the member's tag.
    suspend fun setMemberTag(
        groupId: String,
        profileId: String,
        tagId: String?
    )

    suspend fun setTagOrder(
        groupId: String,
        tagIds: List<String>
    )

    // Not wired to any screen yet: Leave group belongs in the Group app bar
    // overflow (SCREENS.md), which is still a stub.
    suspend fun leave(groupId: String)
}
