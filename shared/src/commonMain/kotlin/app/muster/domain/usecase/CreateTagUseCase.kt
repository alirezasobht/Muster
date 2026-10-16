package app.muster.domain.usecase

import app.muster.domain.repository.MemberRepository

class CreateTagUseCase(private val members: MemberRepository) {
    suspend operator fun invoke(
        groupId: String,
        name: String,
        memberIds: List<String> = emptyList()
    ) = members.createTag(groupId, name, memberIds)
}
