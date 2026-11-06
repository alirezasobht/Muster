package app.muster.ui.screens.event.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.muster.ui.screens.event.EventOverflowAction
import app.muster.ui.screens.event.TestSummary
import app.muster.ui.theme.MusterTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EventAppBarTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val allActions = listOf(EventOverflowAction.Edit, EventOverflowAction.Cancel, EventOverflowAction.Delete)

    private fun show(
        overflowActions: List<EventOverflowAction> = allActions,
        onBack: () -> Unit = {},
        onOverflowAction: (EventOverflowAction) -> Unit = {}
    ) {
        composeRule.setContent {
            MusterTheme {
                EventAppBar(
                    summary = TestSummary,
                    overflowActions = overflowActions,
                    onBack = onBack,
                    onOverflowAction = onOverflowAction
                )
            }
        }
    }

    private fun pick(label: String): EventOverflowAction? {
        var picked: EventOverflowAction? = null
        show(onOverflowAction = { picked = it })
        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText(label).performClick()
        return picked
    }

    @Test
    fun backReachesTheCallback() {
        var back = false
        show(onBack = { back = true })
        composeRule.onNodeWithContentDescription("Back").performClick()
        assert(back)
    }

    @Test
    fun tappingEditEventReachesTheCallbackWithEdit() {
        assert(pick("Edit event") == EventOverflowAction.Edit)
    }

    @Test
    fun tappingCancelEventReachesTheCallbackWithCancel() {
        assert(pick("Cancel event") == EventOverflowAction.Cancel)
    }

    @Test
    fun tappingDeleteEventReachesTheCallbackWithDelete() {
        assert(pick("Delete event") == EventOverflowAction.Delete)
    }

    @Test
    fun noOverflowActionsShowsNoMoreOptions() {
        show(overflowActions = emptyList())
        composeRule.onNodeWithContentDescription("More options").assertDoesNotExist()
    }
}
