package app.muster.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.muster.domain.error.DomainError
import app.muster.ui.screens.home.HomeActions
import app.muster.ui.screens.home.HomeFailedScreen
import app.muster.ui.screens.home.HomeGroup
import app.muster.ui.screens.home.HomeInvitation
import app.muster.ui.screens.home.HomeLoadingScreen
import app.muster.ui.screens.home.HomeScreen
import app.muster.ui.theme.MusterTheme
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(
        myGroups: List<HomeGroup> = emptyList(),
        invitations: List<HomeInvitation> = emptyList(),
        canCreateGroups: Boolean = false,
        signedInEmail: String = "alex.doyle@gmail.com",
        respondingTo: String? = null,
        onGroupClick: (String) -> Unit = {},
        onNewGroupClick: () -> Unit = {},
        onAccept: (String) -> Unit = {},
        onDecline: (String) -> Unit = {}
    ) {
        composeRule.setContent {
            MusterTheme {
                HomeScreen(
                    myGroups = myGroups,
                    invitations = invitations,
                    canCreateGroups = canCreateGroups,
                    signedInEmail = signedInEmail,
                    respondingTo = respondingTo,
                    actions = HomeActions(
                        onGroupClick = onGroupClick,
                        onNewGroupClick = onNewGroupClick,
                        onSettingsClick = {},
                        onAccept = onAccept,
                        onDecline = onDecline
                    )
                )
            }
        }
    }

    @Test
    fun tappingAGroupCardReachesTheCallback() {
        var tapped: String? = null
        show(
            myGroups = listOf(HomeGroup("g1", "Westgate Wednesday 7s")),
            onGroupClick = { tapped = it }
        )
        composeRule.onNodeWithText("Westgate Wednesday 7s").performClick()
        assert(tapped == "g1")
    }

    @Test
    fun acceptingAnInvitationReachesTheCallback() {
        var accepted: String? = null
        show(
            invitations = listOf(HomeInvitation("i1", "Thornbury Thursday")),
            onAccept = { accepted = it }
        )
        composeRule.onNodeWithText("Accept").performClick()
        assert(accepted == "i1")
    }

    @Test
    fun decliningAnInvitationReachesTheCallback() {
        var declined: String? = null
        show(
            invitations = listOf(HomeInvitation("i1", "Thornbury Thursday")),
            onDecline = { declined = it }
        )
        composeRule.onNodeWithText("Decline").performClick()
        assert(declined == "i1")
    }

    // While responding, PrimaryButton swaps its label for a spinner.
    @Test
    fun respondingReplacesAcceptWithASpinner() {
        show(
            invitations = listOf(HomeInvitation("i1", "Thornbury Thursday")),
            respondingTo = "i1"
        )
        composeRule.onNodeWithText("Accept").assertDoesNotExist()
    }

    @Test
    fun invitedByIsShownWhenKnown() {
        show(invitations = listOf(HomeInvitation("i1", "Thornbury Thursday", "Dan Whelan")))
        composeRule.onNodeWithText("Invited by Dan Whelan").assertIsDisplayed()
    }

    @Test
    fun newGroupIsShownOnlyWhenAllowed() {
        show(canCreateGroups = true)
        composeRule.onNodeWithText("New group").assertIsDisplayed()
    }

    @Test
    fun newGroupIsHiddenWithoutTheAllowlistFlag() {
        show(canCreateGroups = false)
        composeRule.onNodeWithText("New group").assertDoesNotExist()
    }

    @Test
    fun theEmptyStateShowsTheSignedInAddress() {
        show(signedInEmail = "alex.doyle@gmail.com")
        composeRule.onNodeWithText("No groups yet").assertIsDisplayed()
        composeRule.onNodeWithText("alex.doyle@gmail.com").assertIsDisplayed()
    }

    @Test
    fun loadingShowsTheAppBarButNoContent() {
        composeRule.setContent {
            MusterTheme { HomeLoadingScreen(onSettingsClick = {}, spinnerDelayMillis = 0) }
        }
        composeRule.onNodeWithText("Muster").assertIsDisplayed()
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
        composeRule.onNodeWithText("No groups yet").assertDoesNotExist()
    }

    @Test
    fun failedShowsTheErrorMessage() {
        composeRule.setContent {
            MusterTheme {
                HomeFailedScreen(error = DomainError.Network(), onSettingsClick = {}, onRetry = {})
            }
        }
        composeRule.onNodeWithText("Couldn’t load your groups").assertIsDisplayed()
        composeRule
            .onNodeWithText("Can’t connect. Check your connection and try again.")
            .assertIsDisplayed()
    }

    @Test
    fun failedTryAgainReachesTheCallback() {
        var retried = false
        composeRule.setContent {
            MusterTheme {
                HomeFailedScreen(
                    error = DomainError.Network(),
                    onSettingsClick = {},
                    onRetry = { retried = true }
                )
            }
        }
        composeRule.onNodeWithText("Try again").performClick()
        assert(retried)
    }
}
