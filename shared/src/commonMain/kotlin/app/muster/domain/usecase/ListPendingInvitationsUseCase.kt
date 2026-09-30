package app.muster.domain.usecase

import app.muster.domain.model.GroupInvitation
import app.muster.domain.repository.GroupRepository

class ListPendingInvitationsUseCase(private val groups: GroupRepository) {
    suspend operator fun invoke(): List<GroupInvitation> = groups.getPendingInvitations()
}
