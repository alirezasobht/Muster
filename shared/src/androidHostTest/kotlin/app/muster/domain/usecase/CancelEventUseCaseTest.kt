package app.muster.domain.usecase

import app.muster.data.fake.FakeEventRepository
import app.muster.domain.error.DomainError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

class CancelEventUseCaseTest {

    @Test
    fun `cancelling sets the cancel and keeps the first one`() = runTest {
        val events = FakeEventRepository()
        val eventId = events.eventDetail.event.id
        val cancelEvent = CancelEventUseCase(events)

        cancelEvent(eventId)
        val first = events.getEvent(eventId).event.cancelledAt
        assertNotNull(first)

        cancelEvent(eventId)
        assertEquals(first, events.getEvent(eventId).event.cancelledAt)
    }

    @Test
    fun `a started event's rejection surfaces`() = runTest {
        val events = FakeEventRepository(cancelEventError = DomainError.EventFrozen())
        assertFailsWith<DomainError.EventFrozen> {
            CancelEventUseCase(events)(events.eventDetail.event.id)
        }
        assertNull(events.eventDetail.event.cancelledAt)
    }
}
