@file:OptIn(ExperimentalCoroutinesApi::class)

package app.muster.domain.usecase

import app.muster.data.fake.FakeEventRepository
import app.muster.domain.error.DomainError
import app.muster.domain.event.DataChange
import app.muster.domain.event.DataChanges
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

class DeleteEventUseCaseTest {

    @Test
    fun `deleting removes the event and refreshes its group's list`() = runTest {
        val changes = DataChanges()
        val events = FakeEventRepository(dataChanges = changes)
        val event = events.createEvent(
            groupId = "group-1",
            title = "Training",
            startsAt = events.eventDetail.event.startsAt,
            location = null,
            capacity = 10
        )
        var change: DataChange? = null
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { change = changes.changes.first() }

        DeleteEventUseCase(events)(event.id, event.groupId)

        assertEquals(emptyList(), events.listUpcomingEvents(event.groupId))
        assertEquals(listOf(event.id), events.deletedEventIds)
        assertEquals(DataChange.Events(event.groupId), change)
    }

    @Test
    fun `a rejection surfaces and leaves the event in place`() = runTest {
        val events = FakeEventRepository(deleteEventError = DomainError.Unknown(RuntimeException("not admin")))
        val event = events.createEvent(
            groupId = "group-1",
            title = "Training",
            startsAt = events.eventDetail.event.startsAt,
            location = null,
            capacity = 10
        )

        assertFailsWith<DomainError.Unknown> { DeleteEventUseCase(events)(event.id, event.groupId) }

        assertEquals(listOf(event), events.listUpcomingEvents(event.groupId))
        assertEquals(emptyList(), events.deletedEventIds)
    }
}
