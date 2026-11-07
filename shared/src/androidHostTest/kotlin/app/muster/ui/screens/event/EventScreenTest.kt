package app.muster.ui.screens.event

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.muster.domain.error.DomainError
import app.muster.domain.model.RsvpStatus
import app.muster.ui.theme.MusterTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EventScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(
        state: EventUiState,
        onBack: () -> Unit = {},
        onRsvp: (RsvpStatus) -> Unit = {},
        onRosterAction: (String, RosterAction) -> Unit = { _, _ -> },
        onReorderStandby: (List<String>) -> Unit = {},
        onRetry: () -> Unit = {},
        onOverflowAction: (EventOverflowAction) -> Unit = {},
        onLockToggled: (Boolean) -> Unit = {},
        onDialogConfirmed: () -> Unit = {},
        onDialogDismissed: () -> Unit = {}
    ) {
        composeRule.setContent {
            MusterTheme {
                EventScreen(
                    state = state,
                    actions = EventActions(
                        onBack = onBack,
                        onRsvp = onRsvp,
                        onRosterAction = onRosterAction,
                        onReorderStandby = onReorderStandby,
                        onRetry = onRetry,
                        onOverflowAction = onOverflowAction,
                        onLockToggled = onLockToggled,
                        onDialogConfirmed = onDialogConfirmed,
                        onDialogDismissed = onDialogDismissed
                    ),
                    spinnerDelayMillis = 0
                )
            }
        }
    }

    @Test
    fun theAppBarShowsGroupNameAndTitleEvenWhileLoading() {
        show(state = EventUiState.Loading(TestSummary))
        composeRule.onNodeWithText("WESTGATE WEDNESDAY 7S").assertIsDisplayed()
        composeRule.onNodeWithText("Weekly 7-a-side").assertIsDisplayed()
    }

    @Test
    fun loadingShowsNoMoreOptions() {
        show(state = EventUiState.Loading(TestSummary))
        composeRule.onNodeWithContentDescription("More options").assertDoesNotExist()
    }

    @Test
    fun loadingShowsTheCarriedOverDetailsImmediately() {
        show(state = EventUiState.Loading(TestSummary))
        composeRule.onNodeWithText("Wed 17 Sep · 7:00 pm").assertIsDisplayed()
        composeRule.onNodeWithText("Westgate Pitch 2").assertIsDisplayed()
        composeRule.onNodeWithText("of 10 slots").assertIsDisplayed()
        composeRule.onNodeWithText("6 open").assertIsDisplayed()
    }

    @Test
    fun loadingHasNoLegendYet() {
        show(state = EventUiState.Loading(TestSummary))
        composeRule.onNodeWithText("3 in").assertDoesNotExist()
    }

    @Test
    fun loadingHasNoRosterYet() {
        show(state = EventUiState.Loading(TestSummary))
        composeRule.onNodeWithText("Players 3").assertDoesNotExist()
    }

    @Test
    fun errorShowsTheDetailsAndTheErrorMessage() {
        show(state = EventUiState.Error(TestSummary, DomainError.Network()))
        composeRule.onNodeWithText("Westgate Pitch 2").assertIsDisplayed()
        composeRule.onNodeWithText("Couldn’t load this event").assertIsDisplayed()
    }

    @Test
    fun errorRetryReachesTheCallback() {
        var retried = false
        show(state = EventUiState.Error(TestSummary, DomainError.Network()), onRetry = { retried = true })
        composeRule.onNodeWithText("Try again").performClick()
        assert(retried)
    }

    // Cancel, remove and resend all ask for confirmation first. Only the
    // In / Out toggles fire straight away.
    @Test
    fun cancellingAnInviteAsksForConfirmationBeforeCallingBack() {
        var picked: Pair<String, RosterAction>? = null
        show(
            state = TestSuccess.copy(roster = listOf(pendingRow(listOf(RosterAction.CancelInvite)))),
            onRosterAction = { id, action -> picked = id to action }
        )
        composeRule.onNodeWithText("Pending").performClick()
        composeRule.onNodeWithText("Cancel invite").performClick()

        composeRule.onNodeWithText("Cancel invite to Tomás Neale?").assertIsDisplayed()
        assert(picked == null)

        composeRule.onNodeWithText("Cancel invite").performClick()
        assert(picked == "p2" to RosterAction.CancelInvite)
    }

    @Test
    fun dismissingTheCancelInviteConfirmationDoesNotCallBack() {
        var picked: Pair<String, RosterAction>? = null
        show(
            state = TestSuccess.copy(roster = listOf(pendingRow(listOf(RosterAction.CancelInvite)))),
            onRosterAction = { id, action -> picked = id to action }
        )
        composeRule.onNodeWithText("Pending").performClick()
        composeRule.onNodeWithText("Cancel invite").performClick()
        // "Keep invite", not "Cancel": both buttons would otherwise read
        // "Cancel", one for the invite and one for the dialog itself.
        composeRule.onNodeWithText("Keep invite").performClick()

        composeRule.onNodeWithText("Cancel invite to Tomás Neale?").assertDoesNotExist()
        assert(picked == null)
    }

    @Test
    fun removingFromTheEventAsksForConfirmationBeforeCallingBack() {
        var picked: Pair<String, RosterAction>? = null
        show(
            state = TestSuccess.copy(
                roster = listOf(
                    RosterRow(id = "p1", name = "Alex Doyle", status = RsvpStatus.In, actions = listOf(RosterAction.RemoveFromEvent))
                )
            ),
            onRosterAction = { id, action -> picked = id to action }
        )
        composeRule.onNodeWithText("In").performClick()
        composeRule.onNodeWithText("Remove from event").performClick()

        composeRule.onNodeWithText("Remove Alex Doyle from this event?").assertIsDisplayed()
        assert(picked == null)

        composeRule.onNodeWithText("Remove").performClick()
        assert(picked == "p1" to RosterAction.RemoveFromEvent)
    }

    @Test
    fun removingFromTheListAsksForConfirmationBeforeCallingBack() {
        var picked: Pair<String, RosterAction>? = null
        show(
            state = TestSuccess.copy(
                roster = listOf(
                    RosterRow(id = "p3", name = "Marcus Keane", status = RsvpStatus.Out, actions = listOf(RosterAction.RemoveFromList))
                )
            ),
            onRosterAction = { id, action -> picked = id to action }
        )
        composeRule.onNodeWithText("Out").performClick()
        composeRule.onNodeWithText("Remove from list").performClick()

        composeRule.onNodeWithText("Remove Marcus Keane from this event?").assertIsDisplayed()
        assert(picked == null)

        composeRule.onNodeWithText("Remove").performClick()
        assert(picked == "p3" to RosterAction.RemoveFromList)
    }

    @Test
    fun resendingAnInviteAsksForConfirmationBeforeCallingBack() {
        var picked: Pair<String, RosterAction>? = null
        show(
            state = TestSuccess.copy(roster = listOf(pendingRow(listOf(RosterAction.ResendInvite)))),
            onRosterAction = { id, action -> picked = id to action }
        )
        composeRule.onNodeWithText("Pending").performClick()
        composeRule.onNodeWithText("Resend invite").performClick()

        composeRule.onNodeWithText("Resend invite to Tomás Neale?").assertIsDisplayed()
        assert(picked == null)

        composeRule.onNodeWithText("Resend").performClick()
        assert(picked == "p2" to RosterAction.ResendInvite)
    }

    @Test
    fun aLockedEventShowsBothStripLines() {
        show(state = TestLocked)
        composeRule.onNodeWithText("Locked by Ali").assertIsDisplayed()
        composeRule.onNodeWithText("No changes until an admin unlocks it").assertIsDisplayed()
        composeRule.onAllNodesWithText("Locked").assertCountEquals(1)
    }

    @Test
    fun membersSeeTheLockedStripButNoSwitch() {
        show(state = TestLocked.copy(isAdmin = false, lockFeatureFlag = false))
        composeRule.onNodeWithText("Locked by Ali").assertIsDisplayed()
        composeRule.onNodeWithText("Locked").assertDoesNotExist()
        composeRule.onNode(isToggleable()).assertDoesNotExist()
        composeRule.onNodeWithContentDescription("More options").assertDoesNotExist()
    }

    @Test
    fun aStartedEventShowsTheStartedStripAndNoLock() {
        show(state = TestLocked.copy(phase = EventPhase.Started(atDisplay = "7:00 pm")))
        composeRule.onNodeWithText("Started at 7:00 pm · no more changes").assertIsDisplayed()
        composeRule.onNodeWithText("Locked by Ali").assertDoesNotExist()
        composeRule.onNodeWithText("Locked").assertDoesNotExist()
        composeRule.onNode(isToggleable()).assertDoesNotExist()
    }

    @Test
    fun lockedHidesAddAndRowMenus() {
        show(state = TestLocked.copy(roster = listOf(pendingRow(listOf(RosterAction.SetIn, RosterAction.CancelInvite)))))
        composeRule.onNodeWithText("›").assertDoesNotExist()
        composeRule.onNodeWithText("+ Add").assertDoesNotExist()
    }

    @Test
    fun lockingClosesAnOpenRosterConfirmation() {
        val unlocked = TestSuccess.copy(myStatus = null, roster = listOf(pendingRow(listOf(RosterAction.CancelInvite))))
        var state by mutableStateOf(unlocked)
        composeRule.setContent {
            MusterTheme {
                EventScreen(
                    state = state,
                    actions = EventActions(onBack = {}, onRsvp = {}, onRosterAction = { _, _ -> }, onReorderStandby = {}, onRetry = {}),
                    spinnerDelayMillis = 0
                )
            }
        }
        composeRule.onNodeWithText("Pending").performClick()
        composeRule.onNodeWithText("Cancel invite").performClick()
        composeRule.onNodeWithText("Cancel invite to Tomás Neale?").assertIsDisplayed()

        state = unlocked.copy(phase = EventPhase.Locked(byName = "Ali"))
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Cancel invite to Tomás Neale?").assertDoesNotExist()
    }

    @Test
    fun lockedEmptyRosterOffersNoAddPlayers() {
        show(state = TestLocked.copy(roster = emptyList(), myStatus = null))
        composeRule.onNodeWithText("No one invited yet").assertIsDisplayed()
        composeRule.onNodeWithText("+ Add players").assertDoesNotExist()
    }

    @Test
    fun membersSeeNoAddPill() {
        show(state = TestSuccess.copy(isAdmin = false))
        composeRule.onNodeWithText("Players 3").assertIsDisplayed()
        composeRule.onNodeWithText("+ Add").assertDoesNotExist()
    }

    @Test
    fun membersEmptyRosterOffersNoAddPlayers() {
        show(state = TestSuccess.copy(isAdmin = false, roster = emptyList()))
        composeRule.onNodeWithText("No one invited yet").assertIsDisplayed()
        composeRule.onNodeWithText("+ Add players").assertDoesNotExist()
    }

    @Test
    fun anUnlockedUnstartedAdminMenuHoldsEditAndDelete() {
        show(state = TestSuccess)
        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Edit event").assertIsDisplayed()
        composeRule.onNodeWithText("Delete event").assertIsDisplayed()
    }

    @Test
    fun aLockedAdminMenuHoldsDeleteAlone() {
        show(state = TestLocked)
        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Delete event").assertIsDisplayed()
        composeRule.onNodeWithText("Edit event").assertDoesNotExist()
    }

    @Test
    fun aCancelledEventThatHasStartedShowsTheCancelledStripAndNoLock() {
        show(
            state = TestCancelled.copy(
                phase = EventPhase.Cancelled(byName = "Ali", started = true),
                lockFeatureFlag = true
            )
        )
        composeRule.onNodeWithText("Cancelled by Ali").assertIsDisplayed()
        composeRule.onNodeWithText("Started at 7:00 pm · no more changes").assertDoesNotExist()
        composeRule.onNode(isToggleable()).assertDoesNotExist()
    }

    @Test
    fun cancelledHidesAddAndRowMenus() {
        show(state = TestCancelled.copy(roster = listOf(pendingRow(listOf(RosterAction.SetIn, RosterAction.CancelInvite)))))
        composeRule.onNodeWithText("›").assertDoesNotExist()
        composeRule.onNodeWithText("+ Add").assertDoesNotExist()
    }
}
