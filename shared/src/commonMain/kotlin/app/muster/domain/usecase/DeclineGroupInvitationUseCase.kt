package app.muster.domain.usecase

import app.muster.domain.repository.GroupRepository

class DeclineGroupInvitationUseCase(private val groups: GroupRepository) {
    suspend operator fun invoke(invitationId: String) = groups.declineInvitation(invitationId)
}
