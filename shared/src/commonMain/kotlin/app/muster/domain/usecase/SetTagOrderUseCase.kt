package app.muster.domain.usecase

import app.muster.domain.repository.MemberRepository

class SetTagOrderUseCase(private val members: MemberRepository) {
    suspend operator fun invoke(
        groupId: String,
        tagIds: List<String>
    ) = members.setTagOrder(groupId, tagIds)
}
