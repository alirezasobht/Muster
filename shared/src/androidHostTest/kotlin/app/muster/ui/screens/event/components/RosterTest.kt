package app.muster.ui.screens.event.components

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.muster.domain.error.DomainError
import app.muster.domain.model.RsvpStatus
import app.muster.ui.screens.event.EventPhase
import app.muster.ui.screens.event.EventUiState
import app.muster.ui.screens.event.RosterAction
import app.muster.ui.screens.event.RosterRow
import app.muster.ui.screens.event.TestCancelled
import app.muster.ui.screens.event.TestLocked
import app.muster.ui.screens.event.TestSuccess
import app.muster.ui.screens.event.pendingRow
import app.muster.ui.theme.MusterTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RosterTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val started = TestSuccess.copy(phase = EventPhase.Started(atDisplay = "7:00 pm"))

    private fun show(
        state: EventUiState.Success,
        showAddAction: Boolean = true,
        isFrozen: Boolean = false,
        onRowAction: (RosterRow, RosterAction) -> Unit = { _, _ -> }
    ) {
        composeRule.setContent {
            MusterTheme {
                LazyColumn {
                    rosterSection(
                        state = state,
                        showAddAction = showAddAction,
                        isFrozen = isFrozen,
                        onAddPlayers = {},
                        onRowAction = onRowAction
                    )
                }
            }
        }
    }

    @Test
    fun rosterShowsNamesAndMarksTheViewersOwnRow() {
        show(state = TestSuccess)
        composeRule.onNodeWithText("Alex Doyle").assertIsDisplayed()
        composeRule.onNodeWithText("Marcus Keane · you").assertIsDisplayed()
    }

    @Test
    fun rosterHeaderShowsTheCount() {
        show(state = TestSuccess)
        composeRule.onNodeWithText("Players 3").assertIsDisplayed()
    }

    @Test
    fun emptyRosterShowsTheEmptyStateWithAnAddButtonForAdmins() {
        show(state = TestSuccess.copy(roster = emptyList()))
        composeRule.onNodeWithText("No one invited yet").assertIsDisplayed()
        composeRule.onAllNodesWithText("+ Add players").assertCountEquals(1)
    }

    @Test
    fun emptyRosterHasNoHeaderAddPillEvenForAdmins() {
        show(state = TestSuccess.copy(roster = emptyList()))
        composeRule.onNodeWithText("+ Add").assertDoesNotExist()
    }

    @Test
    fun emptyRosterShowsNoAddButtonForMembers() {
        show(state = TestSuccess.copy(isAdmin = false, roster = emptyList()), showAddAction = false)
        composeRule.onNodeWithText("No one invited yet").assertIsDisplayed()
        composeRule.onNodeWithText("+ Add players").assertDoesNotExist()
    }

    @Test
    fun nonEmptyRosterShowsTheHeaderAddPillForAdmins() {
        show(state = TestSuccess)
        composeRule.onNodeWithText("+ Add").assertIsDisplayed()
    }

    @Test
    fun aRowWithNoActionsShowsNoChevron() {
        show(state = TestSuccess)
        composeRule.onNodeWithText("›").assertDoesNotExist()
    }

    @Test
    fun selectingAStatusFromTheMenuReachesTheCallback() {
        var picked: Pair<String, RosterAction>? = null
        show(
            state = TestSuccess.copy(roster = listOf(pendingRow(listOf(RosterAction.SetIn, RosterAction.SetOut)))),
            onRowAction = { row, action -> picked = row.id to action }
        )
        // Opens the menu: the row's own badge is the only "Pending" node here.
        composeRule.onNodeWithText("Pending").performClick()
        composeRule.onNodeWithText("Out").performClick()
        assert(picked == "p2" to RosterAction.SetOut)
    }

    @Test
    fun eachRemovalActionShowsItsOwnLabel() {
        show(
            state = TestSuccess.copy(
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

    @Test
    fun aRowActionErrorIsShownUnderThatRow() {
        show(state = TestSuccess.copy(rowActionErrorId = "p2", rowActionError = DomainError.EventFull()))
        composeRule.onNodeWithText("This event is full.").assertIsDisplayed()
    }

    @Test
    fun aRowActionErrorForAnotherRowIsNotShownHere() {
        show(
            state = TestSuccess.copy(
                roster = listOf(RosterRow(id = "p1", name = "Alex Doyle", status = RsvpStatus.In)),
                rowActionErrorId = "someone-else",
                rowActionError = DomainError.EventFull()
            )
        )
        composeRule.onNodeWithText("This event is full.").assertDoesNotExist()
    }

    @Test
    fun frozenPendingRosterBadgeReadsNoReply() {
        show(
            state = started.copy(roster = listOf(RosterRow(id = "p2", name = "Tomás Neale", status = RsvpStatus.Pending))),
            isFrozen = true
        )
        composeRule.onNodeWithText("No reply").assertIsDisplayed()
        composeRule.onNodeWithText("Pending").assertDoesNotExist()
    }

    @Test
    fun frozenHidesTheMenuEvenWhenTheRowHasActions() {
        show(
            state = started.copy(roster = listOf(pendingRow(listOf(RosterAction.SetIn, RosterAction.CancelInvite)))),
            isFrozen = true
        )
        composeRule.onNodeWithText("›").assertDoesNotExist()
    }

    @Test
    fun aLockedRowBadgeHasNoClickAction() {
        show(state = TestLocked.copy(roster = listOf(pendingRow(listOf(RosterAction.SetOut, RosterAction.CancelInvite)))))
        composeRule.onNodeWithText("Pending").assertHasNoClickAction()
    }

    @Test
    fun aBadgeTappedWhileLockedDoesNotOpenTheMenuOnUnlock() {
        val locked = TestLocked.copy(roster = listOf(pendingRow(listOf(RosterAction.SetOut, RosterAction.CancelInvite))))
        var state by mutableStateOf(locked)
        composeRule.setContent {
            MusterTheme {
                LazyColumn {
                    rosterSection(
                        state = state,
                        showAddAction = false,
                        isFrozen = false,
                        onAddPlayers = {},
                        onRowAction = { _, _ -> }
                    )
                }
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
    fun lockedPendingStaysPending() {
        show(state = TestLocked.copy(roster = listOf(RosterRow(id = "p2", name = "Tomás Neale", status = RsvpStatus.Pending))))
        composeRule.onNodeWithText("Pending").assertIsDisplayed()
        composeRule.onNodeWithText("No reply").assertDoesNotExist()
    }

    @Test
    fun cancelledPendingStaysPending() {
        show(state = TestCancelled.copy(roster = listOf(RosterRow(id = "p2", name = "Tomás Neale", status = RsvpStatus.Pending))))
        composeRule.onNodeWithText("Pending").assertIsDisplayed()
        composeRule.onNodeWithText("No reply").assertDoesNotExist()
    }
}
