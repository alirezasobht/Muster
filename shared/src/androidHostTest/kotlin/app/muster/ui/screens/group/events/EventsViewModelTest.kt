package app.muster.ui.screens.group.events

import app.muster.data.fake.FakeEventRepository
import app.muster.data.fake.FakeGroupRepository
import app.muster.domain.error.DomainError
import app.muster.domain.event.DataChange
import app.muster.domain.event.DataChanges
import app.muster.domain.model.Event
import app.muster.domain.model.GroupRole
import app.muster.domain.model.RsvpStatus
import app.muster.domain.usecase.GetMyGroupRoleUseCase
import app.muster.domain.usecase.ListUpcomingEventsUseCase
import app.muster.testing.MainDispatcherRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.time.Instant
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule

class EventsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val groupId = "group-1"

    private val fullEvent = Event(
        id = "event-1",
        groupId = groupId,
        title = "Weekly 7-a-side",
        startsAt = Instant.parse("2026-09-17T09:00:00Z"),
        location = "Westgate Pitch 2",
        capacity = 10,
        inCount = 7,
        pendingCount = 3,
        myStatus = RsvpStatus.In
    )

    private fun viewModel(
        groupId: String = this.groupId,
        groups: FakeGroupRepository = FakeGroupRepository(myRole = GroupRole.Admin),
        events: FakeEventRepository = FakeEventRepository(),
        dataChanges: DataChanges = DataChanges()
    ) = EventsViewModel(
        groupId = groupId,
        listUpcomingEvents = ListUpcomingEventsUseCase(events),
        getMyGroupRole = GetMyGroupRoleUseCase(groups),
        dataChanges = dataChanges
    )

    @Test
    fun `starts loading`() {
        assertEquals(EventsUiState.Loading, viewModel().state.value)
    }

    @Test
    fun `an admin viewer can create events, a member viewer cannot`() = runTest {
        val admin = viewModel(groups = FakeGroupRepository(myRole = GroupRole.Admin))
        advanceUntilIdle()
        assertEquals(true, assertIs<EventsUiState.Success>(admin.state.value).canCreateEvent)

        val member = viewModel(groups = FakeGroupRepository(myRole = GroupRole.Member))
        advanceUntilIdle()
        assertEquals(false, assertIs<EventsUiState.Success>(member.state.value).canCreateEvent)
    }

    @Test
    fun `a row carries the formatted date, the slot counts and the viewer's own status`() = runTest {
        val events = FakeEventRepository(events = listOf(fullEvent))
        val viewModel = viewModel(events = events)
        advanceUntilIdle()

        val state = assertIs<EventsUiState.Success>(viewModel.state.value)
        val row = state.eventRows.single()
        assertEquals("Weekly 7-a-side", row.title)
        assertEquals("Westgate Pitch 2", row.location)
        assertEquals("Thu 17 Sep · 7:00 pm", row.date)
        assertEquals(10, row.capacity)
        assertEquals(7, row.inCount)
        assertEquals(3, row.pendingCount)
        assertEquals(MemberEventStatus.In, row.status)
    }

    @Test
    fun `midnight and noon format with the 12-hour edge cases`() = runTest {
        val midnight = fullEvent.copy(id = "event-midnight", startsAt = Instant.parse("2026-09-16T14:00:00Z"))
        val noon = fullEvent.copy(id = "event-noon", startsAt = Instant.parse("2026-09-16T02:00:00Z"))
        val events = FakeEventRepository(events = listOf(midnight, noon))
        val viewModel = viewModel(events = events)
        advanceUntilIdle()

        val state = assertIs<EventsUiState.Success>(viewModel.state.value)
        assertEquals("Thu 17 Sep · 12:00 am", state.eventRows.first { it.id == "event-midnight" }.date)
        assertEquals("Wed 16 Sep · 12:00 pm", state.eventRows.first { it.id == "event-noon" }.date)
    }

    @Test
    fun `no event_invitations row for the viewer means no badge, and a null location is blank`() = runTest {
        val notInvited = fullEvent.copy(id = "event-2", location = null, myStatus = null)
        val events = FakeEventRepository(events = listOf(notInvited))
        val viewModel = viewModel(events = events)
        advanceUntilIdle()

        val row = assertIs<EventsUiState.Success>(viewModel.state.value).eventRows.single()
        assertNull(row.status)
        assertEquals("", row.location)
    }

    @Test
    fun `an events load failure reports Error`() = runTest {
        val events = FakeEventRepository(listUpcomingEventsError = DomainError.Network())
        val viewModel = viewModel(events = events)
        advanceUntilIdle()

        assertIs<EventsUiState.Error>(viewModel.state.value)
    }

    @Test
    fun `retrying after a failed load can reach Success`() = runTest {
        val events = FakeEventRepository(listUpcomingEventsError = DomainError.Network())
        val viewModel = viewModel(events = events)
        advanceUntilIdle()
        assertIs<EventsUiState.Error>(viewModel.state.value)

        events.listUpcomingEventsError = null
        viewModel.onRetry()
        advanceUntilIdle()

        assertIs<EventsUiState.Success>(viewModel.state.value)
    }

    @Test
    fun `refresh is a no-op before the first load succeeds`() = runTest {
        val events = FakeEventRepository(listUpcomingEventsError = DomainError.Network())
        val viewModel = viewModel(events = events)
        advanceUntilIdle()
        assertIs<EventsUiState.Error>(viewModel.state.value)

        viewModel.onRefresh()
        advanceUntilIdle()

        assertIs<EventsUiState.Error>(viewModel.state.value)
    }

    @Test
    fun `the first resume after load is ignored, a later one refreshes`() = runTest {
        val events = FakeEventRepository(events = listOf(fullEvent))
        val viewModel = viewModel(events = events)
        advanceUntilIdle()

        viewModel.onResume()
        advanceUntilIdle()
        assertEquals(1, assertIs<EventsUiState.Success>(viewModel.state.value).eventRows.size)

        // Simulates a second event appearing between resumes.
        events.events = listOf(fullEvent, fullEvent.copy(id = "event-2"))
        viewModel.onResume()
        advanceUntilIdle()

        assertEquals(2, assertIs<EventsUiState.Success>(viewModel.state.value).eventRows.size)
    }

    @Test
    fun `a DataChange for this group refreshes the list`() = runTest {
        val changes = DataChanges()
        val events = FakeEventRepository(events = listOf(fullEvent))
        val viewModel = viewModel(events = events, dataChanges = changes)
        advanceUntilIdle()

        events.events = listOf(fullEvent, fullEvent.copy(id = "event-2"))
        changes.notify(DataChange.Events(groupId))
        advanceUntilIdle()

        assertEquals(2, assertIs<EventsUiState.Success>(viewModel.state.value).eventRows.size)
    }

    @Test
    fun `a DataChange for a different group is ignored`() = runTest {
        val changes = DataChanges()
        val events = FakeEventRepository(events = listOf(fullEvent))
        val viewModel = viewModel(events = events, dataChanges = changes)
        advanceUntilIdle()

        events.events = listOf(fullEvent, fullEvent.copy(id = "event-2"))
        changes.notify(DataChange.Events("other-group"))
        advanceUntilIdle()

        assertEquals(1, assertIs<EventsUiState.Success>(viewModel.state.value).eventRows.size)
    }
}
