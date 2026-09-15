package app.muster.domain.usecase

import app.muster.domain.repository.MemberRepository

class RevokeInvitationUseCase(private val members: MemberRepository) {
    suspend operator fun invoke(groupId: String, invitationId: String) = members.revokeInvitation(groupId, invitationId)
}
