package app.muster.domain.usecase

import app.muster.domain.repository.MemberRepository

class CreateTagUseCase(private val members: MemberRepository) {
    suspend operator fun invoke(
        groupId: String,
        name: String,
        memberId: String?
    ) = members.createTag(groupId, name, memberId)
}
