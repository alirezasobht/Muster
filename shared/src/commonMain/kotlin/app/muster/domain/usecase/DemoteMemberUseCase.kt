package app.muster.domain.usecase

import app.muster.domain.repository.MemberRepository

class DemoteMemberUseCase(private val members: MemberRepository) {
    suspend operator fun invoke(
        groupId: String,
        profileId: String
    ) = members.demote(groupId, profileId)
}
