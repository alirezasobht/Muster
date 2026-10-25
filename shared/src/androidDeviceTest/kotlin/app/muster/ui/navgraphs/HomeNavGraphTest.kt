package app.muster.ui.navgraphs

import androidx.compose.material3.Text
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.navigation.compose.composable
import app.muster.ui.screens.group.Group
import app.muster.ui.screens.home.Home
import app.muster.ui.screens.home.NewGroup
import app.muster.ui.screens.home.PrivacyPolicy
import app.muster.ui.screens.home.Settings
import app.muster.ui.screens.home.homeGraph
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test

// Real Home screens with the repository fakes behind Koin. Group is a stub:
// it belongs to another graph, so only the navigation to it is asserted.
class HomeNavGraphTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val harness = NavGraphHarness(composeRule)

    private fun show() {
        harness.show(start = Home) { nav ->
            homeGraph(nav, onSignOut = {})
            composable<Group> { Text("group stub") }
        }
        harness.waitForText("Sunday League")
    }

    @Test
    fun startsOnHome() {
        show()
        harness.assertAt<Home>()
    }

    @Test
    fun tappingAGroupOpensItWithItsIdAndName() {
        show()
        composeRule.onNodeWithText("Sunday League").performClick()

        harness.assertAt<Group>()
        assertEquals(Group(id = "g1", name = "Sunday League"), harness.route<Group>())
    }

    @Test
    fun newGroupOpensTheNewGroupScreen() {
        show()
        composeRule.onNodeWithContentDescription("New group").performClick()

        harness.assertAt<NewGroup>()
    }

    @Test
    fun creatingAGroupOpensItAndDropsNewGroupFromTheBackStack() {
        show()
        composeRule.onNodeWithContentDescription("New group").performClick()
        harness.assertAt<NewGroup>()
        composeRule.onNode(hasSetTextAction()).performTextInput("Westgate Wednesday 7s")
        composeRule.onNodeWithText("Create group").performClick()

        harness.assertAt<Group>()
        assertTrue(!harness.onBackStack<NewGroup>())
    }

    @Test
    fun settingsOpensFromHomeAndBackReturnsToIt() {
        show()
        composeRule.onNodeWithText("Settings").performClick()
        harness.assertAt<Settings>()

        composeRule.onNodeWithContentDescription("Back").performClick()
        harness.assertAt<Home>()
    }

    @Test
    fun privacyPolicyOpensFromSettingsAndBackReturnsToSettings() {
        show()
        composeRule.onNodeWithText("Settings").performClick()
        harness.assertAt<Settings>()
        harness.waitForText("Privacy policy")
        composeRule.onNodeWithText("Privacy policy").performClick()
        harness.assertAt<PrivacyPolicy>()

        composeRule.onNodeWithContentDescription("Back").performClick()
        harness.assertAt<Settings>()
    }
}
