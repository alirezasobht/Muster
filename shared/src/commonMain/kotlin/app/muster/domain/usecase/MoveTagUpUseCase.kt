package app.muster.domain.usecase

import app.muster.domain.repository.MemberRepository

class MoveTagUpUseCase(private val members: MemberRepository) {
    suspend operator fun invoke(
        groupId: String,
        currentTagIds: List<String>,
        tagIdToMoveUp: String
    ) {
        val index = currentTagIds.indexOf(tagIdToMoveUp)
        if (index <= 0) return
        val reordered = currentTagIds.toMutableList().apply {
            this[index] = this[index - 1]
            this[index - 1] = tagIdToMoveUp
        }
        members.setTagOrder(groupId, reordered)
    }
}
