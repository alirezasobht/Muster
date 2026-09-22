package app.muster.domain.usecase

import app.muster.domain.model.InviteeCandidates
import app.muster.domain.model.RsvpStatus
import app.muster.domain.repository.EventRepository
import app.muster.domain.repository.MemberRepository

class GetInviteeCandidatesUseCase(
    private val events: EventRepository,
    private val members: MemberRepository
) {
    suspend operator fun invoke(eventId: String, groupId: String): InviteeCandidates {
        val detail = events.getEvent(eventId)
        val onEvent = (detail.roster.map { it.profileId } + detail.standby.map { it.profileId }).toSet()
        val occupied = detail.roster.count { it.status == RsvpStatus.In || it.status == RsvpStatus.Pending }
        val members = members.listMembers(groupId).members.filter { it.profileId !in onEvent }
        return InviteeCandidates(
            members = members,
            capacity = detail.event.capacity,
            freeSlots = detail.event.capacity - occupied,
            queueLength = detail.standby.size
        )
    }
}
