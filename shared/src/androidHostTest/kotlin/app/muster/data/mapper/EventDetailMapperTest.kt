package app.muster.data.mapper

import app.muster.data.dto.EventDetailDto
import app.muster.domain.model.RsvpStatus
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

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
        assertEquals(emptyList(), detail.roster)
        assertEquals(emptyList(), detail.standby)
    }
}
