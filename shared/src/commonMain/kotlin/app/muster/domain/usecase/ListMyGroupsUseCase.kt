package app.muster.domain.usecase

import app.muster.domain.model.Group
import app.muster.domain.repository.GroupRepository

class ListMyGroupsUseCase(private val groups: GroupRepository) {
    suspend operator fun invoke(): List<Group> = groups.getMyGroups()
}
