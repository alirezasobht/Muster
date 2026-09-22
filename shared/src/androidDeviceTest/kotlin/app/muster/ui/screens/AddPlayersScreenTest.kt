package app.muster.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.muster.domain.error.DomainError
import app.muster.ui.screens.addplayers.AddPlayersActions
import app.muster.ui.screens.addplayers.AddPlayersScreen
import app.muster.ui.screens.addplayers.AddPlayersUiState
import app.muster.ui.screens.addplayers.CandidateRow
import app.muster.ui.theme.MusterTheme
import org.junit.Rule
import org.junit.Test

class AddPlayersScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val candidates = listOf(
        CandidateRow(id = "1", name = "Leo Fanning"),
        CandidateRow(id = "2", name = "Hana Berg"),
        CandidateRow(id = "3", name = "Eoin O’Shea")
    )

    private fun show(
        state: AddPlayersUiState = AddPlayersUiState.Success(candidates = candidates, capacity = 12, freeSlots = 3),
        onToggle: (String) -> Unit = {},
        onConfirm: () -> Unit = {},
        onBack: () -> Unit = {},
        onRetry: () -> Unit = {}
    ) {
        composeRule.setContent {
            MusterTheme {
                AddPlayersScreen(
                    state = state,
                    actions = AddPlayersActions(
                        onToggle = onToggle,
                        onConfirm = onConfirm,
                        onBack = onBack,
                        onRetry = onRetry
                    )
                )
            }
        }
    }

    @Test
    fun candidatesAreListed() {
        show()
        composeRule.onNodeWithText("Leo Fanning").assertIsDisplayed()
        composeRule.onNodeWithText("Hana Berg").assertIsDisplayed()
    }

    @Test
    fun backReachesTheCallback() {
        var back = false
        show(onBack = { back = true })
        composeRule.onNodeWithContentDescription("Back").performClick()
        assert(back)
    }

    @Test
    fun tappingARowReachesToggleWithItsId() {
        var toggled: String? = null
        show(onToggle = { toggled = it })
        composeRule.onNodeWithText("Leo Fanning").performClick()
        assert(toggled == "1")
    }

    @Test
    fun confirmIsDisabledWithNothingSelected() {
        show()
        composeRule.onNodeWithText("Add 0 players").assertIsNotEnabled()
    }

    @Test
    fun confirmIsEnabledOnceSomethingIsSelected() {
        show(
            state = AddPlayersUiState.Success(
                candidates = candidates,
                capacity = 12,
                freeSlots = 3,
                selectedIds = listOf("1")
            )
        )
        composeRule.onNodeWithText("Add 1 players").assertIsEnabled()
    }

    @Test
    fun confirmReachesTheCallback() {
        var confirmed = false
        show(
            state = AddPlayersUiState.Success(
                candidates = candidates,
                capacity = 12,
                freeSlots = 3,
                selectedIds = listOf("1")
            ),
            onConfirm = { confirmed = true }
        )
        composeRule.onNodeWithText("Add 1 players").performClick()
        assert(confirmed)
    }

    @Test
    fun aSelectedRowShowsWhereItLands() {
        show(
            state = AddPlayersUiState.Success(
                candidates = candidates,
                capacity = 12,
                freeSlots = 0,
                queueLength = 3,
                selectedIds = listOf("1")
            )
        )
        composeRule.onNodeWithText("Standby #4").assertIsDisplayed()
    }

    @Test
    fun anUnselectedRowShowsNoDestination() {
        show(state = AddPlayersUiState.Success(candidates = candidates, capacity = 12, freeSlots = 3))
        composeRule.onNodeWithText("Invited").assertDoesNotExist()
    }

    @Test
    fun theStatsLineShowsCapacityFreeInvitingAndStandby() {
        show(
            state = AddPlayersUiState.Success(
                candidates = candidates,
                capacity = 12,
                freeSlots = 3,
                selectedIds = listOf("1")
            )
        )
        composeRule.onNodeWithText("Capacity 12 · Free 3 · Inviting 1 · Standby 0").assertIsDisplayed()
    }

    @Test
    fun theEmptyStateIsShownWhenEveryoneIsAlreadyOnTheEvent() {
        show(state = AddPlayersUiState.Success(candidates = emptyList()))
        composeRule.onNodeWithText("Everyone’s already on this event").assertIsDisplayed()
    }

    @Test
    fun addingDisablesTheRows() {
        var toggled = false
        show(
            state = AddPlayersUiState.Success(candidates = candidates, selectedIds = listOf("1"), adding = true),
            onToggle = { toggled = true }
        )
        composeRule.onNodeWithText("Leo Fanning").performClick()
        assert(!toggled)
    }

    @Test
    fun aFormErrorIsShownAboveTheButton() {
        show(
            state = AddPlayersUiState.Success(
                candidates = candidates,
                selectedIds = listOf("1"),
                error = DomainError.Network()
            )
        )
        composeRule.onNodeWithText("Can’t connect. Check your connection and try again.").assertIsDisplayed()
    }

    @Test
    fun retryReachesTheCallbackOnAFailedLoad() {
        var retried = false
        show(state = AddPlayersUiState.Error(DomainError.Network()), onRetry = { retried = true })
        composeRule.onNodeWithText("Try again").performClick()
        assert(retried)
    }
}
