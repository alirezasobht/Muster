package app.muster.domain.usecase

import app.muster.domain.model.GroupRole
import app.muster.domain.repository.GroupRepository

class GetMyGroupRoleUseCase(private val groups: GroupRepository) {
    suspend operator fun invoke(groupId: String): GroupRole = groups.getMyRole(groupId)
}
