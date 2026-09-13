package app.muster.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.muster.domain.error.DomainError
import app.muster.ui.screens.signin.EnterCodeScreen
import app.muster.ui.theme.MusterTheme
import org.junit.Rule
import org.junit.Test

class EnterCodeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(
        code: String = "",
        onContinue: () -> Unit = {},
        onResend: () -> Unit = {},
        onBack: () -> Unit = {},
        resendInSeconds: Int = 0,
        verifying: Boolean = false,
        error: DomainError? = null
    ) {
        composeRule.setContent {
            MusterTheme {
                EnterCodeScreen(
                    email = "alex.doyle@gmail.com",
                    code = code,
                    onCodeChange = {},
                    onContinue = onContinue,
                    onResend = onResend,
                    onBack = onBack,
                    resendInSeconds = resendInSeconds,
                    verifying = verifying,
                    error = error
                )
            }
        }
    }

    @Test
    fun theAddressIsShown() {
        show()
        composeRule.onNodeWithText("Sent to alex.doyle@gmail.com").assertIsDisplayed()
    }

    @Test
    fun continueIsDisabledOnAPartialCode() {
        show(code = "418")
        composeRule.onNodeWithText("Continue").assertIsNotEnabled()
    }

    @Test
    fun continueIsEnabledOnASixDigitCode() {
        show(code = "418027")
        composeRule.onNodeWithText("Continue").assertIsEnabled()
    }

    @Test
    fun theCountdownIsShownAsAClock() {
        show(resendInSeconds = 42)
        composeRule.onNodeWithText("Resend code in 0:42").assertIsDisplayed()
    }

    @Test
    fun resendBecomesTappableAtZero() {
        var resent = false
        show(resendInSeconds = 0, onResend = { resent = true })
        composeRule.onNodeWithText("Resend code").performClick()
        assert(resent)
    }

    @Test
    fun backReachesTheCallback() {
        var back = false
        show(onBack = { back = true })
        composeRule.onNodeWithContentDescription("Back").performClick()
        assert(back)
    }

    @Test
    fun aWrongCodeIsReported() {
        show(code = "418027", error = DomainError.InvalidCode())
        composeRule.onNodeWithText("That code is wrong or has expired.").assertIsDisplayed()
    }

    // Verifying stays set after success while routing loads the profile, so
    // back must not offer a way out mid-flight.
    @Test
    fun backIsDisabledWhileVerifying() {
        show(code = "418027", verifying = true)
        composeRule.onNodeWithContentDescription("Back").assertIsNotEnabled()
    }
}
