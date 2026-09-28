package app.muster.data.repository

import app.muster.data.dto.CreateEventDto
import app.muster.data.dto.EventDetailDto
import app.muster.data.dto.EventDto
import app.muster.data.dto.EventInvitationGroupIdDto
import app.muster.data.dto.GetEventDetailDto
import app.muster.data.dto.ListUpcomingEventsDto
import app.muster.data.dto.SetEventRsvpDto
import app.muster.data.dto.UpcomingEventDto
import app.muster.data.mapper.mapErrors
import app.muster.data.mapper.toDbValue
import app.muster.data.mapper.toEvent
import app.muster.data.mapper.toEventDetail
import app.muster.domain.event.DataChange
import app.muster.domain.event.DataChanges
import app.muster.domain.model.Event
import app.muster.domain.model.EventDetail
import app.muster.domain.model.MusterTimeZone
import app.muster.domain.model.RsvpStatus
import app.muster.domain.repository.EventRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlin.time.Instant

internal class EventRepositoryImpl(
    private val client: SupabaseClient,
    private val dataChanges: DataChanges
) : EventRepository {

    override suspend fun listUpcomingEvents(groupId: String): List<Event> = mapErrors {
        client.postgrest.rpc(LIST_UPCOMING_EVENTS_FUNCTION, ListUpcomingEventsDto(groupId))
            .decodeList<UpcomingEventDto>()
            .map { it.toEvent() }
    }

    override suspend fun createEvent(
        groupId: String,
        title: String,
        startsAt: Instant,
        location: String?,
        capacity: Int
    ): Event = mapErrors {
        client.postgrest.rpc(
            CREATE_EVENT_FUNCTION,
            CreateEventDto(
                groupId = groupId,
                title = title,
                startsAt = startsAt.toString(),
                location = location,
                capacity = capacity
            )
        )
            .decodeSingle<EventDto>()
            // A brand-new event has no invitations yet.
            .toEvent(inCount = 0, pendingCount = 0, myStatus = null)
            .also { dataChanges.notify(DataChange.Events(groupId)) }
    }

    override suspend fun getEvent(eventId: String): EventDetail = mapErrors {
        client.postgrest.rpc(GET_EVENT_DETAIL_FUNCTION, GetEventDetailDto(eventId))
            .decodeSingle<EventDetailDto>()
            .toEventDetail()
    }

    override suspend fun setRsvp(eventId: String, profileId: String, status: RsvpStatus): Unit = mapErrors {
        val groupId = client.postgrest.rpc(
            SET_EVENT_RSVP_FUNCTION,
            SetEventRsvpDto(eventId = eventId, profileId = profileId, status = status.toDbValue())
        )
            .decodeSingle<EventInvitationGroupIdDto>()
            .groupId

        dataChanges.notify(DataChange.Roster(eventId))
        dataChanges.notify(DataChange.Events(groupId))
    }

    override suspend fun disinvitePlayer(eventId: String, groupId: String, profileId: String): Unit = mapErrors {
        client.postgrest.rpc(
            DISINVITE_PLAYER_FUNCTION,
            buildJsonObject {
                put("event_id", eventId)
                put("profile_id", profileId)
            }
        )
        dataChanges.notify(DataChange.Roster(eventId))
        dataChanges.notify(DataChange.Events(groupId))
    }

    override suspend fun resendInvitation(eventId: String, profileId: String): Unit = mapErrors {
        client.postgrest.rpc(
            RESEND_EVENT_INVITATION_FUNCTION,
            buildJsonObject {
                put("event_id", eventId)
                put("profile_id", profileId)
                put("tz", MusterTimeZone.id)
            }
        )
    }

    override suspend fun reorderStandby(eventId: String, orderedProfileIds: List<String>): Unit = mapErrors {
        client.postgrest.rpc(
            SET_STANDBY_ORDER_FUNCTION,
            buildJsonObject {
                put("eid", eventId)
                putJsonArray("ordered_players") { orderedProfileIds.forEach { add(it) } }
            }
        )
        dataChanges.notify(DataChange.Roster(eventId))
    }

    override suspend fun addPlayers(eventId: String, groupId: String, profileIds: List<String>): Unit = mapErrors {
        client.postgrest.rpc(
            ADD_PLAYERS_TO_EVENT_FUNCTION,
            buildJsonObject {
                put("event_id", eventId)
                putJsonArray("profile_ids") { profileIds.forEach { add(it) } }
            }
        )
        dataChanges.notify(DataChange.Roster(eventId))
        dataChanges.notify(DataChange.Events(groupId))
    }

    private companion object {
        const val GET_EVENT_DETAIL_FUNCTION = "get_event_detail"
        const val LIST_UPCOMING_EVENTS_FUNCTION = "list_upcoming_events"
        const val CREATE_EVENT_FUNCTION = "create_event"
        const val SET_EVENT_RSVP_FUNCTION = "set_event_rsvp"
        const val SET_STANDBY_ORDER_FUNCTION = "set_standby_order"
        const val DISINVITE_PLAYER_FUNCTION = "disinvite_player"
        const val RESEND_EVENT_INVITATION_FUNCTION = "resend_event_invitation"
        const val ADD_PLAYERS_TO_EVENT_FUNCTION = "add_players_to_event"
    }
}
