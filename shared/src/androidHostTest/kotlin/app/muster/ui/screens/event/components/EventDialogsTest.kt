package app.muster.ui.screens.event.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.muster.domain.error.DomainError
import app.muster.ui.screens.event.EventDialogKind
import app.muster.ui.screens.event.EventDialogState
import app.muster.ui.screens.event.EventSummary
import app.muster.ui.screens.event.TestSummary
import app.muster.ui.theme.MusterTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EventDialogsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(
        dialog: EventDialogState,
        summary: EventSummary = TestSummary,
        onConfirm: () -> Unit = {},
        onDismiss: () -> Unit = {}
    ) {
        composeRule.setContent {
            MusterTheme {
                EventDialogs(
                    pendingConfirmation = null,
                    onPendingClosed = {},
                    dialog = dialog,
                    summary = summary,
                    onConfirm = onConfirm,
                    onDismiss = onDismiss
                )
            }
        }
    }

    @Test
    fun theDeleteDialogNamesTheEventDateAndLocationAndConfirms() {
        var confirmed = false
        show(dialog = EventDialogState(EventDialogKind.Delete), onConfirm = { confirmed = true })
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
        show(dialog = EventDialogState(EventDialogKind.Delete), summary = TestSummary.copy(location = ""))
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
        show(dialog = EventDialogState(EventDialogKind.Delete), onDismiss = { dismissed = true })
        composeRule.onNodeWithText("Cancel").performClick()
        assert(dismissed)
    }

    @Test
    fun aDeleteErrorIsShownInTheDialog() {
        show(dialog = EventDialogState(EventDialogKind.Delete, error = DomainError.Network()))
        composeRule.onNodeWithText("Can’t connect. Check your connection and try again.").assertIsDisplayed()
    }

    @Test
    fun theLockDialogExplainsAndConfirms() {
        var confirmed = false
        show(dialog = EventDialogState(EventDialogKind.Lock), onConfirm = { confirmed = true })
        composeRule.onNodeWithText("Lock this event?").assertIsDisplayed()
        composeRule.onNodeWithText("Lock").performClick()
        assert(confirmed)
    }

    @Test
    fun theUnlockDialogExplainsAndConfirms() {
        var confirmed = false
        show(dialog = EventDialogState(EventDialogKind.Unlock), onConfirm = { confirmed = true })
        composeRule.onNodeWithText("Unlock this event?").assertIsDisplayed()
        composeRule.onNodeWithText("Unlock").performClick()
        assert(confirmed)
    }

    @Test
    fun aLockErrorIsShownInTheDialog() {
        show(dialog = EventDialogState(EventDialogKind.Lock, error = DomainError.EventFrozen()))
        composeRule.onNodeWithText("This event has already started.").assertIsDisplayed()
    }

    @Test
    fun theCancelDialogNamesTheEventAndConfirms() {
        var confirmed = false
        show(dialog = EventDialogState(EventDialogKind.Cancel), onConfirm = { confirmed = true })
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
        show(dialog = EventDialogState(EventDialogKind.Cancel), onDismiss = { dismissed = true })
        composeRule.onNodeWithText("Keep event").performClick()
        assert(dismissed)
    }

    @Test
    fun aCancelErrorIsShownInTheDialog() {
        show(dialog = EventDialogState(EventDialogKind.Cancel, error = DomainError.EventFrozen()))
        composeRule.onNodeWithText("This event has already started.").assertIsDisplayed()
    }
}
