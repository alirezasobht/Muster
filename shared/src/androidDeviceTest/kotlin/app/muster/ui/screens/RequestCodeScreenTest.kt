package app.muster.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import app.muster.domain.error.DomainError
import app.muster.ui.screens.signin.RequestCodeScreen
import app.muster.ui.theme.MusterTheme
import org.junit.Rule
import org.junit.Test

class RequestCodeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(
        email: String = "",
        onEmailChange: (String) -> Unit = {},
        onSendCode: () -> Unit = {},
        sending: Boolean = false,
        emailError: DomainError? = null,
        error: DomainError? = null,
        canSend: Boolean = email.isNotBlank()
    ) {
        composeRule.setContent {
            MusterTheme {
                RequestCodeScreen(
                    email = email,
                    onEmailChange = onEmailChange,
                    onSendCode = onSendCode,
                    sending = sending,
                    emailError = emailError,
                    error = error,
                    canSend = canSend
                )
            }
        }
    }

    @Test
    fun sendIsDisabledUntilAnAddressIsTyped() {
        show(email = "")
        composeRule.onNodeWithText("Send code").assertIsNotEnabled()
    }

    @Test
    fun sendIsEnabledWithAnAddress() {
        show(email = "alex.doyle@gmail.com")
        composeRule.onNodeWithText("Send code").assertIsEnabled()
    }

    @Test
    fun typingReachesTheCallback() {
        var typed: String? = null
        show(onEmailChange = { typed = it })
        // Not onNodeWithText("Email"): that matches the label above the field.
        composeRule.onNode(hasSetTextAction()).performTextInput("a")
        assert(typed == "a")
    }

    @Test
    fun tappingSendReachesTheCallback() {
        var sent = false
        show(email = "alex.doyle@gmail.com", onSendCode = { sent = true })
        composeRule.onNodeWithText("Send code").performClick()
        assert(sent)
    }

    @Test
    fun aFieldErrorIsShown() {
        show(email = "nope", emailError = DomainError.InvalidEmail())
        composeRule.onNodeWithText("That doesn’t look like an email address.").assertIsDisplayed()
    }

    // Network failures are not about the address, so they sit above the
    // button rather than under the field.
    @Test
    fun aScreenErrorIsShown() {
        show(email = "alex.doyle@gmail.com", error = DomainError.Network())
        composeRule
            .onNodeWithText("Can’t connect. Check your connection and try again.")
            .assertIsDisplayed()
    }

    // Retryable: the button is the retry, so it stays live.
    @Test
    fun aRetryableFormErrorLeavesTheButtonLive() {
        show(email = "alex.doyle@gmail.com", error = DomainError.Network())
        composeRule.onNodeWithText("Send code").assertIsEnabled()
    }

    // Terminal: retrying the same request won't help, so the button greys.
    @Test
    fun aTerminalFormErrorDisablesTheButton() {
        show(email = "alex.doyle@gmail.com", error = DomainError.RateLimited(), canSend = false)
        composeRule.onNodeWithText("Send code").assertIsNotEnabled()
    }

    // While sending, the label is replaced by a spinner.
    @Test
    fun sendingReplacesTheLabelWithASpinner() {
        show(email = "alex.doyle@gmail.com", sending = true)
        composeRule.onNodeWithText("Send code").assertDoesNotExist()
    }
}
