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
import app.muster.ui.screens.addbyemail.AddMemberByEmailScreen
import app.muster.ui.theme.MusterTheme
import org.junit.Rule
import org.junit.Test

class AddMemberByEmailScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(
        groupName: String = "Westgate Wednesday 7s",
        email: String = "",
        onEmailChange: (String) -> Unit = {},
        onInvite: () -> Unit = {},
        onClose: () -> Unit = {},
        sending: Boolean = false,
        emailError: DomainError? = null,
        error: DomainError? = null,
        canInvite: Boolean = email.isNotBlank(),
        invited: Boolean = false,
        onInviteMore: () -> Unit = {}
    ) {
        composeRule.setContent {
            MusterTheme {
                AddMemberByEmailScreen(
                    groupName = groupName,
                    email = email,
                    onEmailChange = onEmailChange,
                    onInvite = onInvite,
                    onClose = onClose,
                    sending = sending,
                    emailError = emailError,
                    error = error,
                    canInvite = canInvite,
                    invited = invited,
                    onInviteMore = onInviteMore
                )
            }
        }
    }

    @Test
    fun theGroupNameIsShownInTheAppBar() {
        show(groupName = "Westgate Wednesday 7s")
        composeRule.onNodeWithText("Westgate Wednesday 7s").assertIsDisplayed()
    }

    @Test
    fun closeReachesTheCallback() {
        var closed = false
        show(onClose = { closed = true })
        composeRule.onNodeWithContentDescription("Close").performClick()
        assert(closed)
    }

    @Test
    fun inviteIsDisabledWhileBlank() {
        show(email = "   ")
        composeRule.onNodeWithText("Send invite").assertIsNotEnabled()
    }

    @Test
    fun inviteIsEnabledWithAnAddress() {
        show(email = "priya.n@gmail.com")
        composeRule.onNodeWithText("Send invite").assertIsEnabled()
    }

    @Test
    fun inviteReachesTheCallback() {
        var invited = false
        show(email = "priya.n@gmail.com", onInvite = { invited = true })
        composeRule.onNodeWithText("Send invite").performClick()
        assert(invited)
    }

    @Test
    fun editingTheEmailReachesTheCallback() {
        var typed: String? = null
        show(onEmailChange = { typed = it })
        composeRule.onNode(hasSetTextAction()).performTextInput("a")
        assert(typed == "a")
    }

    @Test
    fun aFieldErrorIsShownUnderTheField() {
        show(email = "not-an-email", emailError = DomainError.InvalidEmail())
        composeRule.onNodeWithText("That doesn’t look like an email address.").assertIsDisplayed()
        composeRule.onNodeWithText("Send invite").assertIsEnabled()
    }

    // Retryable: the button is the retry, so it stays live.
    @Test
    fun aRetryableFormErrorLeavesTheButtonLive() {
        show(email = "priya.n@gmail.com", error = DomainError.Network())
        composeRule.onNodeWithText("Can’t connect. Check your connection and try again.").assertIsDisplayed()
        composeRule.onNodeWithText("Send invite").assertIsEnabled()
    }

    // While sending, the label is replaced by a spinner.
    @Test
    fun sendingReplacesTheLabelWithASpinner() {
        show(email = "priya.n@gmail.com", sending = true)
        composeRule.onNodeWithText("Send invite").assertDoesNotExist()
    }

    @Test
    fun theSuccessDialogShowsTheConfirmation() {
        show(invited = true)
        composeRule.onNodeWithText("Invite sent").assertIsDisplayed()
    }

    @Test
    fun inviteMoreOnTheDialogReachesTheCallback() {
        var invitedMore = false
        show(invited = true, onInviteMore = { invitedMore = true })
        composeRule.onNodeWithText("Invite more").performClick()
        assert(invitedMore)
    }

    // Close doubles as the dialog's dismiss action, popping back to Members.
    @Test
    fun closeOnTheDialogReachesTheCallback() {
        var closed = false
        show(invited = true, onClose = { closed = true })
        composeRule.onNodeWithText("Close").performClick()
        assert(closed)
    }
}
