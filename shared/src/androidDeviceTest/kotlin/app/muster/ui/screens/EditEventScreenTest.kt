package app.muster.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.muster.domain.error.DomainError
import app.muster.ui.common.components.EventFormActions
import app.muster.ui.screens.event.editevent.EditEventScreen
import app.muster.ui.screens.event.editevent.EditEventUiState
import app.muster.ui.theme.MusterTheme
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.Rule
import org.junit.Test

class EditEventScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    // Wednesday, so the date field reads "Wed 23 Sep".
    private val unchanged = EditEventUiState(
        initialTitle = "Weekly 7-a-side",
        initialDate = LocalDate(2026, 9, 23),
        initialTime = LocalTime(19, 0),
        initialLocation = "Westgate Pitch 2"
    )
    private val changed = unchanged.copy(time = LocalTime(20, 0))

    private fun show(
        state: EditEventUiState = unchanged,
        onSave: () -> Unit = {},
        onBack: () -> Unit = {}
    ) {
        composeRule.setContent {
            MusterTheme {
                EditEventScreen(
                    state = state,
                    actions = EventFormActions(
                        onTitleChange = {},
                        onDateChange = {},
                        onTimeChange = {},
                        onLocationChange = {},
                        onCapacityChange = {},
                        onSubmit = onSave,
                        onBack = onBack
                    )
                )
            }
        }
    }

    @Test
    fun theFieldsStartWithTheEventsValues() {
        show()
        composeRule.onNodeWithText("Weekly 7-a-side").assertIsDisplayed()
        composeRule.onNodeWithText("Wed 23 Sep").assertIsDisplayed()
        composeRule.onNodeWithText("7:00 pm").assertIsDisplayed()
        composeRule.onNodeWithText("Westgate Pitch 2").assertIsDisplayed()
    }

    @Test
    fun capacityIsNotEditable() {
        show()
        composeRule.onNodeWithText("Capacity").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Increase capacity").assertDoesNotExist()
    }

    @Test
    fun saveIsDisabledUntilSomethingChanges() {
        show()
        composeRule.onNodeWithText("Save").assertIsNotEnabled()
    }

    @Test
    fun saveReachesTheCallbackOnceChanged() {
        var saved = false
        show(state = changed, onSave = { saved = true })
        composeRule.onNodeWithText("Save").assertIsEnabled().performClick()
        assert(saved)
    }

    @Test
    fun aPastStartIsShownUnderTheDateTimeRow() {
        show(state = changed.copy(dateTimeError = DomainError.EventStartsInPast()))
        composeRule.onNodeWithText("Pick a time that hasn’t passed yet.").assertIsDisplayed()
    }

    @Test
    fun aFrozenEventDisablesSave() {
        show(state = changed.copy(error = DomainError.EventFrozen()))
        composeRule.onNodeWithText("This event has already started.").assertIsDisplayed()
        composeRule.onNodeWithText("Save").assertIsNotEnabled()
    }
}
