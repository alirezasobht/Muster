package app.muster.domain.usecase

import app.muster.domain.model.Group
import app.muster.domain.repository.GroupRepository

class CreateGroupUseCase(private val groups: GroupRepository) {
    // Trim only. Don't add a blank check here: the database CHECK is the rule.
    suspend operator fun invoke(name: String): Group = groups.createGroup(name.trim())
}
