package app.muster.ui.screens

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import app.muster.APP_VERSION
import app.muster.domain.error.DomainError
import app.muster.ui.screens.settings.SettingsActions
import app.muster.ui.screens.settings.SettingsScreen
import app.muster.ui.screens.settings.SettingsUiState
import app.muster.ui.theme.MusterTheme
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(
        state: SettingsUiState,
        onNameChange: (String) -> Unit = {},
        onSave: () -> Unit = {},
        onRetryLoad: () -> Unit = {},
        onBack: () -> Unit = {},
        onSignOut: () -> Unit = {},
        onDeleteAccount: () -> Unit = {},
        onOpenPrivacyPolicy: () -> Unit = {}
    ) {
        composeRule.setContent {
            MusterTheme {
                SettingsScreen(
                    state = state,
                    actions = SettingsActions(
                        onNameChange = onNameChange,
                        onSave = onSave,
                        onRetryLoad = onRetryLoad,
                        onBack = onBack,
                        onSignOut = onSignOut,
                        onDeleteAccount = onDeleteAccount,
                        onOpenPrivacyPolicy = onOpenPrivacyPolicy
                    ),
                    spinnerDelayMillis = 0
                )
            }
        }
    }

    @Test
    fun backReachesTheCallback() {
        var back = false
        show(state = SettingsUiState.Loading, onBack = { back = true })
        composeRule.onNodeWithContentDescription("Back").performClick()
        assert(back)
    }

    @Test
    fun loadingShowsTheAppBarButNoForm() {
        show(state = SettingsUiState.Loading)
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
        composeRule.onNodeWithText("Save").assertDoesNotExist()
    }

    @Test
    fun failedShowsTheErrorMessage() {
        show(state = SettingsUiState.Error(DomainError.Network()))
        composeRule.onNodeWithText("Couldn’t load your profile").assertIsDisplayed()
        composeRule
            .onNodeWithText("Can’t connect. Check your connection and try again.")
            .assertIsDisplayed()
    }

    @Test
    fun failedTryAgainReachesTheCallback() {
        var retried = false
        show(state = SettingsUiState.Error(DomainError.Network()), onRetryLoad = { retried = true })
        composeRule.onNodeWithText("Try again").performClick()
        assert(retried)
    }

    @Test
    fun saveIsDisabledWhileBlank() {
        show(state = SettingsUiState.Success(name = "  ", email = "alex.doyle@gmail.com"))
        composeRule.onNodeWithText("Save").assertIsNotEnabled()
    }

    @Test
    fun saveIsEnabledWithAName() {
        show(state = SettingsUiState.Success(name = "Alex Doyle", email = "alex.doyle@gmail.com"))
        composeRule.onNodeWithText("Save").assertIsEnabled()
    }

    @Test
    fun tappingSaveReachesTheCallback() {
        var saved = false
        show(
            state = SettingsUiState.Success(name = "Alex Doyle", email = "alex.doyle@gmail.com"),
            onSave = { saved = true }
        )
        composeRule.onNodeWithText("Save").performClick()
        assert(saved)
    }

    @Test
    fun editingTheNameReachesTheCallback() {
        var typed: String? = null
        show(
            state = SettingsUiState.Success(name = "", email = "alex.doyle@gmail.com"),
            onNameChange = { typed = it }
        )
        // Not onNodeWithText("Name"): that matches the label above the field.
        composeRule.onNode(hasSetTextAction()).performTextInput("a")
        assert(typed == "a")
    }

    // The email field must not be editable — regression check for the
    // readOnly styling on MusterTextField.
    @Test
    fun theEmailFieldIsNotEditable() {
        show(state = SettingsUiState.Success(name = "Alex Doyle", email = "alex.doyle@gmail.com"))
        composeRule.onNodeWithText("alex.doyle@gmail.com").assertIsDisplayed()
        composeRule.onAllNodes(hasSetTextAction()).assertCountEquals(1)
    }

    @Test
    fun signOutReachesTheCallback() {
        var signedOut = false
        show(
            state = SettingsUiState.Success(name = "Alex Doyle", email = "alex.doyle@gmail.com"),
            onSignOut = { signedOut = true }
        )
        composeRule.onNodeWithText("Sign out").performClick()
        assert(signedOut)
    }

    @Test
    fun deleteAccountReachesTheCallback() {
        var deleting = false
        show(
            state = SettingsUiState.Success(name = "Alex Doyle", email = "alex.doyle@gmail.com"),
            onDeleteAccount = { deleting = true }
        )
        composeRule.onNodeWithText("Delete account").performClick()
        assert(deleting)
    }

    @Test
    fun privacyPolicyReachesTheCallback() {
        var opened = false
        show(
            state = SettingsUiState.Success(name = "Alex Doyle", email = "alex.doyle@gmail.com"),
            onOpenPrivacyPolicy = { opened = true }
        )
        composeRule.onNodeWithText("Privacy policy").performClick()
        assert(opened)
    }

    @Test
    fun aSaveErrorIsShown() {
        show(
            state = SettingsUiState.Success(
                name = "",
                email = "alex.doyle@gmail.com",
                saveError = DomainError.InvalidName()
            )
        )
        composeRule.onNodeWithText("Enter your name.").assertIsDisplayed()
    }

    // While saving, the label is replaced by a spinner.
    @Test
    fun savingReplacesTheLabelWithASpinner() {
        show(
            state = SettingsUiState.Success(
                name = "Alex Doyle",
                email = "alex.doyle@gmail.com",
                saving = true
            )
        )
        composeRule.onNodeWithText("Save").assertDoesNotExist()
    }

    @Test
    fun theVersionIsShown() {
        show(state = SettingsUiState.Success(name = "Alex Doyle", email = "alex.doyle@gmail.com"))
        composeRule.onNodeWithText("Version $APP_VERSION").assertIsDisplayed()
    }
}
