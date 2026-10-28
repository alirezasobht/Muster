package app.muster.domain.usecase

import app.muster.domain.model.TagLayoutEntry
import app.muster.domain.repository.MemberRepository

class SetTagLayoutUseCase(private val members: MemberRepository) {
    suspend operator fun invoke(
        groupId: String,
        layout: List<TagLayoutEntry>
    ) = members.setTagLayout(groupId, layout)
}
