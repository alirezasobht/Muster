package app.muster.ui.screens.event.components

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import app.muster.domain.error.DomainError
import app.muster.ui.screens.event.EventPhase
import app.muster.ui.screens.event.StandbyRow
import app.muster.ui.screens.event.TestStandby
import app.muster.ui.theme.MusterTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StandbyTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(
        standby: List<StandbyRow> = TestStandby,
        isAdmin: Boolean = true,
        phase: EventPhase = EventPhase.Active,
        error: DomainError? = null
    ) {
        composeRule.setContent {
            MusterTheme {
                LazyColumn {
                    standbySection(
                        standby = standby,
                        isAdmin = isAdmin,
                        phase = phase,
                        reordering = false,
                        error = error,
                        onDraggingChanged = {},
                        onReorder = {}
                    )
                }
            }
        }
    }

    private fun assertNoDragHintOrHandle() {
        composeRule.onNodeWithText("Drag to reorder").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Drag to reorder").assertDoesNotExist()
    }

    @Test
    fun emptyStandbyShowsNoSectionAtAll() {
        show(standby = emptyList())
        composeRule.onNodeWithText("Standby", substring = true).assertDoesNotExist()
        assertNoDragHintOrHandle()
    }

    @Test
    fun adminSeesTheHeaderAndTheDragHint() {
        show()
        composeRule.onNodeWithText("Standby · 2").assertIsDisplayed()
        composeRule.onNodeWithText("Drag to reorder").assertIsDisplayed()
    }

    @Test
    fun membersSeeNoDragHintOrHandle() {
        show(isAdmin = false)
        composeRule.onNodeWithText("Standby · 2").assertIsDisplayed()
        assertNoDragHintOrHandle()
    }

    @Test
    fun standbyShowsNamesAndMarksTheViewersOwnRow() {
        show(isAdmin = false)
        composeRule.onNodeWithText("Priya Nair").assertIsDisplayed()
        composeRule.onNodeWithText("Joe Moriarty · you").assertIsDisplayed()
    }

    @Test
    fun adminSeesADragHandlePerRow() {
        show()
        composeRule.onAllNodesWithContentDescription("Drag to reorder").assertCountEquals(2)
    }

    @Test
    fun frozenStandbyReadsNotCalledUpWithNoHandleEvenForAdmins() {
        show(phase = EventPhase.Started(atDisplay = "7:00 pm"))
        composeRule.onNodeWithText("Standby · Not called up").assertIsDisplayed()
        assertNoDragHintOrHandle()
    }

    @Test
    fun lockedStandbyReadsPausedWithNoHandleEvenForAdmins() {
        show(phase = EventPhase.Locked(byName = "Ali"))
        composeRule.onNodeWithText("Standby · paused").assertIsDisplayed()
        assertNoDragHintOrHandle()
    }

    @Test
    fun cancelledStandbyReadsNotCalledUpWithNoHandle() {
        show(phase = EventPhase.Cancelled(byName = "Ali", started = false))
        composeRule.onNodeWithText("Standby · Not called up").assertIsDisplayed()
        assertNoDragHintOrHandle()
    }

    @Test
    fun aStandbyErrorIsShown() {
        show(error = DomainError.StandbyQueueStale())
        composeRule.onNodeWithText("Someone was promoted while you were reordering. Refresh and try again.").assertIsDisplayed()
    }
}
