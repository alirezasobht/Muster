package app.muster.ui.screens.event

import app.muster.domain.model.RsvpStatus

internal val TestSummary = EventSummary(
    groupId = "group-1",
    eventId = "event-1",
    groupName = "Westgate Wednesday 7s",
    title = "Weekly 7-a-side",
    date = "Wed 17 Sep · 7:00 pm",
    location = "Westgate Pitch 2",
    capacity = 10,
    inCount = 3,
    pendingCount = 1
)

internal val TestRoster = listOf(
    RosterRow(id = "p1", name = "Alex Doyle", status = RsvpStatus.In),
    RosterRow(id = "p2", name = "Tomás Neale", status = RsvpStatus.Pending),
    RosterRow(id = "p3", name = "Marcus Keane", status = RsvpStatus.Out, isSelf = true)
)

internal val TestStandby = listOf(
    StandbyRow(id = "p4", name = "Priya Nair"),
    StandbyRow(id = "p5", name = "Joe Moriarty", isSelf = true)
)

internal val TestSuccess = EventUiState.Success(
    summary = TestSummary,
    isAdmin = true,
    myStatus = RsvpStatus.Out,
    roster = TestRoster
)

internal val TestLocked = TestSuccess.copy(
    phase = EventPhase.Locked(byName = "Ali"),
    lockFeatureFlag = true
)

internal val TestCancelled = TestSuccess.copy(
    phase = EventPhase.Cancelled(byName = "Ali", started = false)
)

internal fun pendingRow(actions: List<RosterAction>) = RosterRow(id = "p2", name = "Tomás Neale", status = RsvpStatus.Pending, actions = actions)
