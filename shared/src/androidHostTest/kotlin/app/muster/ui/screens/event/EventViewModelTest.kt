package app.muster.ui.screens.event

import app.muster.data.fake.FAKE_USER_ID
import app.muster.data.fake.FakeEventRepository
import app.muster.data.fake.FakeGroupRepository
import app.muster.data.fake.FakeProfileRepository
import app.muster.domain.error.DomainError
import app.muster.domain.event.DataChange
import app.muster.domain.event.DataChanges
import app.muster.domain.model.Event
import app.muster.domain.model.EventDetail
import app.muster.domain.model.GroupRole
import app.muster.domain.model.RosterEntry
import app.muster.domain.model.RsvpStatus
import app.muster.domain.model.StandbyEntry
import app.muster.domain.usecase.DisinvitePlayerUseCase
import app.muster.domain.usecase.GetEventUseCase
import app.muster.domain.usecase.GetMyGroupRoleUseCase
import app.muster.domain.usecase.GetMyProfileUseCase
import app.muster.domain.usecase.ReorderStandbyUseCase
import app.muster.domain.usecase.ResendEventInvitationUseCase
import app.muster.domain.usecase.SetRsvpUseCase
import app.muster.testing.MainDispatcherRule
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class EventViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val groupId = "group-1"
    private val eventId = "event-1"
    private val futureStart = Instant.parse("2099-01-01T09:00:00Z")
    private val pastStart = Instant.parse("2020-01-01T09:00:00Z")

    private val summary = EventSummary(
        groupId = groupId,
        eventId = eventId,
        groupName = "Westgate Wednesday 7s",
        title = "Weekly 7-a-side",
        date = "Wed 17 Sep · 7:00 pm",
        location = "Westgate Pitch 2",
        capacity = 10,
        inCount = 0,
        pendingCount = 0
    )

    private val defaultRoster = listOf(
        RosterEntry(profileId = FAKE_USER_ID, name = "Alex Doyle", status = RsvpStatus.Pending),
        RosterEntry(profileId = "player-2", name = "Dan Whelan", status = RsvpStatus.In),
        RosterEntry(profileId = "player-3", name = "Marcus Keane", status = RsvpStatus.Out)
    )

    private val defaultStandby = listOf(
        StandbyEntry(profileId = "player-4", name = "Priya Nair"),
        StandbyEntry(profileId = "player-5", name = "Joe Moriarty")
    )

    private fun detail(
        startsAt: Instant = futureStart,
        roster: List<RosterEntry> = defaultRoster,
        standby: List<StandbyEntry> = emptyList(),
        capacity: Int = 10
    ) = EventDetail(
        event = Event(
            id = eventId,
            groupId = groupId,
            title = "Weekly 7-a-side",
            startsAt = startsAt,
            location = "Westgate Pitch 2",
            capacity = capacity,
            inCount = roster.count { it.status == RsvpStatus.In },
            pendingCount = roster.count { it.status == RsvpStatus.Pending },
            myStatus = roster.firstOrNull { it.profileId == FAKE_USER_ID }?.status
        ),
        roster = roster,
        standby = standby
    )

    private fun viewModel(
        dataChanges: DataChanges = DataChanges(),
        events: FakeEventRepository = FakeEventRepository(eventDetail = detail(), dataChanges = dataChanges),
        groups: FakeGroupRepository = FakeGroupRepository(myRole = GroupRole.Admin),
        profiles: FakeProfileRepository = FakeProfileRepository()
    ) = EventViewModel(
        initialSummary = summary,
        getEvent = GetEventUseCase(events),
        getMyGroupRole = GetMyGroupRoleUseCase(groups),
        getMyProfile = GetMyProfileUseCase(profiles),
        setRsvp = SetRsvpUseCase(events),
        reorderStandby = ReorderStandbyUseCase(events),
        disinvitePlayer = DisinvitePlayerUseCase(events),
        resendEventInvitation = ResendEventInvitationUseCase(events),
        dataChanges = dataChanges
    )

    @Test
    fun `starts loading with the carried-over summary`() {
        assertEquals(EventUiState.Loading(summary), viewModel().state.value)
    }

    @Test
    fun `a fetched event sorts the roster in, then pending, then out`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        val state = assertIs<EventUiState.Success>(viewModel.state.value)
        assertEquals(listOf("Dan Whelan", "Alex Doyle", "Marcus Keane"), state.roster.map { it.name })
    }

    @Test
    fun `an admin viewer can act, a member viewer cannot`() = runTest {
        val admin = viewModel(groups = FakeGroupRepository(myRole = GroupRole.Admin))
        advanceUntilIdle()
        assertTrue(assertIs<EventUiState.Success>(admin.state.value).isAdmin)

        val member = viewModel(groups = FakeGroupRepository(myRole = GroupRole.Member))
        advanceUntilIdle()
        assertEquals(false, assertIs<EventUiState.Success>(member.state.value).isAdmin)
    }

    @Test
    fun `the viewer's own row is marked isSelf and myStatus matches it`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        val state = assertIs<EventUiState.Success>(viewModel.state.value)
        val ownRow = state.roster.single { it.id == FAKE_USER_ID }
        assertTrue(ownRow.isSelf)
        assertEquals(RsvpStatus.Pending, state.myStatus)
        assertTrue(state.roster.filterNot { it.id == FAKE_USER_ID }.none { it.isSelf })
    }

    @Test
    fun `myStatus is null when the viewer has no invitation`() = runTest {
        val roster = defaultRoster.filterNot { it.profileId == FAKE_USER_ID }
        val events = FakeEventRepository(eventDetail = detail(roster = roster))
        val viewModel = viewModel(events = events)
        advanceUntilIdle()

        assertNull(assertIs<EventUiState.Success>(viewModel.state.value).myStatus)
    }

    @Test
    fun `isFrozen is true once starts_at has passed`() = runTest {
        val events = FakeEventRepository(eventDetail = detail(startsAt = pastStart))
        val viewModel = viewModel(events = events)
        advanceUntilIdle()

        val state = assertIs<EventUiState.Success>(viewModel.state.value)
        assertTrue(state.isFrozen)
        assertTrue(state.startTime.isNotBlank())
    }

    @Test
    fun `isFrozen is false while starts_at is in the future`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals(false, assertIs<EventUiState.Success>(viewModel.state.value).isFrozen)
    }

    @Test
    fun `a load failure reports Error with the carried-over summary`() = runTest {
        val events = FakeEventRepository(eventDetail = detail(), getEventError = DomainError.Network())
        val viewModel = viewModel(events = events)
        advanceUntilIdle()

        val state = assertIs<EventUiState.Error>(viewModel.state.value)
        assertEquals(summary, state.summary)
    }

    @Test
    fun `retrying after a failed load can reach Success`() = runTest {
        val events = FakeEventRepository(eventDetail = detail(), getEventError = DomainError.Network())
        val viewModel = viewModel(events = events)
        advanceUntilIdle()
        assertIs<EventUiState.Error>(viewModel.state.value)

        events.getEventError = null
        viewModel.onRetry()
        advanceUntilIdle()

        assertIs<EventUiState.Success>(viewModel.state.value)
    }

    @Test
    fun `refresh is a no-op before the first load succeeds`() = runTest {
        val events = FakeEventRepository(eventDetail = detail(), getEventError = DomainError.Network())
        val viewModel = viewModel(events = events)
        advanceUntilIdle()
        assertIs<EventUiState.Error>(viewModel.state.value)

        viewModel.onRefresh()
        advanceUntilIdle()

        assertIs<EventUiState.Error>(viewModel.state.value)
    }

    @Test
    fun `the first resume after load is ignored, a later one refreshes`() = runTest {
        val events = FakeEventRepository(eventDetail = detail())
        val viewModel = viewModel(events = events)
        advanceUntilIdle()

        viewModel.onResume()
        advanceUntilIdle()
        assertEquals(3, assertIs<EventUiState.Success>(viewModel.state.value).roster.size)

        events.eventDetail = detail(roster = defaultRoster + RosterEntry("player-4", "Sam Okafor", RsvpStatus.In))
        viewModel.onResume()
        advanceUntilIdle()

        assertEquals(4, assertIs<EventUiState.Success>(viewModel.state.value).roster.size)
    }

    @Test
    fun `a DataChange Roster for this event refreshes the screen`() = runTest {
        val changes = DataChanges()
        val events = FakeEventRepository(eventDetail = detail(), dataChanges = changes)
        val viewModel = viewModel(dataChanges = changes, events = events)
        advanceUntilIdle()

        events.eventDetail = detail(roster = defaultRoster + RosterEntry("player-4", "Sam Okafor", RsvpStatus.In))
        changes.notify(DataChange.Roster(eventId))
        advanceUntilIdle()

        assertEquals(4, assertIs<EventUiState.Success>(viewModel.state.value).roster.size)
    }

    @Test
    fun `a DataChange Roster for a different event is ignored`() = runTest {
        val changes = DataChanges()
        val events = FakeEventRepository(eventDetail = detail(), dataChanges = changes)
        val viewModel = viewModel(dataChanges = changes, events = events)
        advanceUntilIdle()

        events.eventDetail = detail(roster = defaultRoster + RosterEntry("player-4", "Sam Okafor", RsvpStatus.In))
        changes.notify(DataChange.Roster("other-event"))
        advanceUntilIdle()

        assertEquals(3, assertIs<EventUiState.Success>(viewModel.state.value).roster.size)
    }

    @Test
    fun `a DataChange Events for this group refreshes the screen`() = runTest {
        val changes = DataChanges()
        val events = FakeEventRepository(eventDetail = detail(), dataChanges = changes)
        val viewModel = viewModel(dataChanges = changes, events = events)
        advanceUntilIdle()

        events.eventDetail = detail(roster = defaultRoster + RosterEntry("player-4", "Sam Okafor", RsvpStatus.In))
        changes.notify(DataChange.Events(groupId))
        advanceUntilIdle()

        assertEquals(4, assertIs<EventUiState.Success>(viewModel.state.value).roster.size)
    }

    @Test
    fun `onRsvp sends the viewer's own id and status, then clears in-flight via the refresh`() = runTest {
        val changes = DataChanges()
        val events = FakeEventRepository(eventDetail = detail(), dataChanges = changes)
        val viewModel = viewModel(dataChanges = changes, events = events)
        advanceUntilIdle()

        viewModel.onRsvp(RsvpStatus.In)
        advanceUntilIdle()

        assertEquals(listOf(Triple(eventId, FAKE_USER_ID, RsvpStatus.In)), events.rsvpUpdates)
        val state = assertIs<EventUiState.Success>(viewModel.state.value)
        assertEquals(false, state.rsvpInFlight)
        assertEquals(RsvpStatus.In, state.myStatus)
    }

    @Test
    fun `a failed onRsvp surfaces rsvpError`() = runTest {
        val events = FakeEventRepository(eventDetail = detail(), setRsvpError = DomainError.EventFull())
        val viewModel = viewModel(events = events)
        advanceUntilIdle()

        viewModel.onRsvp(RsvpStatus.In)
        advanceUntilIdle()

        val state = assertIs<EventUiState.Success>(viewModel.state.value)
        assertIs<DomainError.EventFull>(state.rsvpError)
        assertEquals(false, state.rsvpInFlight)
    }

    @Test
    fun `a second onRsvp while in flight is ignored`() = runTest {
        val events = FakeEventRepository(eventDetail = detail())
        val viewModel = viewModel(events = events)
        advanceUntilIdle()

        viewModel.onRsvp(RsvpStatus.In)
        viewModel.onRsvp(RsvpStatus.Out)
        advanceUntilIdle()

        assertEquals(1, events.rsvpUpdates.size)
    }

    @Test
    fun `a status action targets the given player, not the viewer`() = runTest {
        val changes = DataChanges()
        val events = FakeEventRepository(eventDetail = detail(), dataChanges = changes)
        val viewModel = viewModel(dataChanges = changes, events = events)
        advanceUntilIdle()

        viewModel.onRosterAction("player-2", RosterAction.SetOut)
        advanceUntilIdle()

        assertEquals(listOf(Triple(eventId, "player-2", RsvpStatus.Out)), events.rsvpUpdates)
        assertNull(assertIs<EventUiState.Success>(viewModel.state.value).rowActionTargetId)
    }

    @Test
    fun `a failed status action surfaces rowActionError for that row`() = runTest {
        val events = FakeEventRepository(eventDetail = detail(), setRsvpError = DomainError.EventFull())
        val viewModel = viewModel(events = events)
        advanceUntilIdle()

        viewModel.onRosterAction("player-3", RosterAction.SetIn)
        advanceUntilIdle()

        val state = assertIs<EventUiState.Success>(viewModel.state.value)
        assertIs<DomainError.EventFull>(state.rowActionError)
        assertEquals("player-3", state.rowActionErrorId)
        assertNull(state.rowActionTargetId)
    }

    @Test
    fun `a new row action clears the previous row's error`() = runTest {
        val events = FakeEventRepository(eventDetail = detail(), setRsvpError = DomainError.EventFull())
        val viewModel = viewModel(events = events)
        advanceUntilIdle()
        viewModel.onRosterAction("player-3", RosterAction.SetIn)
        advanceUntilIdle()

        events.setRsvpError = null
        viewModel.onRosterAction("player-2", RosterAction.SetOut)

        val state = assertIs<EventUiState.Success>(viewModel.state.value)
        assertNull(state.rowActionError)
        assertNull(state.rowActionErrorId)
    }

    @Test
    fun `a second row action while one is in flight is ignored`() = runTest {
        val events = FakeEventRepository(eventDetail = detail())
        val viewModel = viewModel(events = events)
        advanceUntilIdle()

        viewModel.onRosterAction("player-2", RosterAction.SetOut)
        viewModel.onRosterAction("player-3", RosterAction.SetIn)
        advanceUntilIdle()

        assertEquals(1, events.rsvpUpdates.size)
    }

    private fun EventUiState.actionsFor(id: String) =
        assertIs<EventUiState.Success>(this).roster.single { it.id == id }.actions

    @Test
    fun `an admin gets status and removal actions per status`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        val state = viewModel.state.value

        assertEquals(
            listOf(RosterAction.SetIn, RosterAction.SetOut, RosterAction.ResendInvite, RosterAction.CancelInvite),
            state.actionsFor(FAKE_USER_ID)
        )
        assertEquals(
            listOf(RosterAction.SetOut, RosterAction.RemoveFromEvent),
            state.actionsFor("player-2")
        )
        assertEquals(
            listOf(RosterAction.SetIn, RosterAction.RemoveFromList),
            state.actionsFor("player-3")
        )
    }

    @Test
    fun `a member can change only their own status and remove nobody`() = runTest {
        val viewModel = viewModel(groups = FakeGroupRepository(myRole = GroupRole.Member))
        advanceUntilIdle()
        val state = viewModel.state.value

        assertEquals(listOf(RosterAction.SetIn, RosterAction.SetOut), state.actionsFor(FAKE_USER_ID))
        assertEquals(emptyList(), state.actionsFor("player-2"))
        assertEquals(emptyList(), state.actionsFor("player-3"))
    }

    @Test
    fun `out to in is not offered on a full event`() = runTest {
        // 1 in + 1 pending fills a capacity of 2.
        val events = FakeEventRepository(eventDetail = detail(capacity = 2))
        val viewModel = viewModel(events = events)
        advanceUntilIdle()

        assertEquals(listOf(RosterAction.RemoveFromList), viewModel.state.value.actionsFor("player-3"))
    }

    @Test
    fun `out to in reappears once a slot frees, using fresh counts`() = runTest {
        val changes = DataChanges()
        val events = FakeEventRepository(eventDetail = detail(capacity = 2), dataChanges = changes)
        val viewModel = viewModel(dataChanges = changes, events = events)
        advanceUntilIdle()

        viewModel.onRosterAction("player-2", RosterAction.RemoveFromEvent)
        advanceUntilIdle()

        assertTrue(RosterAction.SetIn in viewModel.state.value.actionsFor("player-3"))
    }

    @Test
    fun `removing a player who held a slot promotes the front of the queue`() = runTest {
        val changes = DataChanges()
        val events = FakeEventRepository(eventDetail = detail(standby = defaultStandby), dataChanges = changes)
        val viewModel = viewModel(dataChanges = changes, events = events)
        advanceUntilIdle()

        viewModel.onRosterAction("player-2", RosterAction.RemoveFromEvent)
        advanceUntilIdle()

        val state = assertIs<EventUiState.Success>(viewModel.state.value)
        assertTrue(state.roster.none { it.id == "player-2" })
        assertEquals(RsvpStatus.Pending, state.roster.single { it.id == "player-4" }.status)
        assertEquals(listOf("player-5"), state.standby.map { it.id })
    }

    @Test
    fun `removing an out player promotes nobody`() = runTest {
        val changes = DataChanges()
        val events = FakeEventRepository(eventDetail = detail(standby = defaultStandby), dataChanges = changes)
        val viewModel = viewModel(dataChanges = changes, events = events)
        advanceUntilIdle()

        viewModel.onRosterAction("player-3", RosterAction.RemoveFromList)
        advanceUntilIdle()

        val state = assertIs<EventUiState.Success>(viewModel.state.value)
        assertTrue(state.roster.none { it.id == "player-3" })
        assertEquals(listOf("player-4", "player-5"), state.standby.map { it.id })
    }

    @Test
    fun `a failed removal surfaces rowActionError for that row`() = runTest {
        val events = FakeEventRepository(eventDetail = detail(), disinvitePlayerError = DomainError.Network())
        val viewModel = viewModel(events = events)
        advanceUntilIdle()

        viewModel.onRosterAction("player-2", RosterAction.RemoveFromEvent)
        advanceUntilIdle()

        val state = assertIs<EventUiState.Success>(viewModel.state.value)
        assertIs<DomainError.Network>(state.rowActionError)
        assertNull(state.rowActionTargetId)
    }

    @Test
    fun `resending an event invitation calls through`() = runTest {
        val events = FakeEventRepository(eventDetail = detail())
        val viewModel = viewModel(events = events)
        advanceUntilIdle()

        viewModel.onRosterAction(FAKE_USER_ID, RosterAction.ResendInvite)
        advanceUntilIdle()

        assertEquals(listOf(FAKE_USER_ID), events.resentInvitationProfileIds)
        val state = assertIs<EventUiState.Success>(viewModel.state.value)
        assertNull(state.rowActionError)
    }

    @Test
    fun `a throttled resend surfaces rowActionError for that row`() = runTest {
        val events = FakeEventRepository(
            eventDetail = detail(),
            resendInvitationError = DomainError.InvitationSentTooRecently()
        )
        val viewModel = viewModel(events = events)
        advanceUntilIdle()

        viewModel.onRosterAction(FAKE_USER_ID, RosterAction.ResendInvite)
        advanceUntilIdle()

        val state = assertIs<EventUiState.Success>(viewModel.state.value)
        assertIs<DomainError.InvitationSentTooRecently>(state.rowActionError)
        assertEquals(FAKE_USER_ID, state.rowActionErrorId)
    }

    @Test
    fun `the standby queue keeps its own order, not alphabetical`() = runTest {
        val events = FakeEventRepository(eventDetail = detail(standby = defaultStandby))
        val viewModel = viewModel(events = events)
        advanceUntilIdle()

        val state = assertIs<EventUiState.Success>(viewModel.state.value)
        assertEquals(listOf("Priya Nair", "Joe Moriarty"), state.standby.map { it.name })
    }

    @Test
    fun `the viewer's own standby row is marked isSelf`() = runTest {
        val standby = defaultStandby + StandbyEntry(profileId = FAKE_USER_ID, name = "Alex Doyle")
        val events = FakeEventRepository(eventDetail = detail(roster = emptyList(), standby = standby))
        val viewModel = viewModel(events = events)
        advanceUntilIdle()

        val state = assertIs<EventUiState.Success>(viewModel.state.value)
        assertTrue(state.standby.single { it.id == FAKE_USER_ID }.isSelf)
        assertTrue(state.standby.filterNot { it.id == FAKE_USER_ID }.none { it.isSelf })
    }

    @Test
    fun `onReorderStandby sends the ordered ids and clears in-flight via the refresh`() = runTest {
        val changes = DataChanges()
        val events = FakeEventRepository(eventDetail = detail(standby = defaultStandby), dataChanges = changes)
        val viewModel = viewModel(dataChanges = changes, events = events)
        advanceUntilIdle()

        viewModel.onReorderStandby(listOf("player-5", "player-4"))
        advanceUntilIdle()

        assertEquals(listOf(listOf("player-5", "player-4")), events.reorderCalls)
        val state = assertIs<EventUiState.Success>(viewModel.state.value)
        assertEquals(false, state.standbyReordering)
        assertEquals(listOf("Joe Moriarty", "Priya Nair"), state.standby.map { it.name })
    }

    // The exact race the task calls out: reordering can promote, so the
    // queue the admin just sent can already be shorter/different by the
    // time the write lands.
    @Test
    fun `reordering can promote, shrinking the queue the caller just sent`() = runTest {
        val changes = DataChanges()
        val events = FakeEventRepository(eventDetail = detail(standby = defaultStandby), dataChanges = changes)
        val viewModel = viewModel(dataChanges = changes, events = events)
        advanceUntilIdle()

        // Simulates the trigger promoting player-4 out of the queue and
        // into the roster as part of the same write.
        events.eventDetail = events.eventDetail.copy(
            roster = defaultRoster + RosterEntry("player-4", "Priya Nair", RsvpStatus.Pending),
            standby = listOf(StandbyEntry("player-5", "Joe Moriarty"))
        )
        viewModel.onReorderStandby(listOf("player-4", "player-5"))
        advanceUntilIdle()

        val state = assertIs<EventUiState.Success>(viewModel.state.value)
        assertEquals(1, state.standby.size)
        assertEquals(4, state.roster.size)
    }

    @Test
    fun `a failed reorder surfaces standbyError`() = runTest {
        val events = FakeEventRepository(
            eventDetail = detail(standby = defaultStandby),
            reorderStandbyError = DomainError.StandbyQueueStale()
        )
        val viewModel = viewModel(events = events)
        advanceUntilIdle()

        viewModel.onReorderStandby(listOf("player-5", "player-4"))
        advanceUntilIdle()

        val state = assertIs<EventUiState.Success>(viewModel.state.value)
        assertIs<DomainError.StandbyQueueStale>(state.standbyError)
        assertEquals(false, state.standbyReordering)
    }

    @Test
    fun `a second reorder while one is in flight is ignored`() = runTest {
        val events = FakeEventRepository(eventDetail = detail(standby = defaultStandby))
        val viewModel = viewModel(events = events)
        advanceUntilIdle()

        viewModel.onReorderStandby(listOf("player-5", "player-4"))
        viewModel.onReorderStandby(listOf("player-4", "player-5"))
        advanceUntilIdle()

        assertEquals(1, events.reorderCalls.size)
    }
}
