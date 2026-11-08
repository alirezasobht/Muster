package app.muster.ui.screens.group.members.components

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.muster.ui.screens.group.members.MemberRow
import app.muster.ui.screens.group.members.MemberStatus
import app.muster.ui.theme.MusterTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MemberRowItemTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val sam = MemberRow(
        id = "4",
        displayName = "Sam Okafor",
        status = MemberStatus.Member,
        canMoveToTag = true,
        canPromote = true,
        canRemove = true
    )

    private val invite = MemberRow(
        id = "9",
        displayName = "j.moriarty@outlook.com",
        status = MemberStatus.Pending,
        canRevokeInvitation = true,
        canResendInvitation = true
    )

    private val self = MemberRow(
        id = "1",
        displayName = "Alex Doyle",
        status = MemberStatus.Admin,
        isSelf = true,
        canMoveToTag = true
    )

    private fun show(
        row: MemberRow,
        actions: MemberRowActions = MemberRowActions(),
        inFlight: Boolean = false,
        errorMessage: String? = null,
        isMoving: Boolean = false
    ) {
        composeRule.setContent {
            MusterTheme {
                MemberRowItem(
                    row = row,
                    memberRowActions = actions,
                    inFlight = inFlight,
                    errorMessage = errorMessage,
                    isPending = row.status == MemberStatus.Pending,
                    isMoving = isMoving
                )
            }
        }
    }

    private val spinner = hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)

    @Test
    fun aMovableRowShowsTheDragHandle() {
        show(sam)
        composeRule.onNodeWithContentDescription("Drag to move").assertIsDisplayed()
        composeRule.onNode(spinner).assertDoesNotExist()
    }

    @Test
    fun aRowThatCannotMoveToATagHasNoDragHandle() {
        show(sam.copy(canMoveToTag = false))
        composeRule.onNodeWithContentDescription("Drag to move").assertDoesNotExist()
    }

    @Test
    fun anInvitedRowHasNoDragHandleEvenIfFlaggedMovable() {
        show(invite.copy(canMoveToTag = true))
        composeRule.onNodeWithContentDescription("Drag to move").assertDoesNotExist()
    }

    @Test
    fun aMovingRowReplacesTheHandleWithAProgressIndicator() {
        show(sam, isMoving = true)
        composeRule.onNodeWithContentDescription("Drag to move").assertDoesNotExist()
        composeRule.onNode(spinner).assertExists()
    }

    @Test
    fun anInFlightRowReplacesTheMenuWithAProgressIndicator() {
        show(sam, inFlight = true)
        composeRule.onNodeWithContentDescription("More options").assertDoesNotExist()
        composeRule.onNode(spinner).assertExists()
        composeRule.onNodeWithContentDescription("Drag to move").assertIsDisplayed()
    }

    @Test
    fun aMovingInFlightRowShowsNeitherHandleNorMenu() {
        show(sam, inFlight = true, isMoving = true)
        composeRule.onNodeWithContentDescription("Drag to move").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("More options").assertDoesNotExist()
    }

    @Test
    fun ownRowShowsTheYouBadgeAndAdminStatus() {
        show(self)
        composeRule.onNodeWithText("Alex Doyle").assertIsDisplayed()
        composeRule.onNodeWithText("You").assertIsDisplayed()
        composeRule.onNodeWithText("Admin").assertIsDisplayed()
    }

    @Test
    fun otherRowsHaveNoYouBadge() {
        show(sam)
        composeRule.onNodeWithText("You").assertDoesNotExist()
        composeRule.onNodeWithText("Member").assertIsDisplayed()
    }

    @Test
    fun ownRowMenuOffersMoveToTagOnly() {
        var moved: String? = null
        show(self, actions = MemberRowActions(onMoveToTag = { moved = it }))
        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Make admin").assertDoesNotExist()
        composeRule.onNodeWithText("Make member").assertDoesNotExist()
        composeRule.onNodeWithText("Remove from group").assertDoesNotExist()
        composeRule.onNodeWithText("Move to tag…").performClick()
        assert(moved == "1")
    }

    @Test
    fun aRowWithoutAnyActionHasNoMenu() {
        show(MemberRow(id = "5", displayName = "Dan Whelan", status = MemberStatus.Admin))
        composeRule.onNodeWithContentDescription("More options").assertDoesNotExist()
    }

    @Test
    fun aMemberMenuListsMoveToTagPromoteAndRemove() {
        show(sam)
        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Move to tag…").assertIsDisplayed()
        composeRule.onNodeWithText("Make admin").assertIsDisplayed()
        composeRule.onNodeWithText("Remove from group").assertIsDisplayed()
        composeRule.onNodeWithText("Make member").assertDoesNotExist()
        composeRule.onNodeWithText("Resend invitation").assertDoesNotExist()
    }

    @Test
    fun aRowWithoutTagRightsOmitsMoveToTag() {
        show(sam.copy(canMoveToTag = false))
        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Move to tag…").assertDoesNotExist()
        composeRule.onNodeWithText("Make admin").assertIsDisplayed()
    }

    @Test
    fun anAdminMenuOffersDemoteInsteadOfPromote() {
        show(sam.copy(status = MemberStatus.Admin, canPromote = false, canDemote = true))
        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Make member").assertIsDisplayed()
        composeRule.onNodeWithText("Make admin").assertDoesNotExist()
    }

    @Test
    fun menuItemsReachTheirCallbacksWithTheRowId() {
        var picked = ""
        show(
            sam.copy(canDemote = true),
            actions = MemberRowActions(
                onMoveToTag = { picked += "tag$it," },
                onPromote = { picked += "promote$it," },
                onDemote = { picked += "demote$it," },
                onRemove = { picked += "remove$it," }
            )
        )
        listOf("Move to tag…", "Make admin", "Make member", "Remove from group").forEach {
            composeRule.onNodeWithContentDescription("More options").performClick()
            composeRule.onNodeWithText(it).performClick()
        }
        assert(picked == "tag4,promote4,demote4,remove4,")
    }

    @Test
    fun anInvitedRowShowsTheEmailBadgeAndInvitationItems() {
        var resent: String? = null
        var revoked: String? = null
        show(
            invite,
            actions = MemberRowActions(onResendInvitation = { resent = it }, onRevokeInvitation = { revoked = it })
        )
        composeRule.onNodeWithText("j.moriarty@outlook.com").assertIsDisplayed()
        composeRule.onNodeWithText("Invited").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Drag to move").assertDoesNotExist()

        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Move to tag…").assertDoesNotExist()
        composeRule.onNodeWithText("Resend invitation").performClick()
        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Cancel invitation").performClick()
        assert(resent == "9")
        assert(revoked == "9")
    }

    @Test
    fun anErrorMessageShowsOnTheRow() {
        show(sam, errorMessage = "Can’t connect. Check your connection and try again.")
        composeRule.onNodeWithText("Can’t connect. Check your connection and try again.").assertIsDisplayed()
    }

    @Test
    fun noErrorMessageMeansNoneShown() {
        show(sam)
        composeRule.onNodeWithText("Can’t connect. Check your connection and try again.").assertDoesNotExist()
    }
}
