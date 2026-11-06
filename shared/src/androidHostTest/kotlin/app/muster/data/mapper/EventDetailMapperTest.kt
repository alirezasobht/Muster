package app.muster.data.mapper

import app.muster.data.dto.EventDetailDto
import app.muster.domain.model.RsvpStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant
import kotlinx.serialization.json.Json

class EventDetailMapperTest {

    @Test
    fun `RPC payload preserves RSVP counts names and queue order`() {
        val dto = Json.decodeFromString<List<EventDetailDto>>(
            """
            [{
                "event": {
                    "id": "event", "group_id": "group", "title": "Training",
                    "starts_at": "2026-09-25T18:00:00+00:00", "location": null, "capacity": 2
                },
                "roster": [
                    {"profile_id": "a", "name": "Alex", "status": "in"},
                    {"profile_id": "b", "name": null, "status": "pending"},
                    {"profile_id": "me", "name": "Sam", "status": "out"}
                ],
                "standby": [
                    {"profile_id": "z", "name": "Zoe"},
                    {"profile_id": "c", "name": null}
                ],
                "my_status": "out"
            }]
            """.trimIndent()
        ).single()

        val detail = dto.toEventDetail()

        assertEquals("event", detail.event.id)
        assertEquals("group", detail.event.groupId)
        assertEquals(Instant.parse("2026-09-25T18:00:00Z"), detail.event.startsAt)
        assertEquals(1, detail.event.inCount)
        assertEquals(1, detail.event.pendingCount)
        assertEquals(RsvpStatus.Out, detail.event.myStatus)
        assertEquals(listOf("Alex", "", "Sam"), detail.roster.map { it.name })
        assertEquals(listOf("z", "c"), detail.standby.map { it.profileId })
        assertEquals(listOf("Zoe", ""), detail.standby.map { it.name })
        assertNull(detail.event.location)
    }

    @Test
    fun `empty roster and queue have zero counts and no RSVP`() {
        val dto = Json.decodeFromString<List<EventDetailDto>>(
            """
            [{
                "event": {
                    "id": "event", "group_id": "group", "title": "Training",
                    "starts_at": "2026-09-25T18:00:00Z", "location": "Park", "capacity": 2
                },
                "roster": [], "standby": [], "my_status": null
            }]
            """.trimIndent()
        ).single()

        val detail = dto.toEventDetail()

        assertEquals(0, detail.event.inCount)
        assertEquals(0, detail.event.pendingCount)
        assertNull(detail.event.myStatus)
        assertNull(detail.event.lockedAt)
        assertNull(detail.lockedByName)
        assertEquals(emptyList(), detail.roster)
        assertEquals(emptyList(), detail.standby)
    }

    @Test
    fun `an unlocked event has no lock time or locker`() {
        val detail = detailWithLock(lockedAt = "null", lockedByName = "null").toEventDetail()

        assertNull(detail.event.lockedAt)
        assertNull(detail.lockedByName)
    }

    @Test
    fun `a locked event carries the lock time and the locker's name`() {
        val detail = detailWithLock(
            lockedAt = "\"2026-09-20T12:30:00.123456+00:00\"",
            lockedByName = "\"Alex\""
        ).toEventDetail()

        assertEquals(Instant.parse("2026-09-20T12:30:00.123456Z"), detail.event.lockedAt)
        assertEquals("Alex", detail.lockedByName)
    }

    // The locker deleted their account or is hidden by RLS: still locked,
    // just nameless.
    @Test
    fun `a locked event whose locker is gone has no name`() {
        val detail = detailWithLock(
            lockedAt = "\"2026-09-20T12:30:00+00:00\"",
            lockedByName = "null"
        ).toEventDetail()

        assertEquals(Instant.parse("2026-09-20T12:30:00Z"), detail.event.lockedAt)
        assertNull(detail.lockedByName)
    }

    @Test
    fun `a cancelled event carries the cancel time and the canceller's name`() {
        val detail = detailWithCancel(
            cancelledAt = "\"2026-09-21T08:00:00+00:00\"",
            cancelledByName = "\"Sam\""
        ).toEventDetail()

        assertEquals(Instant.parse("2026-09-21T08:00:00Z"), detail.event.cancelledAt)
        assertEquals("Sam", detail.cancelledByName)
    }

    @Test
    fun `a cancelled event whose canceller is gone has no name`() {
        val detail = detailWithCancel(
            cancelledAt = "\"2026-09-21T08:00:00+00:00\"",
            cancelledByName = "null"
        ).toEventDetail()

        assertEquals(Instant.parse("2026-09-21T08:00:00Z"), detail.event.cancelledAt)
        assertNull(detail.cancelledByName)
    }

    @Test
    fun `an event without a cancel has no cancel time or canceller`() {
        val detail = detailWithLock(lockedAt = "null", lockedByName = "null").toEventDetail()

        assertNull(detail.event.cancelledAt)
        assertNull(detail.cancelledByName)
    }

    private fun detailWithCancel(
        cancelledAt: String,
        cancelledByName: String
    ) = Json.decodeFromString<List<EventDetailDto>>(
        """
        [{
            "event": {
                "id": "event", "group_id": "group", "title": "Training",
                "starts_at": "2026-09-25T18:00:00Z", "location": "Park", "capacity": 2,
                "locked_at": null, "locked_by_name": null,
                "cancelled_at": $cancelledAt, "cancelled_by_name": $cancelledByName
            },
            "roster": [], "standby": [], "my_status": null
        }]
        """.trimIndent()
    ).single()

    private fun detailWithLock(
        lockedAt: String,
        lockedByName: String
    ) = Json.decodeFromString<List<EventDetailDto>>(
        """
        [{
            "event": {
                "id": "event", "group_id": "group", "title": "Training",
                "starts_at": "2026-09-25T18:00:00Z", "location": "Park", "capacity": 2,
                "locked_at": $lockedAt, "locked_by_name": $lockedByName
            },
            "roster": [], "standby": [], "my_status": null
        }]
        """.trimIndent()
    ).single()
}
