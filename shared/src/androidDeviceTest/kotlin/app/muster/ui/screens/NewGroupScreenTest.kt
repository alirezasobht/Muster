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
import app.muster.ui.screens.newgroup.NewGroupScreen
import app.muster.ui.theme.MusterTheme
import org.junit.Rule
import org.junit.Test

class NewGroupScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(
        name: String = "",
        onNameChange: (String) -> Unit = {},
        onCreate: () -> Unit = {},
        onBack: () -> Unit = {},
        creating: Boolean = false,
        nameError: DomainError? = null,
        error: DomainError? = null,
        canCreate: Boolean = name.isNotBlank()
    ) {
        composeRule.setContent {
            MusterTheme {
                NewGroupScreen(
                    name = name,
                    onNameChange = onNameChange,
                    onCreate = onCreate,
                    onBack = onBack,
                    creating = creating,
                    nameError = nameError,
                    error = error,
                    canCreate = canCreate
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
    fun createIsDisabledWhileBlank() {
        show(name = "   ")
        composeRule.onNodeWithText("Create group").assertIsNotEnabled()
    }

    @Test
    fun createIsEnabledWithAName() {
        show(name = "Westgate Wednesday 7s")
        composeRule.onNodeWithText("Create group").assertIsEnabled()
    }

    @Test
    fun createReachesTheCallback() {
        var created = false
        show(name = "Westgate Wednesday 7s", onCreate = { created = true })
        composeRule.onNodeWithText("Create group").performClick()
        assert(created)
    }

    @Test
    fun editingTheNameReachesTheCallback() {
        var typed: String? = null
        show(onNameChange = { typed = it })
        composeRule.onNode(hasSetTextAction()).performTextInput("a")
        assert(typed == "a")
    }

    @Test
    fun aFieldErrorIsShownUnderTheField() {
        show(name = "W", nameError = DomainError.InvalidName())
        composeRule.onNodeWithText("Enter your name.").assertIsDisplayed()
        composeRule.onNodeWithText("Create group").assertIsEnabled()
    }

    // Terminal: retrying the same name won't help, so the button greys.
    @Test
    fun aTerminalFormErrorIsReportedAndDisablesTheButton() {
        show(
            name = "Westgate Wednesday 7s",
            error = DomainError.NotAllowedToCreateGroups(),
            canCreate = false
        )
        composeRule.onNodeWithText("You’re not allowed to create groups.").assertIsDisplayed()
        composeRule.onNodeWithText("Create group").assertIsNotEnabled()
    }

    // Retryable: the button is the retry, so it stays live.
    @Test
    fun aRetryableFormErrorLeavesTheButtonLive() {
        show(name = "Westgate Wednesday 7s", error = DomainError.Network())
        composeRule.onNodeWithText("Create group").assertIsEnabled()
    }

    // While creating, the label is replaced by a spinner.
    @Test
    fun creatingReplacesTheLabelWithASpinner() {
        show(name = "Westgate Wednesday 7s", creating = true)
        composeRule.onNodeWithText("Create group").assertDoesNotExist()
    }
}
