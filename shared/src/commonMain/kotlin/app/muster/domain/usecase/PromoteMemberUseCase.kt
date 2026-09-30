package app.muster.domain.usecase

import app.muster.domain.repository.MemberRepository

class PromoteMemberUseCase(private val members: MemberRepository) {
    suspend operator fun invoke(
        groupId: String,
        profileId: String
    ) = members.promote(groupId, profileId)
}
