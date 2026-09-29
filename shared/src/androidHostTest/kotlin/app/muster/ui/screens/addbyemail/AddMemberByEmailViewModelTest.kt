package app.muster.ui.screens.addbyemail

import app.muster.data.fake.FakeMemberRepository
import app.muster.domain.error.DomainError
import app.muster.domain.usecase.InviteByEmailUseCase
import app.muster.testing.MainDispatcherRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule

class AddMemberByEmailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val groupId = "group-1"

    private fun viewModel(members: FakeMemberRepository = FakeMemberRepository()) = AddMemberByEmailViewModel(groupId, InviteByEmailUseCase(members))

    @Test
    fun `starts empty`() {
        assertEquals(AddMemberByEmailUiState(), viewModel().state.value)
    }

    @Test
    fun `inviting shows the success dialog`() = runTest {
        val members = FakeMemberRepository()
        val viewModel = viewModel(members)
        viewModel.onEmailChange("priya.n@gmail.com")
        viewModel.onInvite()
        assertTrue(viewModel.state.value.sending)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(false, state.sending)
        assertEquals(true, state.invited)
        assertNull(state.error)
        assertEquals(listOf("priya.n@gmail.com"), members.invitedEmails)
    }

    @Test
    fun `the address is trimmed before inviting`() = runTest {
        val members = FakeMemberRepository()
        val viewModel = viewModel(members)
        viewModel.onEmailChange("  priya.n@gmail.com  ")
        viewModel.onInvite()
        advanceUntilIdle()

        assertEquals(listOf("priya.n@gmail.com"), members.invitedEmails)
    }

    @Test
    fun `a blank address invites nothing`() = runTest {
        val members = FakeMemberRepository()
        val viewModel = viewModel(members)
        viewModel.onEmailChange("   ")
        viewModel.onInvite()
        advanceUntilIdle()

        assertTrue(members.invitedEmails.isEmpty())
        assertEquals(false, viewModel.state.value.invited)
    }

    @Test
    fun `a second tap while sending is ignored`() = runTest {
        val members = FakeMemberRepository()
        val viewModel = viewModel(members)
        viewModel.onEmailChange("priya.n@gmail.com")
        viewModel.onInvite()
        viewModel.onInvite()
        advanceUntilIdle()

        assertEquals(1, members.invitedEmails.size)
    }

    // AlreadyInvited and AlreadyMember are both about the address that was
    // typed, so they land under the field, not above the button.
    @Test
    fun `an already-invited address is a field error`() = runTest {
        val members = FakeMemberRepository(inviteByEmailError = DomainError.AlreadyInvited())
        val viewModel = viewModel(members)
        viewModel.onEmailChange("priya.n@gmail.com")
        viewModel.onInvite()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertIs<DomainError.AlreadyInvited>(state.emailError)
        assertNull(state.error)
        assertEquals(false, state.sending)
        assertEquals(false, state.invited)
    }

    @Test
    fun `an already-member address is a field error`() = runTest {
        val members = FakeMemberRepository(inviteByEmailError = DomainError.AlreadyMember())
        val viewModel = viewModel(members)
        viewModel.onEmailChange("priya.n@gmail.com")
        viewModel.onInvite()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertIs<DomainError.AlreadyMember>(state.emailError)
        assertNull(state.error)
    }

    // Retryable: the button is the retry, so it stays live.
    @Test
    fun `a network error leaves canInvite true`() = runTest {
        val members = FakeMemberRepository(inviteByEmailError = DomainError.Network())
        val viewModel = viewModel(members)
        viewModel.onEmailChange("priya.n@gmail.com")
        viewModel.onInvite()
        advanceUntilIdle()

        assertIs<DomainError.Network>(viewModel.state.value.error)
        assertTrue(viewModel.state.value.canInvite)
    }

    @Test
    fun `typing clears both errors`() = runTest {
        val members = FakeMemberRepository(inviteByEmailError = DomainError.Network())
        val viewModel = viewModel(members)
        viewModel.onEmailChange("priya.n@gmail.com")
        viewModel.onInvite()
        advanceUntilIdle()
        assertIs<DomainError.Network>(viewModel.state.value.error)

        viewModel.onEmailChange("priya.n2@gmail.com")
        assertNull(viewModel.state.value.error)
        assertNull(viewModel.state.value.emailError)
    }

    @Test
    fun `invite more clears the field and dismisses the dialog`() = runTest {
        val viewModel = viewModel()
        viewModel.onEmailChange("priya.n@gmail.com")
        viewModel.onInvite()
        advanceUntilIdle()
        assertTrue(viewModel.state.value.invited)

        viewModel.onInviteMore()
        assertEquals(AddMemberByEmailUiState(), viewModel.state.value)
    }
}
