package app.muster.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.muster.domain.error.DomainError
import app.muster.ui.screens.group.editgroup.EditGroupActions
import app.muster.ui.screens.group.editgroup.EditGroupScreen
import app.muster.ui.screens.group.editgroup.EditGroupUiState
import app.muster.ui.theme.MusterTheme
import org.junit.Rule
import org.junit.Test

class EditGroupScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val unchanged = EditGroupUiState(initialName = "Westgate Wednesday 7s")
    private val changed = unchanged.copy(name = "Westgate Thursday 7s")

    private fun show(
        state: EditGroupUiState = unchanged,
        onSave: () -> Unit = {},
        onBack: () -> Unit = {}
    ) {
        composeRule.setContent {
            MusterTheme {
                EditGroupScreen(
                    state = state,
                    actions = EditGroupActions(onNameChange = {}, onSave = onSave, onBack = onBack)
                )
            }
        }
    }

    @Test
    fun theFieldStartsWithTheCurrentName() {
        show()
        composeRule.onNodeWithText("Westgate Wednesday 7s").assertIsDisplayed()
    }

    @Test
    fun saveIsDisabledUntilTheNameChanges() {
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
    fun backReachesTheCallback() {
        var back = false
        show(onBack = { back = true })
        composeRule.onNodeWithContentDescription("Back").performClick()
        assert(back)
    }

    @Test
    fun aFormErrorIsShown() {
        show(state = changed.copy(error = DomainError.Network()))
        composeRule.onNodeWithText("Can’t connect. Check your connection and try again.").assertIsDisplayed()
        composeRule.onNodeWithText("Save").assertIsEnabled()
    }
}
