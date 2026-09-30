package app.muster.domain.usecase

import app.muster.domain.repository.GroupRepository

class ArchiveGroupUseCase(private val groups: GroupRepository) {
    suspend operator fun invoke(groupId: String) = groups.archive(groupId)
}
