package app.muster.domain.usecase

import app.muster.domain.model.Group
import app.muster.domain.repository.GroupRepository

class UpdateGroupUseCase(private val groups: GroupRepository) {
    suspend operator fun invoke(
        groupId: String,
        name: String
    ): Group = groups.updateGroup(groupId, name.trim())
}
