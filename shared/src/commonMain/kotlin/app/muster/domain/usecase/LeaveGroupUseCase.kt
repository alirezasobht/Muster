package app.muster.domain.usecase

import app.muster.domain.repository.MemberRepository

class LeaveGroupUseCase(private val members: MemberRepository) {
    suspend operator fun invoke(groupId: String) = members.leave(groupId)
}
