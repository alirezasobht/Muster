package app.muster.domain.usecase

import app.muster.domain.repository.MemberRepository

class MoveTagDownUseCase(private val members: MemberRepository) {
    suspend operator fun invoke(
        groupId: String,
        currentTagIds: List<String>,
        tagIdToMoveDown: String
    ) {
        val index = currentTagIds.indexOf(tagIdToMoveDown)
        if (index < 0 || index == currentTagIds.lastIndex) return
        val reordered = currentTagIds.toMutableList().apply {
            this[index] = this[index + 1]
            this[index + 1] = tagIdToMoveDown
        }
        members.setTagOrder(groupId, reordered)
    }
}
