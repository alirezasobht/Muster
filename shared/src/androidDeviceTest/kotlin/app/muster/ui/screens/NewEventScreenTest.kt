package app.muster.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import app.muster.domain.error.DomainError
import app.muster.ui.screens.newevent.NewEventActions
import app.muster.ui.screens.newevent.NewEventScreen
import app.muster.ui.screens.newevent.NewEventUiState
import app.muster.ui.theme.MusterTheme
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.Rule
import org.junit.Test

class NewEventScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    // Wednesday, so the date field's expected label is "Wed 23 Sep".
    private val validDate = LocalDate(2026, 9, 23)
    private val validTime = LocalTime(19, 0)

    private val completeState = NewEventUiState(
        title = "Weekly 7-a-side",
        date = validDate,
        time = validTime
    )

    private fun show(
        state: NewEventUiState = NewEventUiState(),
        onTitleChange: (String) -> Unit = {},
        onDateChange: (LocalDate) -> Unit = {},
        onTimeChange: (LocalTime) -> Unit = {},
        onLocationChange: (String) -> Unit = {},
        onCapacityChange: (Int) -> Unit = {},
        onCreate: () -> Unit = {},
        onBack: () -> Unit = {}
    ) {
        composeRule.setContent {
            MusterTheme {
                NewEventScreen(
                    state = state,
                    actions = NewEventActions(
                        onTitleChange = onTitleChange,
                        onDateChange = onDateChange,
                        onTimeChange = onTimeChange,
                        onLocationChange = onLocationChange,
                        onCapacityChange = onCapacityChange,
                        onCreate = onCreate,
                        onBack = onBack
                    )
                )
            }
        }
    }

    @Test
    fun backReachesTheCallback() {
        var back = false
        show(onBack = { back = true })
        composeRule.onNodeWithContentDescription("Back").performClick()
        assert(back)
    }

    @Test
    fun createIsDisabledWhileIncomplete() {
        show(state = NewEventUiState(title = "Weekly 7-a-side"))
        composeRule.onNodeWithText("Create event").assertIsNotEnabled()
    }

    @Test
    fun createIsEnabledWithTitleDateAndTime() {
        show(state = completeState)
        composeRule.onNodeWithText("Create event").assertIsEnabled()
    }

    @Test
    fun createReachesTheCallback() {
        var created = false
        show(state = completeState, onCreate = { created = true })
        composeRule.onNodeWithText("Create event").performClick()
        assert(created)
    }

    @Test
    fun editingTheTitleReachesTheCallback() {
        var typed: String? = null
        show(onTitleChange = { typed = it })
        composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("a")
        assert(typed == "a")
    }

    @Test
    fun editingTheLocationReachesTheCallback() {
        var typed: String? = null
        show(onLocationChange = { typed = it })
        composeRule.onAllNodes(hasSetTextAction())[1].performTextInput("a")
        assert(typed == "a")
    }

    @Test
    fun theCapacityHintExplainsWhatItCounts() {
        show()
        composeRule.onNodeWithText("Invites count toward this, answered or not. Can’t be changed later.")
            .assertIsDisplayed()
    }

    @Test
    fun increasingCapacityReachesTheCallback() {
        var capacity: Int? = null
        show(state = NewEventUiState(capacity = 10), onCapacityChange = { capacity = it })
        composeRule.onNodeWithContentDescription("Increase capacity").performClick()
        assert(capacity == 11)
    }

    @Test
    fun decreasingCapacityReachesTheCallback() {
        var capacity: Int? = null
        show(state = NewEventUiState(capacity = 10), onCapacityChange = { capacity = it })
        composeRule.onNodeWithContentDescription("Decrease capacity").performClick()
        assert(capacity == 9)
    }

    @Test
    fun capacityCannotBeDecreasedBelowOne() {
        show(state = NewEventUiState(capacity = 1))
        composeRule.onNodeWithContentDescription("Decrease capacity").assertIsNotEnabled()
    }

    @Test
    fun dateAndTimeShowPlaceholdersWhenUnset() {
        show()
        composeRule.onNodeWithText("Select date").assertIsDisplayed()
        composeRule.onNodeWithText("Select time").assertIsDisplayed()
    }

    @Test
    fun dateAndTimeShowTheirFormattedValuesWhenSet() {
        show(state = completeState)
        composeRule.onNodeWithText("Wed 23 Sep").assertIsDisplayed()
        composeRule.onNodeWithText("7:00 pm").assertIsDisplayed()
    }

    @Test
    fun tappingTheDateFieldOpensAPickerThatCancelDismisses() {
        var changed = false
        show(onDateChange = { changed = true })
        composeRule.onNodeWithText("Select date").performClick()
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.onNodeWithText("Select date").assertIsDisplayed()
        assert(!changed)
    }

    @Test
    fun tappingTheTimeFieldOpensAPickerThatCancelDismisses() {
        var changed = false
        show(onTimeChange = { changed = true })
        composeRule.onNodeWithText("Select time").performClick()
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.onNodeWithText("Select time").assertIsDisplayed()
        assert(!changed)
    }

    @Test
    fun aDateTimeFieldErrorIsShownUnderTheRow() {
        show(state = completeState.copy(dateTimeError = DomainError.EventStartsInPast()))
        composeRule.onNodeWithText("Pick a time that hasn’t passed yet.").assertIsDisplayed()
    }

    // Terminal: retrying the same request won't help, so the button greys.
    @Test
    fun aTerminalFormErrorIsReportedAndDisablesTheButton() {
        show(state = completeState.copy(error = DomainError.NotSignedIn()))
        composeRule.onNodeWithText("You’ve been signed out. Sign in again.").assertIsDisplayed()
        composeRule.onNodeWithText("Create event").assertIsNotEnabled()
    }

    // Retryable: the button is the retry, so it stays live.
    @Test
    fun aRetryableFormErrorLeavesTheButtonEnabled() {
        show(state = completeState.copy(error = DomainError.Network()))
        composeRule.onNodeWithText("Create event").assertIsEnabled()
    }

    @Test
    fun creatingReplacesTheLabelWithASpinner() {
        show(state = completeState.copy(creating = true))
        composeRule.onNodeWithText("Create event").assertDoesNotExist()
    }
}
