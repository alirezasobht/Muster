package app.muster.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.muster.domain.error.DomainError
import app.muster.ui.screens.settings.DeleteAccountDialog
import app.muster.ui.screens.settings.DeleteAccountUiState
import app.muster.ui.theme.MusterTheme
import org.junit.Rule
import org.junit.Test

class DeleteAccountDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val warning = "They’ll disappear for all their members, and this can’t be undone in the app."

    private fun show(
        state: DeleteAccountUiState,
        onConfirm: () -> Unit = {},
        onDismiss: () -> Unit = {}
    ) {
        composeRule.setContent {
            MusterTheme {
                DeleteAccountDialog(state = state, onConfirm = onConfirm, onDismiss = onDismiss)
            }
        }
    }

    @Test
    fun hiddenShowsNothing() {
        show(state = DeleteAccountUiState.Hidden)
        composeRule.onNodeWithText("Delete your account?").assertDoesNotExist()
        composeRule.onNodeWithText("You’re the only admin").assertDoesNotExist()
    }

    @Test
    fun confirmShowsTheTitle() {
        show(state = DeleteAccountUiState.Confirm())
        composeRule.onNodeWithText("Delete your account?").assertIsDisplayed()
    }

    @Test
    fun deleteReachesTheCallback() {
        var confirmed = false
        show(state = DeleteAccountUiState.Confirm(), onConfirm = { confirmed = true })
        composeRule.onNodeWithText("Delete").performClick()
        assert(confirmed)
    }

    @Test
    fun cancelReachesTheCallback() {
        var dismissed = false
        show(state = DeleteAccountUiState.Confirm(), onDismiss = { dismissed = true })
        composeRule.onNodeWithText("Cancel").performClick()
        assert(dismissed)
    }

    @Test
    fun aFailedDeleteShowsTheError() {
        show(state = DeleteAccountUiState.Confirm(error = DomainError.Network()))
        composeRule
            .onNodeWithText("Can’t connect. Check your connection and try again.")
            .assertIsDisplayed()
    }

    // While deleting, the label is replaced by a spinner and Cancel is locked.
    @Test
    fun deletingLocksTheDialog() {
        show(state = DeleteAccountUiState.Confirm(deleting = true))
        composeRule.onNodeWithText("Delete").assertDoesNotExist()
        composeRule.onNodeWithText("Cancel").assertIsNotEnabled()
    }

    @Test
    fun soleAdminListsTheGroupsAndTheWarning() {
        show(state = DeleteAccountUiState.SoleAdmin(groupNames = listOf("Sunday League", "Thursday Five")))
        composeRule.onNodeWithText("You’re the only admin").assertIsDisplayed()
        composeRule.onNodeWithText("Sunday League", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Thursday Five", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText(warning).assertIsDisplayed()
    }

    @Test
    fun deleteAndArchiveReachesTheCallback() {
        var confirmed = false
        show(
            state = DeleteAccountUiState.SoleAdmin(groupNames = listOf("Sunday League")),
            onConfirm = { confirmed = true }
        )
        composeRule.onNodeWithText("Delete and archive").performClick()
        assert(confirmed)
    }

    @Test
    fun aFailedForcedDeleteReplacesTheWarning() {
        show(
            state = DeleteAccountUiState.SoleAdmin(
                groupNames = listOf("Sunday League"),
                error = DomainError.Network()
            )
        )
        composeRule
            .onNodeWithText("Can’t connect. Check your connection and try again.")
            .assertIsDisplayed()
        composeRule.onNodeWithText(warning).assertDoesNotExist()
    }
}
