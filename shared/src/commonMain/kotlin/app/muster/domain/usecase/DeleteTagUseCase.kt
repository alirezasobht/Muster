package app.muster.domain.usecase

import app.muster.domain.repository.MemberRepository

class DeleteTagUseCase(private val members: MemberRepository) {
    suspend operator fun invoke(
        groupId: String,
        tagId: String
    ) = members.deleteTag(groupId, tagId)
}
