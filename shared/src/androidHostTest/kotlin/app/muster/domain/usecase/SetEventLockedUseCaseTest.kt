package app.muster.domain.usecase

import app.muster.data.fake.FakeEventRepository
import app.muster.domain.error.DomainError
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

class SetEventLockedUseCaseTest {

    @Test
    fun `locking then unlocking sets and clears the lock`() = runTest {
        val events = FakeEventRepository()
        val eventId = events.eventDetail.event.id
        val setEventLocked = SetEventLockedUseCase(events)

        setEventLocked(eventId, locked = true)
        assertNotNull(events.getEvent(eventId).event.lockedAt)

        setEventLocked(eventId, locked = false)
        assertNull(events.getEvent(eventId).event.lockedAt)
    }

    @Test
    fun `a started event's rejection surfaces`() = runTest {
        val events = FakeEventRepository(setEventLockedError = DomainError.EventFrozen())
        assertFailsWith<DomainError.EventFrozen> {
            SetEventLockedUseCase(events)(events.eventDetail.event.id, locked = true)
        }
        assertNull(events.eventDetail.event.lockedAt)
    }
}
