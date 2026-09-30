package app.muster.domain.usecase

import app.muster.domain.model.Group
import app.muster.domain.repository.GroupRepository

class GetGroupUseCase(private val groups: GroupRepository) {
    suspend operator fun invoke(groupId: String): Group = groups.getGroup(groupId)
}
