package app.muster.data.mapper

import app.muster.data.dto.UpcomingEventDto
import app.muster.domain.model.RsvpStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant
import kotlinx.serialization.json.Json

class UpcomingEventMapperTest {

    @Test
    fun `a locked row carries its lock time`() {
        val event = Json.decodeFromString<List<UpcomingEventDto>>(
            """
            [{
                "id": "event", "group_id": "group", "title": "Training",
                "starts_at": "2026-09-25T18:00:00+00:00", "location": null, "capacity": 10,
                "locked_at": "2026-09-20T12:30:00+00:00",
                "in_count": 4, "pending_count": 2, "my_status": "in"
            }]
            """.trimIndent()
        ).single().toEvent()

        assertEquals(Instant.parse("2026-09-20T12:30:00Z"), event.lockedAt)
        assertEquals(4, event.inCount)
        assertEquals(2, event.pendingCount)
        assertEquals(RsvpStatus.In, event.myStatus)
    }

    @Test
    fun `an unlocked row has no lock time`() {
        val event = Json.decodeFromString<List<UpcomingEventDto>>(
            """
            [{
                "id": "event", "group_id": "group", "title": "Training",
                "starts_at": "2026-09-25T18:00:00+00:00", "location": "Park", "capacity": 10,
                "locked_at": null,
                "in_count": 0, "pending_count": 0, "my_status": null
            }]
            """.trimIndent()
        ).single().toEvent()

        assertNull(event.lockedAt)
        assertNull(event.cancelledAt)
    }

    @Test
    fun `a cancelled row carries its cancel time`() {
        val event = Json.decodeFromString<List<UpcomingEventDto>>(
            """
            [{
                "id": "event", "group_id": "group", "title": "Training",
                "starts_at": "2026-09-25T18:00:00+00:00", "location": "Park", "capacity": 10,
                "locked_at": null, "cancelled_at": "2026-09-21T08:00:00+00:00",
                "in_count": 3, "pending_count": 1, "my_status": "pending"
            }]
            """.trimIndent()
        ).single().toEvent()

        assertEquals(Instant.parse("2026-09-21T08:00:00Z"), event.cancelledAt)
    }
}
