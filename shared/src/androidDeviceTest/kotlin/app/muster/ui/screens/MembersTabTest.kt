package app.muster.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.muster.domain.error.DomainError
import app.muster.ui.screens.group.members.MemberRow
import app.muster.ui.screens.group.members.MemberStatus
import app.muster.ui.screens.group.members.MembersActions
import app.muster.ui.screens.group.members.MembersTab
import app.muster.ui.screens.group.members.MembersUiState
import app.muster.ui.theme.MusterTheme
import org.junit.Rule
import org.junit.Test

class MembersTabTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val self = MemberRow(id = "1", displayName = "Alex Doyle", status = MemberStatus.Admin, isSelf = true)

    private fun show(
        state: MembersUiState,
        actionErrorMessage: String? = null,
        onAddMemberByEmail: () -> Unit = {},
        onPromote: (String) -> Unit = {},
        onDemote: (String) -> Unit = {},
        onRemove: (String) -> Unit = {},
        onRevokeInvitation: (String) -> Unit = {},
        onResendInvitation: (String) -> Unit = {},
        onRetry: () -> Unit = {}
    ) {
        composeRule.setContent {
            MusterTheme {
                MembersTab(
                    state = state,
                    actions = MembersActions(
                        onAddMemberByEmail = onAddMemberByEmail,
                        onPromote = onPromote,
                        onDemote = onDemote,
                        onRemove = onRemove,
                        onRevokeInvitation = onRevokeInvitation,
                        onResendInvitation = onResendInvitation,
                        onRetry = onRetry
                    ),
                    actionErrorMessage = actionErrorMessage,
                    spinnerDelayMillis = 0
                )
            }
        }
    }

    @Test
    fun loadingShowsNoRows() {
        show(state = MembersUiState.Loading)
        composeRule.onNodeWithText("Add by email").assertDoesNotExist()
    }

    @Test
    fun failedShowsTheErrorMessage() {
        show(state = MembersUiState.Error(DomainError.Network()))
        composeRule.onNodeWithText("Couldn’t load members").assertIsDisplayed()
        composeRule
            .onNodeWithText("Can’t connect. Check your connection and try again.")
            .assertIsDisplayed()
    }

    @Test
    fun failedTryAgainReachesTheCallback() {
        var retried = false
        show(state = MembersUiState.Error(DomainError.Network()), onRetry = { retried = true })
        composeRule.onNodeWithText("Try again").performClick()
        assert(retried)
    }

    @Test
    fun addMemberByEmailIsShownOnlyWhenAllowed() {
        show(state = MembersUiState.Success(rows = listOf(self), canAddMembers = true))
        composeRule.onNodeWithText("Add by email").assertIsDisplayed()
    }

    @Test
    fun addMemberByEmailIsHiddenForNonAdmins() {
        show(state = MembersUiState.Success(rows = listOf(self.copy(isSelf = false)), canAddMembers = false))
        composeRule.onNodeWithText("Add by email").assertDoesNotExist()
    }

    @Test
    fun tappingAddMemberByEmailReachesTheCallback() {
        var tapped = false
        show(
            state = MembersUiState.Success(rows = listOf(self), canAddMembers = true),
            onAddMemberByEmail = { tapped = true }
        )
        composeRule.onNodeWithText("Add by email").performClick()
        assert(tapped)
    }

    @Test
    fun theEmptyStateIsShownWhenOnlySelfRemains() {
        show(state = MembersUiState.Success(rows = listOf(self), canAddMembers = true))
        composeRule.onNodeWithText("It’s just you for now").assertIsDisplayed()
    }

    @Test
    fun selfRowShowsTheYouSuffixAndNoMenu() {
        show(
            state = MembersUiState.Success(
                rows = listOf(
                    self,
                    MemberRow(
                        id = "2",
                        displayName = "Sam Okafor",
                        status = MemberStatus.Member,
                        canPromote = true,
                        canRemove = true
                    )
                )
            )
        )
        composeRule.onNodeWithText("Alex Doyle · you").assertIsDisplayed()
        // Only Sam's row has a menu — a single match proves self's row has none.
        composeRule.onNodeWithContentDescription("More options").assertIsDisplayed()
    }

    @Test
    fun aPendingRowShowsTheEmailAndInvitedBadge() {
        show(
            state = MembersUiState.Success(
                rows = listOf(
                    self,
                    MemberRow(
                        id = "2",
                        displayName = "j.moriarty@outlook.com",
                        status = MemberStatus.Pending,
                        canRevokeInvitation = true
                    )
                ),
                canAddMembers = true
            )
        )
        composeRule.onNodeWithText("j.moriarty@outlook.com").assertIsDisplayed()
        composeRule.onNodeWithText("Invited").assertIsDisplayed()
    }

    // Every admin action on a row asks for confirmation first
    @Test
    fun promotingAMemberAsksForConfirmationBeforeCallingBack() {
        var promoted: String? = null
        show(
            state = MembersUiState.Success(
                rows = listOf(
                    self,
                    MemberRow(
                        id = "2",
                        displayName = "Sam Okafor",
                        status = MemberStatus.Member,
                        canPromote = true,
                        canRemove = true
                    )
                )
            ),
            onPromote = { promoted = it }
        )
        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Make admin").performClick()

        composeRule.onNodeWithText("Make Sam Okafor an admin?").assertIsDisplayed()
        assert(promoted == null)

        composeRule.onNodeWithText("Make admin").performClick()
        assert(promoted == "2")
    }

    @Test
    fun dismissingThePromoteConfirmationDoesNotCallBack() {
        var promoted: String? = null
        show(
            state = MembersUiState.Success(
                rows = listOf(
                    self,
                    MemberRow(
                        id = "2",
                        displayName = "Sam Okafor",
                        status = MemberStatus.Member,
                        canPromote = true,
                        canRemove = true
                    )
                )
            ),
            onPromote = { promoted = it }
        )
        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Make admin").performClick()
        composeRule.onNodeWithText("Cancel").performClick()

        composeRule.onNodeWithText("Make Sam Okafor an admin?").assertDoesNotExist()
        assert(promoted == null)
    }

    @Test
    fun demotingAnAdminAsksForConfirmationBeforeCallingBack() {
        var demoted: String? = null
        show(
            state = MembersUiState.Success(
                rows = listOf(
                    self,
                    MemberRow(
                        id = "2",
                        displayName = "Dan Whelan",
                        status = MemberStatus.Admin,
                        canDemote = true,
                        canRemove = true
                    )
                )
            ),
            onDemote = { demoted = it }
        )
        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Make member").performClick()

        composeRule.onNodeWithText("Make Dan Whelan a member?").assertIsDisplayed()
        assert(demoted == null)

        composeRule.onNodeWithText("Make member").performClick()
        assert(demoted == "2")
    }

    @Test
    fun dismissingTheDemoteConfirmationDoesNotCallBack() {
        var demoted: String? = null
        show(
            state = MembersUiState.Success(
                rows = listOf(
                    self,
                    MemberRow(
                        id = "2",
                        displayName = "Dan Whelan",
                        status = MemberStatus.Admin,
                        canDemote = true,
                        canRemove = true
                    )
                )
            ),
            onDemote = { demoted = it }
        )
        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Make member").performClick()
        composeRule.onNodeWithText("Cancel").performClick()

        composeRule.onNodeWithText("Make Dan Whelan a member?").assertDoesNotExist()
        assert(demoted == null)
    }

    @Test
    fun revokingAnInvitationAsksForConfirmationBeforeCallingBack() {
        var revoked: String? = null
        show(
            state = MembersUiState.Success(
                rows = listOf(
                    self,
                    MemberRow(
                        id = "2",
                        displayName = "j.moriarty@outlook.com",
                        status = MemberStatus.Pending,
                        canRevokeInvitation = true
                    )
                ),
                canAddMembers = true
            ),
            onRevokeInvitation = { revoked = it }
        )
        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Cancel invitation").performClick()

        composeRule.onNodeWithText("Cancel invitation to j.moriarty@outlook.com?").assertIsDisplayed()
        assert(revoked == null)

        composeRule.onNodeWithText("Cancel invitation").performClick()
        assert(revoked == "2")
    }

    @Test
    fun dismissingTheRevokeConfirmationDoesNotCallBack() {
        var revoked: String? = null
        show(
            state = MembersUiState.Success(
                rows = listOf(
                    self,
                    MemberRow(
                        id = "2",
                        displayName = "j.moriarty@outlook.com",
                        status = MemberStatus.Pending,
                        canRevokeInvitation = true
                    )
                ),
                canAddMembers = true
            ),
            onRevokeInvitation = { revoked = it }
        )
        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Cancel invitation").performClick()
        composeRule.onNodeWithText("Keep invitation").performClick()

        composeRule.onNodeWithText("Cancel invitation to j.moriarty@outlook.com?").assertDoesNotExist()
        assert(revoked == null)
    }

    @Test
    fun resendingAnInvitationAsksForConfirmationBeforeCallingBack() {
        var resent: String? = null
        show(
            state = MembersUiState.Success(
                rows = listOf(
                    self,
                    MemberRow(
                        id = "2",
                        displayName = "j.moriarty@outlook.com",
                        status = MemberStatus.Pending,
                        canRevokeInvitation = true,
                        canResendInvitation = true
                    )
                ),
                canAddMembers = true
            ),
            onResendInvitation = { resent = it }
        )
        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Resend invitation").performClick()

        composeRule.onNodeWithText("Resend invitation to j.moriarty@outlook.com?").assertIsDisplayed()
        assert(resent == null)

        composeRule.onNodeWithText("Resend").performClick()
        assert(resent == "2")
    }

    @Test
    fun dismissingTheResendConfirmationDoesNotCallBack() {
        var resent: String? = null
        show(
            state = MembersUiState.Success(
                rows = listOf(
                    self,
                    MemberRow(
                        id = "2",
                        displayName = "j.moriarty@outlook.com",
                        status = MemberStatus.Pending,
                        canRevokeInvitation = true,
                        canResendInvitation = true
                    )
                ),
                canAddMembers = true
            ),
            onResendInvitation = { resent = it }
        )
        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Resend invitation").performClick()
        composeRule.onNodeWithText("Cancel").performClick()

        composeRule.onNodeWithText("Resend invitation to j.moriarty@outlook.com?").assertDoesNotExist()
        assert(resent == null)
    }

    @Test
    fun removingAMemberAsksForConfirmationBeforeCallingBack() {
        var removed: String? = null
        show(
            state = MembersUiState.Success(
                rows = listOf(
                    self,
                    MemberRow(
                        id = "2",
                        displayName = "Sam Okafor",
                        status = MemberStatus.Member,
                        canPromote = true,
                        canRemove = true
                    )
                )
            ),
            onRemove = { removed = it }
        )
        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Remove from group").performClick()

        composeRule.onNodeWithText("Remove Sam Okafor?").assertIsDisplayed()
        assert(removed == null)

        composeRule.onNodeWithText("Remove").performClick()
        assert(removed == "2")
    }

    @Test
    fun dismissingTheRemoveConfirmationDoesNotCallBack() {
        var removed: String? = null
        show(
            state = MembersUiState.Success(
                rows = listOf(
                    self,
                    MemberRow(
                        id = "2",
                        displayName = "Sam Okafor",
                        status = MemberStatus.Member,
                        canPromote = true,
                        canRemove = true
                    )
                )
            ),
            onRemove = { removed = it }
        )
        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Remove from group").performClick()
        composeRule.onNodeWithText("Cancel").performClick()

        composeRule.onNodeWithText("Remove Sam Okafor?").assertDoesNotExist()
        assert(removed == null)
    }

    @Test
    fun anInFlightActionHidesTheRowMenu() {
        show(
            state = MembersUiState.Success(
                rows = listOf(
                    self,
                    MemberRow(
                        id = "2",
                        displayName = "Sam Okafor",
                        status = MemberStatus.Member,
                        canPromote = true,
                        canRemove = true
                    )
                ),
                actionTargetId = "2"
            )
        )
        composeRule.onNodeWithContentDescription("More options").assertDoesNotExist()
    }

    @Test
    fun aRowLevelErrorIsShownUnderThatRow() {
        show(
            state = MembersUiState.Success(
                rows = listOf(
                    self,
                    MemberRow(
                        id = "2",
                        displayName = "Sam Okafor",
                        status = MemberStatus.Member,
                        canPromote = true,
                        canRemove = true
                    )
                ),
                failedActionId = "2"
            ),
            actionErrorMessage = "Can’t connect. Check your connection and try again."
        )
        composeRule.onNodeWithText("Can’t connect. Check your connection and try again.").assertIsDisplayed()
    }
}
