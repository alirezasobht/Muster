package app.muster.ui.navgraphs

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.composable
import app.muster.ui.screens.group.members.AddMemberByEmail
import app.muster.ui.screens.group.members.membersGraph
import kotlinx.serialization.Serializable
import org.junit.Rule
import org.junit.Test

class MembersNavGraphTest {

    @Serializable
    private data object Start

    @get:Rule
    val composeRule = createComposeRule()

    private val harness = NavGraphHarness(composeRule)

    @Test
    fun addByEmailShowsTheGroupNameAndCloseGoesBack() {
        harness.show(start = Start) { nav ->
            composable<Start> { Text("start stub") }
            membersGraph(nav)
        }
        composeRule.runOnIdle { harness.navController.navigate(AddMemberByEmail(groupId = "g1", groupName = "Sunday League")) }
        harness.assertAt<AddMemberByEmail>()
        harness.waitForText("Sunday League")

        composeRule.onNodeWithContentDescription("Close").performClick()

        harness.assertAt<Start>()
    }
}
