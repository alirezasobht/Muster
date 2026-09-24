package app.muster.data.repository

import app.muster.data.dto.CreateEventDto
import app.muster.data.dto.EventDto
import app.muster.data.dto.EventInvitationGroupIdDto
import app.muster.data.dto.EventInvitationRowDto
import app.muster.data.dto.EventStandbyRowDto
import app.muster.data.dto.ListUpcomingEventsDto
import app.muster.data.dto.ProfileNameRowDto
import app.muster.data.dto.SetEventRsvpDto
import app.muster.data.dto.UpcomingEventDto
import app.muster.data.mapper.mapErrors
import app.muster.data.mapper.toDbValue
import app.muster.data.mapper.toEvent
import app.muster.data.mapper.toRosterEntry
import app.muster.data.mapper.toRsvpStatus
import app.muster.data.mapper.toStandbyEntry
import app.muster.domain.error.DomainError
import app.muster.domain.event.DataChange
import app.muster.domain.event.DataChanges
import app.muster.domain.model.Event
import app.muster.domain.model.EventDetail
import app.muster.domain.model.RsvpStatus
import app.muster.domain.repository.EventRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
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
        val eventDto = client.from(EVENTS_TABLE)
            .select(Columns.raw("id,group_id,title,starts_at,location,capacity")) {
                filter { eq("id", eventId) }
            }
            .decodeSingle<EventDto>()

        val invitations = client.from(EVENT_INVITATIONS_TABLE)
            .select(Columns.raw("event_id,profile_id,status")) {
                filter { eq("event_id", eventId) }
            }
            .decodeList<EventInvitationRowDto>()

        // Ordered server-side; row order is queue order (SCHEMA.md — no
        // position column comes back, see EventStandbyRowDto).
        val standbyRows = client.from(EVENT_STANDBY_TABLE)
            .select(Columns.raw("profile_id")) {
                filter { eq("event_id", eventId) }
                order("position", Order.ASCENDING)
            }
            .decodeList<EventStandbyRowDto>()

        val allProfileIds = (invitations.map { it.profileId } + standbyRows.map { it.profileId }).distinct()
        val namesByProfileId = if (allProfileIds.isEmpty()) {
            emptyMap()
        } else {
            client.from(PROFILES_TABLE)
                .select(Columns.raw("id,name")) {
                    filter { isIn("id", allProfileIds) }
                }
                .decodeList<ProfileNameRowDto>()
                .associate { it.id to it.name.orEmpty() }
        }

        val roster = invitations.map { invitation ->
            invitation.toRosterEntry(name = namesByProfileId[invitation.profileId].orEmpty())
        }
        val standby = standbyRows.map { row ->
            row.toStandbyEntry(name = namesByProfileId[row.profileId].orEmpty())
        }

        val myId = myId()
        val event = eventDto.toEvent(
            inCount = roster.count { it.status == RsvpStatus.In },
            pendingCount = roster.count { it.status == RsvpStatus.Pending },
            myStatus = roster.firstOrNull { it.profileId == myId }?.status
        )

        EventDetail(event = event, roster = roster, standby = standby)
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

    private fun myId(): String =
        client.auth.currentUserOrNull()?.id ?: throw DomainError.NotSignedIn()

    private companion object {
        const val EVENTS_TABLE = "events"
        const val EVENT_INVITATIONS_TABLE = "event_invitations"
        const val EVENT_STANDBY_TABLE = "event_standby"
        const val PROFILES_TABLE = "profiles"
        const val LIST_UPCOMING_EVENTS_FUNCTION = "list_upcoming_events"
        const val CREATE_EVENT_FUNCTION = "create_event"
        const val SET_EVENT_RSVP_FUNCTION = "set_event_rsvp"
        const val SET_STANDBY_ORDER_FUNCTION = "set_standby_order"
        const val DISINVITE_PLAYER_FUNCTION = "disinvite_player"
        const val ADD_PLAYERS_TO_EVENT_FUNCTION = "add_players_to_event"
    }
}
