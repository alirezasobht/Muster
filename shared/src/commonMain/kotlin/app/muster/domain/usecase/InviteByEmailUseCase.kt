package app.muster.domain.usecase

import app.muster.domain.repository.MemberRepository

class InviteByEmailUseCase(private val members: MemberRepository) {
    suspend operator fun invoke(groupId: String, email: String) = members.inviteByEmail(groupId, email.trim())
}
