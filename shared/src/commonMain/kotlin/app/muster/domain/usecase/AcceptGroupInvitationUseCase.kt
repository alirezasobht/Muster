package app.muster.domain.usecase

import app.muster.domain.repository.GroupRepository

class AcceptGroupInvitationUseCase(private val groups: GroupRepository) {
    suspend operator fun invoke(invitationId: String) = groups.acceptInvitation(invitationId)
}
