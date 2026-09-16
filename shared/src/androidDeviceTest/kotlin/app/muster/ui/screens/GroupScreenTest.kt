package app.muster.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.muster.domain.error.DomainError
import app.muster.domain.model.GroupRole
import app.muster.ui.screens.group.GroupActions
import app.muster.ui.screens.group.GroupOverflowAction
import app.muster.ui.screens.group.GroupScreen
import app.muster.ui.screens.group.GroupUiState
import app.muster.ui.screens.group.OverflowDialogState
import app.muster.ui.theme.MusterTheme
import org.junit.Rule
import org.junit.Test

// The overflow dialog itself is driven entirely by `overflowDialog` on
// state — the screen never opens or closes one on its own. Tests either
// check the menu asks the ViewModel to open one (onXRequested fires), or
// preset `overflowDialog` and check the dialog it renders from that state.
class GroupScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(
        myRole: GroupRole = GroupRole.Member,
        overflowDialog: OverflowDialogState? = null,
        onOverflowActionRequested: (GroupOverflowAction) -> Unit = {},
        onOverflowConfirmed: () -> Unit = {},
        onOverflowDialogDismissed: () -> Unit = {}
    ) {
        composeRule.setContent {
            MusterTheme {
                GroupScreen(
                    state = GroupUiState.Success(
                        groupName = "Westgate Wednesday 7s",
                        myRole = myRole,
                        overflowDialog = overflowDialog
                    ),
                    actions = GroupActions(
                        onBack = {},
                        onTabSelected = {},
                        onRetry = {},
                        onOverflowActionRequested = onOverflowActionRequested,
                        onOverflowConfirmed = onOverflowConfirmed,
                        onOverflowDialogDismissed = onOverflowDialogDismissed
                    )
                )
            }
        }
    }

    private fun openOverflowMenu() {
        composeRule.onNodeWithContentDescription("More options").performClick()
    }

    // Loading has no Success state to read overflowActions from, so
    // GroupScreen falls back to emptyList() — no ⋮ at all, not an empty menu.
    @Test
    fun aScreenWithNoOverflowActionsShowsNoOverflowButton() {
        composeRule.setContent {
            MusterTheme {
                GroupScreen(
                    state = GroupUiState.Loading("Westgate Wednesday 7s"),
                    actions = GroupActions(
                        onBack = {},
                        onTabSelected = {},
                        onRetry = {},
                        onOverflowActionRequested = {},
                        onOverflowConfirmed = {},
                        onOverflowDialogDismissed = {}
                    )
                )
            }
        }
        composeRule.onNodeWithContentDescription("More options").assertDoesNotExist()
    }

    @Test
    fun aMemberDoesNotSeeArchiveInTheOverflowMenu() {
        show(myRole = GroupRole.Member)
        openOverflowMenu()
        composeRule.onNodeWithText("Archive group").assertDoesNotExist()
        composeRule.onNodeWithText("Leave group").assertIsDisplayed()
    }

    @Test
    fun anAdminSeesArchiveAboveLeaveInTheOverflowMenu() {
        show(myRole = GroupRole.Admin)
        openOverflowMenu()
        composeRule.onNodeWithText("Archive group").assertIsDisplayed()
        composeRule.onNodeWithText("Leave group").assertIsDisplayed()
    }

    @Test
    fun tappingLeaveGroupReachesTheRequestedCallbackWithLeave() {
        var requested: GroupOverflowAction? = null
        show(onOverflowActionRequested = { requested = it })
        openOverflowMenu()
        composeRule.onNodeWithText("Leave group").performClick()
        assert(requested == GroupOverflowAction.Leave)
    }

    @Test
    fun tappingArchiveGroupReachesTheRequestedCallbackWithArchive() {
        var requested: GroupOverflowAction? = null
        show(myRole = GroupRole.Admin, onOverflowActionRequested = { requested = it })
        openOverflowMenu()
        composeRule.onNodeWithText("Archive group").performClick()
        assert(requested == GroupOverflowAction.Archive)
    }

    @Test
    fun anOpenLeaveDialogShowsItsCopy() {
        show(overflowDialog = OverflowDialogState(GroupOverflowAction.Leave))
        composeRule.onNodeWithText("Leave this group?").assertIsDisplayed()
    }

    @Test
    fun anOpenArchiveDialogShowsItsCopy() {
        show(overflowDialog = OverflowDialogState(GroupOverflowAction.Archive))
        composeRule.onNodeWithText("Archive this group?").assertIsDisplayed()
    }

    @Test
    fun confirmingTheOpenDialogReachesTheConfirmedCallback() {
        var confirmed = false
        show(
            overflowDialog = OverflowDialogState(GroupOverflowAction.Leave),
            onOverflowConfirmed = { confirmed = true }
        )
        composeRule.onNodeWithText("Leave").performClick()
        assert(confirmed)
    }

    @Test
    fun cancellingTheOpenDialogReachesTheDismissedCallback() {
        var dismissed = false
        show(
            overflowDialog = OverflowDialogState(GroupOverflowAction.Leave),
            onOverflowDialogDismissed = { dismissed = true }
        )
        composeRule.onNodeWithText("Cancel").performClick()
        assert(dismissed)
    }

    // Not a screen-level banner — the failed request's own dialog carries the
    // error, and its confirm button is the retry.
    @Test
    fun anOverflowErrorIsShownInsideTheOpenDialog() {
        show(
            overflowDialog = OverflowDialogState(
                action = GroupOverflowAction.Leave,
                error = DomainError.LastAdmin()
            )
        )
        composeRule.onNodeWithText("A group needs at least one admin. Promote someone else first.").assertIsDisplayed()
    }

    @Test
    fun aLoadingConfirmReplacesTheLabelAndDisablesCancel() {
        show(overflowDialog = OverflowDialogState(action = GroupOverflowAction.Archive, inFlight = true))
        composeRule.onNodeWithText("Archive this group?").assertIsDisplayed()
        composeRule.onNodeWithText("Archive").assertDoesNotExist()
        composeRule.onNodeWithText("Cancel").assertIsNotEnabled()
    }
}
