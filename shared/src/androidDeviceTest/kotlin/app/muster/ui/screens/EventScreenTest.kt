package app.muster.ui.screens

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import app.muster.domain.error.DomainError
import app.muster.domain.model.RsvpStatus
import app.muster.ui.screens.event.EventActions
import app.muster.ui.screens.event.EventScreen
import app.muster.ui.screens.event.EventSummary
import app.muster.ui.screens.event.EventUiState
import app.muster.ui.screens.event.RosterAction
import app.muster.ui.screens.event.RosterRow
import app.muster.ui.screens.event.StandbyRow
import app.muster.ui.theme.MusterTheme
import org.junit.Rule
import org.junit.Test

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
        onRetry: () -> Unit = {}
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
                        onRetry = onRetry
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
    fun theAppBarShowsThePlaceholderMoreOptionsIcon() {
        show(state = successState)
        composeRule.onNodeWithContentDescription("More options").assertIsDisplayed()
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
        show(state = successState.copy(isFrozen = true, startTime = "7:00 pm", myStatus = RsvpStatus.In))
        composeRule.onNodeWithText("Started at 7:00 pm · no more changes").assertIsDisplayed()
    }

    @Test
    fun frozenSlotsLineReadsNoReplyInsteadOfPending() {
        show(state = successState.copy(isFrozen = true, startTime = "7:00 pm"))
        composeRule.onNodeWithText("of 10 slots").assertIsDisplayed()
        composeRule.onNodeWithText("6 open").assertIsDisplayed()
        composeRule.onNodeWithText("3 in").assertIsDisplayed()
        composeRule.onNodeWithText("1 no reply").assertIsDisplayed()
        composeRule.onNodeWithText("1 out").assertIsDisplayed()
        composeRule.onNodeWithText("1 pending").assertDoesNotExist()
    }

    @Test
    fun frozenRsvpBlockIsStaticWithNoChangeButton() {
        show(state = successState.copy(isFrozen = true, startTime = "7:00 pm", myStatus = RsvpStatus.In))
        composeRule.onNodeWithText("You played").assertIsDisplayed()
        composeRule.onNodeWithText("Change").assertDoesNotExist()
    }

    @Test
    fun frozenPendingRosterBadgeReadsNoReply() {
        show(
            state = successState.copy(
                isFrozen = true,
                startTime = "7:00 pm",
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

    private fun pendingRow(actions: List<RosterAction>) =
        RosterRow(id = "p2", name = "Tomás Neale", status = RsvpStatus.Pending, actions = actions)

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
                isFrozen = true,
                startTime = "7:00 pm",
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
        composeRule.onNodeWithText("Priya Nair").assertIsDisplayed()
        composeRule.onNodeWithText("Joe Moriarty · you").assertIsDisplayed()
    }

    @Test
    fun adminSeesADragHandlePerRow() {
        show(state = successState.copy(isAdmin = true, standby = standby))
        composeRule.onAllNodesWithContentDescription("Drag to reorder").assertCountEquals(2)
    }

    @Test
    fun frozenStandbyReadsNotCalledUpWithNoHandleEvenForAdmins() {
        show(
            state = successState.copy(
                isAdmin = true,
                isFrozen = true,
                startTime = "7:00 pm",
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
        composeRule.onNodeWithText("Someone was promoted while you were reordering. Refresh and try again.")
            .assertIsDisplayed()
    }
}
