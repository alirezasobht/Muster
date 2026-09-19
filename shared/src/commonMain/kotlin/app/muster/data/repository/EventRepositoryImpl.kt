package app.muster.data.repository

import app.muster.data.dto.EventDto
import app.muster.data.dto.EventInsertDto
import app.muster.data.dto.EventInvitationGroupIdDto
import app.muster.data.dto.EventInvitationRowDto
import app.muster.data.dto.EventRsvpUpdateDto
import app.muster.data.dto.EventStandbyRowDto
import app.muster.data.dto.ProfileNameRowDto
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
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlin.time.Clock
import kotlin.time.Instant

internal class EventRepositoryImpl(
    private val client: SupabaseClient,
    private val dataChanges: DataChanges
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

    override suspend fun createEvent(
        groupId: String,
        title: String,
        startsAt: Instant,
        location: String?,
        capacity: Int
    ): Event = mapErrors {
        client.from(EVENTS_TABLE)
            .insert(
                EventInsertDto(
                    groupId = groupId,
                    title = title,
                    startsAt = startsAt.toString(),
                    location = location,
                    capacity = capacity,
                    createdBy = myId()
                )
            ) { select() }
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
        // group_id is denormalised onto event_invitations (SCHEMA.md), so
        // reading it back off this same update avoids a second round trip
        // just to know who else to notify.
        val groupId = client.from(EVENT_INVITATIONS_TABLE)
            .update(EventRsvpUpdateDto(status = status.toDbValue())) {
                filter {
                    eq("event_id", eventId)
                    eq("profile_id", profileId)
                }
                select()
            }
            .decodeSingle<EventInvitationGroupIdDto>()
            .groupId

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
        // Reordering can promote — the trigger can move someone from the
        // queue straight into the roster, so the roster listener needs to
        // hear about this too, not just whoever renders the queue itself.
        dataChanges.notify(DataChange.Roster(eventId))
    }

    private fun myId(): String =
        client.auth.currentUserOrNull()?.id ?: throw DomainError.NotSignedIn()

    private companion object {
        const val EVENTS_TABLE = "events"
        const val EVENT_INVITATIONS_TABLE = "event_invitations"
        const val EVENT_STANDBY_TABLE = "event_standby"
        const val PROFILES_TABLE = "profiles"
        const val IN_STATUS = "in"
        const val PENDING_STATUS = "pending"
        const val SET_STANDBY_ORDER_FUNCTION = "set_standby_order"
    }
}
