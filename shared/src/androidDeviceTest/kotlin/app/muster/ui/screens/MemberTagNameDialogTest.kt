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
import app.muster.ui.screens.group.members.tagname.TagNameActions
import app.muster.ui.screens.group.members.tagname.TagNameDialog
import app.muster.ui.screens.group.members.tagname.TagNameDialogUiState
import app.muster.ui.screens.group.members.tagname.TagNameMode
import app.muster.ui.theme.MusterTheme
import org.junit.Rule
import org.junit.Test

class MemberTagNameDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val networkError = "Can’t connect. Check your connection and try again."

    private fun show(
        state: TagNameDialogUiState,
        onNameChange: (String) -> Unit = {},
        onSubmit: () -> Unit = {},
        onDismiss: () -> Unit = {}
    ) {
        composeRule.setContent {
            MusterTheme {
                TagNameDialog(
                    state = state,
                    actions = TagNameActions(onNameChange = onNameChange, onSubmit = onSubmit, onDismiss = onDismiss)
                )
            }
        }
    }

    @Test
    fun hiddenShowsNothing() {
        show(state = TagNameDialogUiState.Hidden)
        composeRule.onNodeWithText("New tag").assertDoesNotExist()
        composeRule.onNodeWithText("Rename tag").assertDoesNotExist()
    }

    @Test
    fun newShowsTitleHintAndCreate() {
        show(state = TagNameDialogUiState.Open(mode = TagNameMode.New))
        composeRule.onNodeWithText("New tag").assertIsDisplayed()
        composeRule.onNodeWithText("Only admins see tags.").assertIsDisplayed()
        composeRule.onNodeWithText("Create").assertIsDisplayed()
    }

    @Test
    fun renameShowsTitleMemberCountAndSave() {
        show(state = TagNameDialogUiState.Open(mode = TagNameMode.Rename(tagId = "t1", memberCount = 3), name = "Casual"))
        composeRule.onNodeWithText("Rename tag").assertIsDisplayed()
        composeRule.onNodeWithText("Casual").assertIsDisplayed()
        composeRule.onNodeWithText("3 members keep this tag.").assertIsDisplayed()
        composeRule.onNodeWithText("Save").assertIsDisplayed()
    }

    @Test
    fun renamingAnEmptyTagShowsTheAdminsHint() {
        show(state = TagNameDialogUiState.Open(mode = TagNameMode.Rename(tagId = "t1", memberCount = 0), name = "Casual"))
        composeRule.onNodeWithText("Only admins see tags.").assertIsDisplayed()
    }

    @Test
    fun typingReachesTheCallback() {
        var typed = ""
        show(state = TagNameDialogUiState.Open(mode = TagNameMode.New), onNameChange = { typed = it })
        composeRule.onNode(hasSetTextAction()).performTextInput("Regular")
        assert(typed == "Regular")
    }

    @Test
    fun createIsDisabledWhenBlank() {
        show(state = TagNameDialogUiState.Open(mode = TagNameMode.New, name = "  "))
        composeRule.onNodeWithText("Create").assertIsNotEnabled()
    }

    @Test
    fun createReachesTheCallback() {
        var submitted = false
        show(state = TagNameDialogUiState.Open(mode = TagNameMode.New, name = "Regular"), onSubmit = { submitted = true })
        composeRule.onNodeWithText("Create").performClick()
        assert(submitted)
    }

    @Test
    fun cancelReachesTheCallback() {
        var dismissed = false
        show(state = TagNameDialogUiState.Open(mode = TagNameMode.New), onDismiss = { dismissed = true })
        composeRule.onNodeWithText("Cancel").performClick()
        assert(dismissed)
    }

    @Test
    fun aFieldErrorShowsUnderTheField() {
        show(state = TagNameDialogUiState.Open(mode = TagNameMode.New, nameError = DomainError.InvalidName()))
        composeRule.onNodeWithText("Enter your name.").assertIsDisplayed()
    }

    @Test
    fun aFailedSaveShowsTheFormError() {
        show(state = TagNameDialogUiState.Open(mode = TagNameMode.New, name = "Regular", error = DomainError.Network()))
        composeRule.onNodeWithText(networkError).assertIsDisplayed()
        composeRule.onNodeWithText("Create").assertIsEnabled()
    }

    // While saving, the label is replaced by a spinner and Cancel is locked.
    @Test
    fun savingLocksTheDialog() {
        show(state = TagNameDialogUiState.Open(mode = TagNameMode.New, name = "Regular", saving = true))
        composeRule.onNodeWithText("Create").assertDoesNotExist()
        composeRule.onNodeWithText("Cancel").assertIsNotEnabled()
    }
}
