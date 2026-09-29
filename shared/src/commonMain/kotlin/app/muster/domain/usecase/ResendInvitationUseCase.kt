package app.muster.domain.usecase

import app.muster.domain.repository.MemberRepository

class ResendInvitationUseCase(private val members: MemberRepository) {
    suspend operator fun invoke(
        groupId: String,
        invitationId: String
    ) = members.resendInvitation(groupId, invitationId)
}
