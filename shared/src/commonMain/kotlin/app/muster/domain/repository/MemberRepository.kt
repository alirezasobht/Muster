package app.muster.domain.repository

import app.muster.domain.model.MemberListing

// Implementations must throw DomainError only; the UI catches nothing else.
interface MemberRepository {

    suspend fun listMembers(groupId: String): MemberListing

    suspend fun promote(groupId: String, profileId: String)

    suspend fun demote(groupId: String, profileId: String)

    suspend fun remove(groupId: String, profileId: String)

    suspend fun revokeInvitation(groupId: String, invitationId: String)

    // Not wired to any screen yet: Leave group belongs in the Group app bar
    // overflow (SCREENS.md), which is still a stub.
    suspend fun leave(groupId: String)
}
