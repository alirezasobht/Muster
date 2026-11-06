package app.muster.ui.screens.event.components

import app.muster.domain.model.RsvpStatus
import app.muster.ui.screens.event.EventActions
import app.muster.ui.screens.event.EventSummary
import app.muster.ui.screens.event.EventUiState
import app.muster.ui.screens.event.RosterRow
import app.muster.ui.screens.event.StandbyRow

internal val PreviewActions = EventActions(
    onBack = {},
    onRsvp = {},
    onRosterAction = { _, _ -> },
    onReorderStandby = {},
    onRetry = {}
)

internal val PreviewRoster = listOf(
    RosterRow(id = "1", name = "Alex Doyle", status = RsvpStatus.In),
    RosterRow(id = "2", name = "Dan Whelan", status = RsvpStatus.In),
    RosterRow(id = "3", name = "Sam Okafor", status = RsvpStatus.In),
    RosterRow(id = "4", name = "Tomás Neale", status = RsvpStatus.Pending),
    RosterRow(id = "5", name = "Marcus Keane", status = RsvpStatus.Out, isSelf = true)
)

internal val PreviewStandby = listOf(
    StandbyRow(id = "6", name = "Joe Moriarty"),
    StandbyRow(id = "7", name = "Priya Nair", isSelf = true),
    StandbyRow(id = "8", name = "Leo Fanning")
)

internal val PreviewSummary = EventSummary(
    groupId = "1",
    eventId = "2",
    groupName = "Westgate Wednesday 7s",
    title = "Weekly 7-a-side",
    date = "Wed 17 Sep · 7:00 pm",
    location = "Westgate Pitch 2",
    capacity = 10,
    inCount = 3,
    pendingCount = 1
)

internal val PreviewSuccess = EventUiState.Success(
    summary = PreviewSummary,
    isAdmin = true,
    myStatus = RsvpStatus.Out,
    roster = PreviewRoster,
    lockFeatureFlag = true,
    cancelFeatureFlag = true
)
