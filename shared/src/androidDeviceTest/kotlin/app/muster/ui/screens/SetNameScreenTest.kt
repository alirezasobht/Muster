package app.muster.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.muster.domain.error.DomainError
import app.muster.ui.screens.setname.SetNameScreen
import app.muster.ui.theme.MusterTheme
import org.junit.Rule
import org.junit.Test

class SetNameScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(
        name: String = "",
        onContinue: () -> Unit = {},
        saving: Boolean = false,
        error: DomainError? = null
    ) {
        composeRule.setContent {
            MusterTheme {
                SetNameScreen(
                    name = name,
                    onNameChange = {},
                    onContinue = onContinue,
                    saving = saving,
                    error = error
                )
            }
        }
    }

    @Test
    fun continueIsDisabledWhileBlank() {
        show(name = "   ")
        composeRule.onNodeWithText("Continue").assertIsNotEnabled()
    }

    @Test
    fun continueIsEnabledWithAName() {
        show(name = "Alex Doyle")
        composeRule.onNodeWithText("Continue").assertIsEnabled()
    }

    @Test
    fun continueReachesTheCallback() {
        var continued = false
        show(name = "Alex Doyle", onContinue = { continued = true })
        composeRule.onNodeWithText("Continue").performClick()
        assert(continued)
    }

    @Test
    fun aFailedSaveIsReported() {
        show(name = "Alex Doyle", error = DomainError.Network())
        composeRule
            .onNodeWithText("Can’t connect. Check your connection and try again.")
            .assertIsDisplayed()
    }

    // There is no home screen without a name, so this screen has no way out.
    @Test
    fun thereIsNoBackButton() {
        show(name = "Alex Doyle")
        composeRule.onNodeWithText("Back").assertDoesNotExist()
    }
}
