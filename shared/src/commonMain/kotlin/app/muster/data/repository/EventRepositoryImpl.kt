package app.muster.data.repository

import app.muster.data.dto.EventDto
import app.muster.data.dto.EventInvitationRowDto
import app.muster.data.mapper.mapErrors
import app.muster.data.mapper.toEvent
import app.muster.data.mapper.toRsvpStatus
import app.muster.domain.error.DomainError
import app.muster.domain.model.Event
import app.muster.domain.repository.EventRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlin.time.Clock

internal class EventRepositoryImpl(
    private val client: SupabaseClient
) : EventRepository {

    override suspend fun listUpcomingEvents(groupId: String): List<Event> = mapErrors {
        val events = client.from(EVENTS_TABLE)
            .select(Columns.raw("id,group_id,title,starts_at,location,capacity")) {
                filter {
                    eq("group_id", groupId)
                    gt("starts_at", Clock.System.now().toString())
                }
                order("starts_at", Order.ASCENDING)
            }
            .decodeList<EventDto>()

        if (events.isEmpty()) return@mapErrors emptyList()

        val invitationsByEvent = client.from(EVENT_INVITATIONS_TABLE)
            .select(Columns.raw("event_id,profile_id,status")) {
                filter { isIn("event_id", events.map { it.id }) }
            }
            .decodeList<EventInvitationRowDto>()
            .groupBy { it.eventId }

        val myId = myId()
        events.map { event ->
            val rows = invitationsByEvent[event.id].orEmpty()
            event.toEvent(
                inCount = rows.count { it.status == IN_STATUS },
                pendingCount = rows.count { it.status == PENDING_STATUS },
                myStatus = rows.firstOrNull { it.profileId == myId }?.status?.toRsvpStatus()
            )
        }
    }

    private fun myId(): String =
        client.auth.currentUserOrNull()?.id ?: throw DomainError.NotSignedIn()

    private companion object {
        const val EVENTS_TABLE = "events"
        const val EVENT_INVITATIONS_TABLE = "event_invitations"
        const val IN_STATUS = "in"
        const val PENDING_STATUS = "pending"
    }
}
