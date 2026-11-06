package app.muster.ui.screens.event

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.text.TextLayoutResult
import app.muster.domain.error.DomainError
import app.muster.domain.model.RsvpStatus
import app.muster.ui.theme.MusterTheme
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
class EventScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val summary = EventSummary(
        groupId = "group-1",
        eventId = "event-1",
        groupName = "Westgate Wednesday 7s",
        title = "Weekly 7-a-side",
        date = "Wed 17 Sep · 7:00 pm",
        location = "Westgate Pitch 2",
        capacity = 10,
        inCount = 3,
        pendingCount = 1
    )

    private val roster = listOf(
        RosterRow(id = "p1", name = "Alex Doyle", status = RsvpStatus.In),
        RosterRow(id = "p2", name = "Tomás Neale", status = RsvpStatus.Pending),
        RosterRow(id = "p3", name = "Marcus Keane", status = RsvpStatus.Out, isSelf = true)
    )

    private val successState = EventUiState.Success(
        summary = summary,
        isAdmin = true,
        myStatus = RsvpStatus.Out,
        roster = roster
    )

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
    fun backReachesTheCallback() {
        var back = false
        show(state = successState, onBack = { back = true })
        composeRule.onNodeWithContentDescription("Back").performClick()
        assert(back)
    }

    @Test
    fun theAppBarShowsGroupNameAndTitleEvenWhileLoading() {
        show(state = EventUiState.Loading(summary))
        composeRule.onNodeWithText("WESTGATE WEDNESDAY 7S").assertIsDisplayed()
        composeRule.onNodeWithText("Weekly 7-a-side").assertIsDisplayed()
    }

    @Test
    fun tappingEditEventReachesTheCallbackWithEdit() {
        var picked: EventOverflowAction? = null
        show(
            state = successState,
            onOverflowAction = { picked = it }
        )
        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Edit event").performClick()
        assert(picked == EventOverflowAction.Edit)
    }

    @Test
    fun noOverflowActionsShowsNoMoreOptions() {
        show(state = successState.copy(isAdmin = false))
        composeRule.onNodeWithContentDescription("More options").assertDoesNotExist()
    }

    @Test
    fun loadingShowsNoMoreOptions() {
        show(state = EventUiState.Loading(summary))
        composeRule.onNodeWithContentDescription("More options").assertDoesNotExist()
    }

    @Test
    fun loadingShowsTheCarriedOverDetailsImmediately() {
        show(state = EventUiState.Loading(summary))
        composeRule.onNodeWithText("Wed 17 Sep · 7:00 pm").assertIsDisplayed()
        composeRule.onNodeWithText("Westgate Pitch 2").assertIsDisplayed()
        composeRule.onNodeWithText("of 10 slots").assertIsDisplayed()
        composeRule.onNodeWithText("6 open").assertIsDisplayed()
    }

    @Test
    fun loadingHasNoLegendYet() {
        show(state = EventUiState.Loading(summary))
        composeRule.onNodeWithText("3 in").assertDoesNotExist()
    }

    @Test
    fun loadingHasNoRosterYet() {
        show(state = EventUiState.Loading(summary))
        composeRule.onNodeWithText("Players 3").assertDoesNotExist()
    }

    @Test
    fun errorShowsTheDetailsAndTheErrorMessage() {
        show(state = EventUiState.Error(summary, DomainError.Network()))
        composeRule.onNodeWithText("Westgate Pitch 2").assertIsDisplayed()
        composeRule.onNodeWithText("Couldn’t load this event").assertIsDisplayed()
    }

    @Test
    fun errorRetryReachesTheCallback() {
        var retried = false
        show(state = EventUiState.Error(summary, DomainError.Network()), onRetry = { retried = true })
        composeRule.onNodeWithText("Try again").performClick()
        assert(retried)
    }

    @Test
    fun unansweredShowsTheQuestionAndTwoButtons() {
        show(state = successState.copy(myStatus = RsvpStatus.Pending))
        composeRule.onNodeWithText("Can you play?").assertIsDisplayed()
        composeRule.onNodeWithText("I’m in").assertIsDisplayed()
        composeRule.onNodeWithText("Can’t make it").assertIsDisplayed()
    }

    @Test
    fun tappingImInReachesOnRsvpWithIn() {
        var status: RsvpStatus? = null
        show(state = successState.copy(myStatus = RsvpStatus.Pending), onRsvp = { status = it })
        composeRule.onNodeWithText("I’m in").performClick()
        assert(status == RsvpStatus.In)
    }

    @Test
    fun tappingCantMakeItReachesOnRsvpWithOut() {
        var status: RsvpStatus? = null
        show(state = successState.copy(myStatus = RsvpStatus.Pending), onRsvp = { status = it })
        composeRule.onNodeWithText("Can’t make it").performClick()
        assert(status == RsvpStatus.Out)
    }

    @Test
    fun answeredInShowsTheCollapsedBlockWithChange() {
        show(state = successState.copy(myStatus = RsvpStatus.In))
        composeRule.onNodeWithText("You’re in").assertIsDisplayed()
        composeRule.onNodeWithText("Change").assertIsDisplayed()
    }

    @Test
    fun answeredOutShowsYoureOut() {
        show(state = successState.copy(myStatus = RsvpStatus.Out))
        composeRule.onNodeWithText("You’re out").assertIsDisplayed()
    }

    @Test
    fun tappingChangeReopensTheTwoButtonsWithoutACallback() {
        var rsvpCalled = false
        show(state = successState.copy(myStatus = RsvpStatus.In), onRsvp = { rsvpCalled = true })
        composeRule.onNodeWithText("Change").performClick()
        composeRule.onNodeWithText("Can you play?").assertIsDisplayed()
        assert(!rsvpCalled)
    }

    @Test
    fun aRsvpErrorIsShownInTheBlock() {
        show(state = successState.copy(myStatus = RsvpStatus.In, rsvpError = DomainError.EventFull()))
        val message = "This event is full."
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(message))
        composeRule.onNodeWithText(message).assertIsDisplayed()
    }

    @Test
    fun noRsvpBlockWhenTheViewerWasNeverInvited() {
        show(state = successState.copy(myStatus = null))
        composeRule.onNodeWithText("Can you play?").assertDoesNotExist()
        composeRule.onNodeWithText("You’re in").assertDoesNotExist()
    }

    @Test
    fun frozenShowsTheStripText() {
        show(state = successState.copy(phase = EventPhase.Started(atDisplay = "7:00 pm"), myStatus = RsvpStatus.In))
        composeRule.onNodeWithText("Started at 7:00 pm · no more changes").assertIsDisplayed()
    }

    @Test
    fun frozenSlotsLineReadsNoReplyInsteadOfPending() {
        show(state = successState.copy(phase = EventPhase.Started(atDisplay = "7:00 pm")))
        composeRule.onNodeWithText("of 10 slots").assertIsDisplayed()
        composeRule.onNodeWithText("6 open").assertIsDisplayed()
        composeRule.onNodeWithText("3 in").assertIsDisplayed()
        composeRule.onNodeWithText("1 no reply").assertIsDisplayed()
        composeRule.onNodeWithText("1 out").assertIsDisplayed()
        composeRule.onNodeWithText("1 pending").assertDoesNotExist()
    }

    @Test
    fun frozenRsvpBlockIsStaticWithNoChangeButton() {
        show(state = successState.copy(phase = EventPhase.Started(atDisplay = "7:00 pm"), myStatus = RsvpStatus.In))
        composeRule.onNodeWithText("You played").assertIsDisplayed()
        composeRule.onNodeWithText("Change").assertDoesNotExist()
    }

    @Test
    fun frozenPendingRosterBadgeReadsNoReply() {
        show(
            state = successState.copy(
                phase = EventPhase.Started(atDisplay = "7:00 pm"),
                roster = listOf(RosterRow(id = "p2", name = "Tomás Neale", status = RsvpStatus.Pending))
            )
        )
        composeRule.onNodeWithText("No reply").assertIsDisplayed()
        composeRule.onNodeWithText("Pending").assertDoesNotExist()
    }

    @Test
    fun rosterShowsNamesAndMarksTheViewersOwnRow() {
        show(state = successState)
        composeRule.onNodeWithText("Alex Doyle").assertIsDisplayed()
        composeRule.onNodeWithText("Marcus Keane · you").assertIsDisplayed()
    }

    @Test
    fun rosterHeaderShowsTheCount() {
        show(state = successState)
        composeRule.onNodeWithText("Players 3").assertIsDisplayed()
    }

    @Test
    fun emptyRosterShowsTheEmptyStateWithAnAddButtonForAdmins() {
        show(state = successState.copy(isAdmin = true, roster = emptyList(), myStatus = null))
        composeRule.onNodeWithText("No one invited yet").assertIsDisplayed()
        composeRule.onAllNodesWithText("+ Add players").assertCountEquals(1)
    }

    @Test
    fun emptyRosterHasNoHeaderAddPillEvenForAdmins() {
        show(state = successState.copy(isAdmin = true, roster = emptyList(), myStatus = null))
        composeRule.onNodeWithText("+ Add").assertDoesNotExist()
    }

    @Test
    fun emptyRosterShowsNoAddButtonForMembers() {
        show(state = successState.copy(isAdmin = false, roster = emptyList(), myStatus = null))
        composeRule.onNodeWithText("No one invited yet").assertIsDisplayed()
        composeRule.onNodeWithText("+ Add players").assertDoesNotExist()
    }

    @Test
    fun nonEmptyRosterShowsTheHeaderAddPillForAdmins() {
        show(state = successState.copy(isAdmin = true))
        composeRule.onNodeWithText("+ Add").assertIsDisplayed()
    }

    @Test
    fun aRowWithNoActionsShowsNoChevron() {
        show(state = successState)
        composeRule.onNodeWithText("›").assertDoesNotExist()
    }

    private fun pendingRow(actions: List<RosterAction>) = RosterRow(id = "p2", name = "Tomás Neale", status = RsvpStatus.Pending, actions = actions)

    @Test
    fun selectingAStatusFromTheMenuReachesTheCallback() {
        var picked: Pair<String, RosterAction>? = null
        show(
            state = successState.copy(
                roster = listOf(pendingRow(listOf(RosterAction.SetIn, RosterAction.SetOut)))
            ),
            onRosterAction = { id, action -> picked = id to action }
        )
        // Opens the menu: the row's own badge is the only "Pending" node here.
        composeRule.onNodeWithText("Pending").performClick()
        composeRule.onNodeWithText("Out").performClick()
        assert(picked == "p2" to RosterAction.SetOut)
    }

    @Test
    fun eachRemovalActionShowsItsOwnLabel() {
        show(
            state = successState.copy(
                roster = listOf(
                    pendingRow(listOf(RosterAction.CancelInvite)),
                    RosterRow(id = "p1", name = "Alex Doyle", status = RsvpStatus.In, actions = listOf(RosterAction.RemoveFromEvent)),
                    RosterRow(id = "p3", name = "Marcus Keane", status = RsvpStatus.Out, actions = listOf(RosterAction.RemoveFromList))
                )
            )
        )
        composeRule.onNodeWithText("Pending").performClick()
        composeRule.onNodeWithText("Cancel invite").assertIsDisplayed()
    }

    // Cancel, remove and resend all ask for confirmation first — the RSVP
    // toggles above are the only roster actions that fire straight away.
    @Test
    fun cancellingAnInviteAsksForConfirmationBeforeCallingBack() {
        var picked: Pair<String, RosterAction>? = null
        show(
            state = successState.copy(roster = listOf(pendingRow(listOf(RosterAction.CancelInvite)))),
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
            state = successState.copy(roster = listOf(pendingRow(listOf(RosterAction.CancelInvite)))),
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
            state = successState.copy(
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
            state = successState.copy(
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
            state = successState.copy(roster = listOf(pendingRow(listOf(RosterAction.ResendInvite)))),
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
    fun aRowActionErrorIsShownUnderThatRow() {
        show(
            state = successState.copy(
                rowActionErrorId = "p2",
                rowActionError = DomainError.EventFull()
            )
        )
        val message = "This event is full."
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(message))
        composeRule.onNodeWithText(message).assertIsDisplayed()
    }

    @Test
    fun aRowActionErrorForAnotherRowIsNotShownHere() {
        show(
            state = successState.copy(
                roster = listOf(RosterRow(id = "p1", name = "Alex Doyle", status = RsvpStatus.In)),
                rowActionErrorId = "someone-else",
                rowActionError = DomainError.EventFull()
            )
        )
        composeRule.onNodeWithText("This event is full.").assertDoesNotExist()
    }

    @Test
    fun frozenHidesTheMenuEvenWhenTheRowHasActions() {
        show(
            state = successState.copy(
                isAdmin = true,
                phase = EventPhase.Started(atDisplay = "7:00 pm"),
                roster = listOf(pendingRow(listOf(RosterAction.SetIn, RosterAction.CancelInvite)))
            )
        )
        composeRule.onNodeWithText("›").assertDoesNotExist()
    }

    private val standby = listOf(
        StandbyRow(id = "p4", name = "Priya Nair"),
        StandbyRow(id = "p5", name = "Joe Moriarty", isSelf = true)
    )

    @Test
    fun emptyStandbyShowsNoSectionAtAll() {
        show(state = successState.copy(standby = emptyList()))
        composeRule.onNodeWithText("Standby", substring = true).assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Drag to reorder").assertDoesNotExist()
    }

    @Test
    fun adminSeesTheHeaderAndTheDragHint() {
        show(state = successState.copy(isAdmin = true, standby = standby))
        composeRule.onNodeWithText("Standby · 2").assertIsDisplayed()
        composeRule.onNodeWithText("Drag to reorder").assertIsDisplayed()
    }

    @Test
    fun membersSeeNoDragHintOrHandle() {
        show(state = successState.copy(isAdmin = false, standby = standby))
        composeRule.onNodeWithText("Standby · 2").assertIsDisplayed()
        composeRule.onNodeWithText("Drag to reorder").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Drag to reorder").assertDoesNotExist()
    }

    @Test
    fun standbyShowsNamesAndMarksTheViewersOwnRow() {
        show(state = successState.copy(isAdmin = false, standby = standby))
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Joe Moriarty · you"))
        composeRule.onNodeWithText("Priya Nair").assertIsDisplayed()
        composeRule.onNodeWithText("Joe Moriarty · you").assertIsDisplayed()
    }

    @Test
    fun adminSeesADragHandlePerRow() {
        show(state = successState.copy(isAdmin = true, standby = standby))
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Joe Moriarty · you"))
        composeRule.onAllNodesWithContentDescription("Drag to reorder").assertCountEquals(2)
    }

    @Test
    fun frozenStandbyReadsNotCalledUpWithNoHandleEvenForAdmins() {
        show(
            state = successState.copy(
                isAdmin = true,
                phase = EventPhase.Started(atDisplay = "7:00 pm"),
                standby = standby
            )
        )
        composeRule.onNodeWithText("Standby · Not called up").assertIsDisplayed()
        composeRule.onNodeWithText("Drag to reorder").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Drag to reorder").assertDoesNotExist()
    }

    @Test
    fun aStandbyErrorIsShown() {
        show(
            state = successState.copy(
                isAdmin = true,
                standby = standby,
                standbyError = DomainError.StandbyQueueStale()
            )
        )
        val message = "Someone was promoted while you were reordering. Refresh and try again."
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(message))
        composeRule.onNodeWithText(message).assertIsDisplayed()
    }

    private val lockedState = successState.copy(
        phase = EventPhase.Locked(byName = "Ali"),
        lockFeatureFlag = true
    )

    @Test
    fun theLockSwitchIsShownWhenTheViewerCanToggleIt() {
        show(state = successState.copy(lockFeatureFlag = true))
        composeRule.onNodeWithText("Locked").assertIsDisplayed()
        composeRule.onNode(isToggleable()).assertIsOff()
    }

    @Test
    fun noLockSwitchWhenTheViewerCannotToggleIt() {
        show(state = successState.copy(lockFeatureFlag = false))
        composeRule.onNode(isToggleable()).assertDoesNotExist()
        composeRule.onNodeWithText("Locked").assertDoesNotExist()
    }

    @Test
    fun theLockSwitchReflectsALockedEvent() {
        show(state = lockedState)
        composeRule.onNode(isToggleable()).assertIsOn()
    }

    @Test
    fun tappingTheLockSwitchReportsTheRequestedValue() {
        var toggled: Boolean? = null
        show(state = successState.copy(lockFeatureFlag = true), onLockToggled = { toggled = it })
        composeRule.onNode(isToggleable()).performClick()
        assert(toggled == true)
    }

    // LEGACY graphics doesn't measure text, so every string fits on one line.
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test
    fun longDetailsBesideTheLockSwitchWrapInsteadOfTruncating() {
        val date = "Wednesday 17 September 2026 · 7:00 pm until 9:30 pm, kick-off sharp"
        val location = "Westgate Community Sports Centre, Pitch 2 behind the car park, Long Lane, Westgate"
        show(state = successState.copy(lockFeatureFlag = true, summary = summary.copy(date = date, location = location)))
        composeRule.onNode(isToggleable()).assertIsDisplayed()
        for (text in listOf(date, location)) {
            val node = composeRule.onNodeWithText(text).assertIsDisplayed()
            val layout = node.textLayout()
            assertTrue(layout.lineCount > 1, "\"$text\" took ${layout.lineCount} line(s)")
            assertFalse(layout.hasVisualOverflow, "\"$text\" overflows")
        }
    }

    @Test
    fun aLockedEventShowsBothStripLines() {
        show(state = lockedState)
        composeRule.onNodeWithText("Locked by Ali").assertIsDisplayed()
        composeRule.onNodeWithText("No changes until an admin unlocks it").assertIsDisplayed()
        composeRule.onAllNodesWithText("Locked").assertCountEquals(1)
    }

    @Test
    fun aLockedEventWithNoLockerNameFallsBackToAnAdmin() {
        show(state = lockedState.copy(phase = EventPhase.Locked(byName = null)))
        composeRule.onNodeWithText("Locked by an admin").assertIsDisplayed()
        composeRule.onNodeWithText("No changes until an admin unlocks it").assertIsDisplayed()
    }

    @Test
    fun membersSeeTheLockedStripButNoSwitch() {
        show(state = lockedState.copy(isAdmin = false, lockFeatureFlag = false))
        composeRule.onNodeWithText("Locked by Ali").assertIsDisplayed()
        composeRule.onNodeWithText("Locked").assertDoesNotExist()
        composeRule.onNode(isToggleable()).assertDoesNotExist()
        composeRule.onNodeWithContentDescription("More options").assertDoesNotExist()
    }

    @Test
    fun anUnlockedEventShowsNoLockedStrip() {
        show(state = successState.copy(lockFeatureFlag = false))
        composeRule.onNodeWithText("No changes until an admin unlocks it").assertDoesNotExist()
    }

    @Test
    fun aStartedEventShowsTheStartedStripAndNoLock() {
        show(state = lockedState.copy(phase = EventPhase.Started(atDisplay = "7:00 pm")))
        composeRule.onNodeWithText("Started at 7:00 pm · no more changes").assertIsDisplayed()
        composeRule.onNodeWithText("Locked by Ali").assertDoesNotExist()
        composeRule.onNodeWithText("Locked").assertDoesNotExist()
        composeRule.onNode(isToggleable()).assertDoesNotExist()
    }

    @Test
    fun lockedHidesAddAndRowMenus() {
        show(state = lockedState.copy(roster = listOf(pendingRow(listOf(RosterAction.SetIn, RosterAction.CancelInvite)))))
        composeRule.onNodeWithText("›").assertDoesNotExist()
        composeRule.onNodeWithText("+ Add").assertDoesNotExist()
    }

    @Test
    fun aLockedRowBadgeHasNoClickAction() {
        show(state = lockedState.copy(myStatus = null, roster = listOf(pendingRow(listOf(RosterAction.SetOut, RosterAction.CancelInvite)))))
        composeRule.onNodeWithText("Pending").assertHasNoClickAction()
    }

    @Test
    fun aBadgeTappedWhileLockedDoesNotOpenTheMenuOnUnlock() {
        val locked = lockedState.copy(myStatus = null, roster = listOf(pendingRow(listOf(RosterAction.SetOut, RosterAction.CancelInvite))))
        var state by mutableStateOf(locked)
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
        composeRule.onNodeWithText("Cancel invite").assertDoesNotExist()

        state = locked.copy(phase = EventPhase.Active)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("›").assertIsDisplayed()
        composeRule.onNodeWithText("Out").assertDoesNotExist()
        composeRule.onNodeWithText("Cancel invite").assertDoesNotExist()
    }

    @Test
    fun lockingClosesAnOpenRosterConfirmation() {
        val unlocked = successState.copy(myStatus = null, roster = listOf(pendingRow(listOf(RosterAction.CancelInvite))))
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
        show(state = lockedState.copy(roster = emptyList(), myStatus = null))
        composeRule.onNodeWithText("No one invited yet").assertIsDisplayed()
        composeRule.onNodeWithText("+ Add players").assertDoesNotExist()
    }

    @Test
    fun lockedRsvpBlockIsStaticWithNoChangeButton() {
        show(state = lockedState.copy(myStatus = RsvpStatus.In))
        composeRule.onNodeWithText("Your reply").assertIsDisplayed()
        composeRule.onNodeWithText("Change").assertDoesNotExist()
        composeRule.onNodeWithText("You played").assertDoesNotExist()
    }

    @Test
    fun lockedUnansweredRsvpOffersNoButtons() {
        show(state = lockedState.copy(myStatus = RsvpStatus.Pending))
        composeRule.onNodeWithText("Can you play?").assertDoesNotExist()
        composeRule.onNodeWithText("I’m in").assertDoesNotExist()
    }

    @Test
    fun lockedPendingStaysPending() {
        show(
            state = lockedState.copy(
                myStatus = null,
                roster = listOf(RosterRow(id = "p2", name = "Tomás Neale", status = RsvpStatus.Pending))
            )
        )
        composeRule.onNodeWithText("Pending").assertIsDisplayed()
        composeRule.onNodeWithText("No reply").assertDoesNotExist()
        composeRule.onNodeWithText("1 no reply").assertDoesNotExist()
    }

    @Test
    fun lockedStandbyReadsPausedWithNoHandleEvenForAdmins() {
        show(state = lockedState.copy(standby = standby))
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Standby · paused"))
        composeRule.onNodeWithText("Standby · paused").assertIsDisplayed()
        composeRule.onNodeWithText("Drag to reorder").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Drag to reorder").assertDoesNotExist()
    }

    @Test
    fun anUnlockedUnstartedAdminMenuHoldsEditAndDelete() {
        show(state = successState)
        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Edit event").assertIsDisplayed()
        composeRule.onNodeWithText("Delete event").assertIsDisplayed()
    }

    @Test
    fun aLockedAdminMenuHoldsDeleteAlone() {
        show(state = lockedState)
        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Delete event").assertIsDisplayed()
        composeRule.onNodeWithText("Edit event").assertDoesNotExist()
    }

    @Test
    fun tappingDeleteEventReachesTheCallbackWithDelete() {
        var picked: EventOverflowAction? = null
        show(state = lockedState, onOverflowAction = { picked = it })
        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Delete event").performClick()
        assert(picked == EventOverflowAction.Delete)
    }

    @Test
    fun theDeleteDialogNamesTheEventDateAndLocationAndConfirms() {
        var confirmed = false
        show(
            state = successState.copy(dialog = EventDialogState(EventDialogKind.Delete)),
            onDialogConfirmed = { confirmed = true }
        )
        composeRule.onNodeWithText("Delete this event?").assertIsDisplayed()
        composeRule
            .onNodeWithText(
                "Weekly 7-a-side on Wed 17 Sep · 7:00 pm at Westgate Pitch 2 is deleted for everyone, " +
                    "with its roster and standby queue. This can’t be undone."
            )
            .assertIsDisplayed()
        composeRule.onNodeWithText("Delete").performClick()
        assert(confirmed)
    }

    @Test
    fun theDeleteDialogLeavesOutABlankLocation() {
        show(
            state = successState.copy(
                summary = summary.copy(location = ""),
                dialog = EventDialogState(EventDialogKind.Delete)
            )
        )
        composeRule
            .onNodeWithText(
                "Weekly 7-a-side on Wed 17 Sep · 7:00 pm is deleted for everyone, " +
                    "with its roster and standby queue. This can’t be undone."
            )
            .assertIsDisplayed()
    }

    @Test
    fun cancellingTheDeleteDialogReachesDismiss() {
        var dismissed = false
        show(
            state = successState.copy(dialog = EventDialogState(EventDialogKind.Delete)),
            onDialogDismissed = { dismissed = true }
        )
        composeRule.onNodeWithText("Cancel").performClick()
        assert(dismissed)
    }

    @Test
    fun aDeleteErrorIsShownInTheDialog() {
        show(state = successState.copy(dialog = EventDialogState(EventDialogKind.Delete, error = DomainError.Network())))
        composeRule.onNodeWithText("Can’t connect. Check your connection and try again.").assertIsDisplayed()
    }

    @Test
    fun theLockDialogExplainsAndConfirms() {
        var confirmed = false
        show(
            state = successState.copy(lockFeatureFlag = true, dialog = EventDialogState(EventDialogKind.Lock)),
            onDialogConfirmed = { confirmed = true }
        )
        composeRule.onNodeWithText("Lock this event?").assertIsDisplayed()
        composeRule.onNodeWithText("Lock").performClick()
        assert(confirmed)
    }

    @Test
    fun theUnlockDialogExplainsAndConfirms() {
        var confirmed = false
        show(
            state = lockedState.copy(dialog = EventDialogState(EventDialogKind.Unlock)),
            onDialogConfirmed = { confirmed = true }
        )
        composeRule.onNodeWithText("Unlock this event?").assertIsDisplayed()
        composeRule.onNodeWithText("Unlock").performClick()
        assert(confirmed)
    }

    @Test
    fun aLockErrorIsShownInTheDialog() {
        show(
            state = successState.copy(
                lockFeatureFlag = true,
                dialog = EventDialogState(EventDialogKind.Lock, error = DomainError.EventFrozen())
            )
        )
        composeRule.onNodeWithText("This event has already started.").assertIsDisplayed()
    }

    private val cancelledState = successState.copy(
        phase = EventPhase.Cancelled(byName = "Ali", started = false)
    )

    @Test
    fun aCancelledEventShowsBothStripLines() {
        show(state = cancelledState)
        composeRule.onNodeWithText("Cancelled by Ali").assertIsDisplayed()
        composeRule.onNodeWithText("This event won’t go ahead").assertIsDisplayed()
    }

    @Test
    fun aCancelledEventWithNoCancellerNameFallsBackToAnAdmin() {
        show(state = cancelledState.copy(phase = EventPhase.Cancelled(byName = null, started = false)))
        composeRule.onNodeWithText("Cancelled by an admin").assertIsDisplayed()
    }

    @Test
    fun aCancelledEventThatHasStartedShowsTheCancelledStripAndNoLock() {
        show(
            state = cancelledState.copy(
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
        show(state = cancelledState.copy(roster = listOf(pendingRow(listOf(RosterAction.SetIn, RosterAction.CancelInvite)))))
        composeRule.onNodeWithText("›").assertDoesNotExist()
        composeRule.onNodeWithText("+ Add").assertDoesNotExist()
    }

    @Test
    fun cancelledPendingStaysPending() {
        show(
            state = cancelledState.copy(
                myStatus = null,
                roster = listOf(RosterRow(id = "p2", name = "Tomás Neale", status = RsvpStatus.Pending))
            )
        )
        composeRule.onNodeWithText("Pending").assertIsDisplayed()
        composeRule.onNodeWithText("No reply").assertDoesNotExist()
    }

    @Test
    fun cancelledStandbyReadsNotCalledUpWithNoHandle() {
        show(state = cancelledState.copy(standby = standby))
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Standby · Not called up"))
        composeRule.onNodeWithText("Standby · Not called up").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Drag to reorder").assertDoesNotExist()
    }

    @Test
    fun tappingCancelEventReachesTheCallbackWithCancel() {
        var picked: EventOverflowAction? = null
        show(
            state = successState.copy(cancelFeatureFlag = true),
            onOverflowAction = { picked = it }
        )
        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Cancel event").performClick()
        assert(picked == EventOverflowAction.Cancel)
    }

    @Test
    fun theCancelDialogNamesTheEventAndConfirms() {
        var confirmed = false
        show(
            state = successState.copy(dialog = EventDialogState(EventDialogKind.Cancel)),
            onDialogConfirmed = { confirmed = true }
        )
        composeRule.onNodeWithText("Cancel this event?").assertIsDisplayed()
        composeRule
            .onNodeWithText(
                "Weekly 7-a-side on Wed 17 Sep · 7:00 pm at Westgate Pitch 2 stays visible, " +
                    "but nobody can change anything. This can’t be undone."
            )
            .assertIsDisplayed()
        composeRule.onNodeWithText("Cancel event").performClick()
        assert(confirmed)
    }

    @Test
    fun keepingTheEventReachesDismiss() {
        var dismissed = false
        show(
            state = successState.copy(dialog = EventDialogState(EventDialogKind.Cancel)),
            onDialogDismissed = { dismissed = true }
        )
        composeRule.onNodeWithText("Keep event").performClick()
        assert(dismissed)
    }

    @Test
    fun aCancelErrorIsShownInTheDialog() {
        show(state = successState.copy(dialog = EventDialogState(EventDialogKind.Cancel, error = DomainError.EventFrozen())))
        composeRule.onNodeWithText("This event has already started.").assertIsDisplayed()
    }

    private fun SemanticsNodeInteraction.textLayout(): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        return results.single()
    }
}
