package app.muster.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.muster.domain.error.DomainError
import app.muster.ui.screens.group.events.EventRow
import app.muster.ui.screens.group.events.EventsActions
import app.muster.ui.screens.group.events.EventsTab
import app.muster.ui.screens.group.events.EventsUiState
import app.muster.ui.screens.group.events.MemberEventStatus
import app.muster.ui.theme.MusterTheme
import org.junit.Rule
import org.junit.Test

class EventsTabTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val event = EventRow(
        id = "1",
        title = "Weekly 7-a-side",
        date = "Wed 17 Sep · 7:00 pm",
        location = "Westgate Pitch 2",
        capacity = 10,
        inCount = 7,
        pendingCount = 3,
        status = MemberEventStatus.In
    )

    private fun show(
        state: EventsUiState,
        onSelectEvent: (String) -> Unit = {},
        onNewEvent: () -> Unit = {},
        onRetry: () -> Unit = {}
    ) {
        composeRule.setContent {
            MusterTheme {
                EventsTab(
                    state = state,
                    actions = EventsActions(
                        onSelectEvent = onSelectEvent,
                        onNewEvent = onNewEvent,
                        onRetry = onRetry
                    ),
                    spinnerDelayMillis = 0
                )
            }
        }
    }

    @Test
    fun loadingShowsNoRows() {
        show(state = EventsUiState.Loading)
        composeRule.onNodeWithContentDescription("New event").assertDoesNotExist()
    }

    @Test
    fun failedShowsTheErrorMessage() {
        show(state = EventsUiState.Error(DomainError.Network()))
        composeRule.onNodeWithText("Couldn’t load events").assertIsDisplayed()
    }

    @Test
    fun failedTryAgainReachesTheCallback() {
        var retried = false
        show(state = EventsUiState.Error(DomainError.Network()), onRetry = { retried = true })
        composeRule.onNodeWithText("Try again").performClick()
        assert(retried)
    }

    @Test
    fun theEmptyStateIsShownWhenThereAreNoUpcomingEvents() {
        show(state = EventsUiState.Success(events = emptyList()))
        composeRule.onNodeWithText("No upcoming events").assertIsDisplayed()
    }

    @Test
    fun newEventIsShownOnlyWhenAllowed() {
        show(state = EventsUiState.Success(events = listOf(event), canCreateEvent = true))
        composeRule.onNodeWithContentDescription("New event").assertIsDisplayed()
    }

    @Test
    fun newEventIsHiddenForNonAdmins() {
        show(state = EventsUiState.Success(events = listOf(event), canCreateEvent = false))
        composeRule.onNodeWithContentDescription("New event").assertDoesNotExist()
    }

    @Test
    fun tappingNewEventReachesTheCallback() {
        var tapped = false
        show(
            state = EventsUiState.Success(events = listOf(event), canCreateEvent = true),
            onNewEvent = { tapped = true }
        )
        composeRule.onNodeWithContentDescription("New event").performClick()
        assert(tapped)
    }

    @Test
    fun aRowShowsItsDateTitleLocationStatsAndStatus() {
        show(state = EventsUiState.Success(events = listOf(event)))
        composeRule.onNodeWithText("Wed 17 Sep · 7:00 pm").assertIsDisplayed()
        composeRule.onNodeWithText("Weekly 7-a-side").assertIsDisplayed()
        composeRule.onNodeWithText("Westgate Pitch 2").assertIsDisplayed()
        composeRule.onNodeWithText("10 of 10 slots · 7 in, 3 pending").assertIsDisplayed()
        composeRule.onNodeWithText("In").assertIsDisplayed()
    }

    @Test
    fun aRowWithNoStatusShowsNoBadge() {
        show(state = EventsUiState.Success(events = listOf(event.copy(status = null))))
        composeRule.onNodeWithText("In").assertDoesNotExist()
        composeRule.onNodeWithText("Pending").assertDoesNotExist()
        composeRule.onNodeWithText("Out").assertDoesNotExist()
    }

    @Test
    fun tappingARowReachesTheCallbackWithItsId() {
        var selected: String? = null
        show(state = EventsUiState.Success(events = listOf(event)), onSelectEvent = { selected = it })
        composeRule.onNodeWithText("Weekly 7-a-side").performClick()
        assert(selected == "1")
    }
}
