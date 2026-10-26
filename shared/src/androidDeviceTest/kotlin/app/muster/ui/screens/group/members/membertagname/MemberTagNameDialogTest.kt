package app.muster.ui.screens.group.members.membertagname

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import app.muster.domain.error.DomainError
import app.muster.ui.theme.MusterTheme
import org.junit.Rule
import org.junit.Test

// A Compose Dialog with a text field never goes idle under Robolectric (and exhausts the heap), so this stays on the device.
class MemberTagNameDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun renaming(
        memberCount: Int,
        name: String = "Casual"
    ) = MemberTagNameDialogUiState(name = name, isRename = true, initialName = "Casual", memberCount = memberCount)

    private val networkError = "Can’t connect. Check your connection and try again."

    private fun show(
        state: MemberTagNameDialogUiState,
        onNameChange: (String) -> Unit = {},
        onSubmit: () -> Unit = {},
        onDismiss: () -> Unit = {}
    ) {
        composeRule.setContent {
            MusterTheme {
                MemberTagNameDialog(
                    state = state,
                    actions = MemberTagNameActions(onNameChange = onNameChange, onSubmit = onSubmit, onDismiss = onDismiss)
                )
            }
        }
    }

    @Test
    fun newShowsTitleHintAndCreate() {
        show(state = MemberTagNameDialogUiState())
        composeRule.onNodeWithText("New tag").assertIsDisplayed()
        composeRule.onNodeWithText("Only admins see tags.").assertIsDisplayed()
        composeRule.onNodeWithText("Create").assertIsDisplayed()
    }

    @Test
    fun renameShowsTitleMemberCountAndSave() {
        show(state = renaming(memberCount = 3))
        composeRule.onNodeWithText("Rename tag").assertIsDisplayed()
        composeRule.onNodeWithText("Casual").assertIsDisplayed()
        composeRule.onNodeWithText("3 members keep this tag.").assertIsDisplayed()
        composeRule.onNodeWithText("Save").assertIsDisplayed()
    }

    @Test
    fun renamingAnEmptyTagShowsTheAdminsHint() {
        show(state = renaming(memberCount = 0))
        composeRule.onNodeWithText("Only admins see tags.").assertIsDisplayed()
    }

    @Test
    fun saveIsDisabledWhileTheNameIsUnchanged() {
        show(state = renaming(memberCount = 3))
        composeRule.onNodeWithText("Save").assertIsNotEnabled()
    }

    @Test
    fun saveIsEnabledOnceTheNameChanges() {
        show(state = renaming(memberCount = 3, name = "Social"))
        composeRule.onNodeWithText("Save").assertIsEnabled()
    }

    @Test
    fun typingReachesTheCallback() {
        var typed = ""
        show(state = MemberTagNameDialogUiState(), onNameChange = { typed = it })
        composeRule.onNode(hasSetTextAction()).performTextInput("Regular")
        assert(typed == "Regular")
    }

    @Test
    fun createIsDisabledWhenBlank() {
        show(state = MemberTagNameDialogUiState(name = "  "))
        composeRule.onNodeWithText("Create").assertIsNotEnabled()
    }

    @Test
    fun createReachesTheCallback() {
        var submitted = false
        show(state = MemberTagNameDialogUiState(name = "Regular"), onSubmit = { submitted = true })
        composeRule.onNodeWithText("Create").performClick()
        assert(submitted)
    }

    @Test
    fun cancelReachesTheCallback() {
        var dismissed = false
        show(state = MemberTagNameDialogUiState(), onDismiss = { dismissed = true })
        composeRule.onNodeWithText("Cancel").performClick()
        assert(dismissed)
    }

    @Test
    fun aFieldErrorShowsUnderTheField() {
        show(state = MemberTagNameDialogUiState(nameError = DomainError.InvalidName()))
        composeRule.onNodeWithText("Enter your name.").assertIsDisplayed()
    }

    @Test
    fun aFailedSaveShowsTheFormError() {
        show(state = MemberTagNameDialogUiState(name = "Regular", error = DomainError.Network()))
        composeRule.onNodeWithText(networkError).assertIsDisplayed()
        composeRule.onNodeWithText("Create").assertIsEnabled()
    }

    // While saving, the label is replaced by a spinner and Cancel is locked.
    @Test
    fun savingLocksTheDialog() {
        show(state = MemberTagNameDialogUiState(name = "Regular", saving = true))
        composeRule.onNodeWithText("Create").assertDoesNotExist()
        composeRule.onNodeWithText("Cancel").assertIsNotEnabled()
    }
}
