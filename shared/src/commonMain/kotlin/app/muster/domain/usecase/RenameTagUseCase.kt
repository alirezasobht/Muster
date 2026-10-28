package app.muster.domain.usecase

import app.muster.domain.repository.MemberRepository

class RenameTagUseCase(private val members: MemberRepository) {
    suspend operator fun invoke(
        groupId: String,
        tagId: String,
        name: String
    ) = members.renameTag(groupId, tagId, name)
}
