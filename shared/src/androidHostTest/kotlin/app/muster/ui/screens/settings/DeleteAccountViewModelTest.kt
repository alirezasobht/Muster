package app.muster.ui.screens.settings

import app.muster.data.fake.FAKE_EMAIL
import app.muster.data.fake.FAKE_USER_ID
import app.muster.data.fake.FakeAuthRepository
import app.muster.domain.error.DomainError
import app.muster.domain.model.Group
import app.muster.domain.model.SessionState
import app.muster.domain.usecase.DeleteAccountUseCase
import app.muster.testing.MainDispatcherRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule

class DeleteAccountViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val signedIn = SessionState.SignedIn(userId = FAKE_USER_ID, email = FAKE_EMAIL)
    private val soleAdminGroups = listOf(Group(id = "g1", name = "Sunday League"), Group(id = "g2", name = "Thursday Five"))

    private fun auth(
        soleAdminGroups: List<Group> = emptyList(),
        deleteError: DomainError? = null
    ) = FakeAuthRepository(initial = signedIn, deleteError = deleteError, soleAdminGroups = soleAdminGroups)

    private fun viewModel(auth: FakeAuthRepository) = DeleteAccountViewModel(DeleteAccountUseCase(auth))

    @Test
    fun `starts hidden`() {
        assertEquals(DeleteAccountUiState.Hidden, viewModel(auth()).state.value)
    }

    @Test
    fun `starting opens the confirm dialog`() {
        val viewModel = viewModel(auth())

        viewModel.onStart()

        assertEquals(DeleteAccountUiState.Confirm(), viewModel.state.value)
    }

    @Test
    fun `dismissing closes the dialog without deleting`() = runTest {
        val auth = auth()
        val viewModel = viewModel(auth)
        viewModel.onStart()

        viewModel.onDismiss()
        advanceUntilIdle()

        assertEquals(DeleteAccountUiState.Hidden, viewModel.state.value)
        assertEquals(0, auth.deleteCalls)
    }

    @Test
    fun `confirming with no sole-admin groups deletes and signs out`() = runTest {
        val auth = auth()
        val viewModel = viewModel(auth)
        viewModel.onStart()

        viewModel.onConfirm()
        assertTrue(assertIs<DeleteAccountUiState.Confirm>(viewModel.state.value).deleting)
        advanceUntilIdle()

        assertEquals(SessionState.SignedOut, auth.session.value)
    }

    @Test
    fun `confirming as a sole admin lists the groups and deletes nothing`() = runTest {
        val auth = auth(soleAdminGroups = soleAdminGroups)
        val viewModel = viewModel(auth)
        viewModel.onStart()

        viewModel.onConfirm()
        advanceUntilIdle()

        val state = assertIs<DeleteAccountUiState.SoleAdmin>(viewModel.state.value)
        assertEquals(listOf("Sunday League", "Thursday Five"), state.groupNames)
        assertEquals(false, state.deleting)
        assertEquals(signedIn, auth.session.value)
    }

    @Test
    fun `confirming the sole-admin warning deletes and signs out`() = runTest {
        val auth = auth(soleAdminGroups = soleAdminGroups)
        val viewModel = viewModel(auth)
        viewModel.onStart()
        viewModel.onConfirm()
        advanceUntilIdle()

        viewModel.onConfirm()
        assertTrue(assertIs<DeleteAccountUiState.SoleAdmin>(viewModel.state.value).deleting)
        advanceUntilIdle()

        assertEquals(SessionState.SignedOut, auth.session.value)
        assertEquals(2, auth.deleteCalls)
    }

    @Test
    fun `a failed confirm keeps the dialog open with the error`() = runTest {
        val auth = auth(deleteError = DomainError.Network())
        val viewModel = viewModel(auth)
        viewModel.onStart()

        viewModel.onConfirm()
        advanceUntilIdle()

        val state = assertIs<DeleteAccountUiState.Confirm>(viewModel.state.value)
        assertIs<DomainError.Network>(state.error)
        assertEquals(false, state.deleting)
        assertEquals(signedIn, auth.session.value)
    }

    @Test
    fun `a failed forced delete keeps the groups and shows the error`() = runTest {
        val auth = auth(soleAdminGroups = soleAdminGroups)
        val viewModel = viewModel(auth)
        viewModel.onStart()
        viewModel.onConfirm()
        advanceUntilIdle()

        auth.deleteError = DomainError.Network()
        viewModel.onConfirm()
        advanceUntilIdle()

        val state = assertIs<DeleteAccountUiState.SoleAdmin>(viewModel.state.value)
        assertEquals(listOf("Sunday League", "Thursday Five"), state.groupNames)
        assertIs<DomainError.Network>(state.error)
        assertEquals(false, state.deleting)
        assertEquals(signedIn, auth.session.value)
    }

    @Test
    fun `retrying clears the previous error`() = runTest {
        val auth = auth(deleteError = DomainError.Network())
        val viewModel = viewModel(auth)
        viewModel.onStart()
        viewModel.onConfirm()
        advanceUntilIdle()

        auth.deleteError = null
        viewModel.onConfirm()

        assertNull(assertIs<DeleteAccountUiState.Confirm>(viewModel.state.value).error)
        advanceUntilIdle()
        assertEquals(SessionState.SignedOut, auth.session.value)
    }

    @Test
    fun `dismissing while deleting is ignored`() = runTest {
        val viewModel = viewModel(auth())
        viewModel.onStart()
        viewModel.onConfirm()

        viewModel.onDismiss()

        assertTrue(assertIs<DeleteAccountUiState.Confirm>(viewModel.state.value).deleting)
    }

    @Test
    fun `a second tap while deleting is ignored`() = runTest {
        val auth = auth()
        val viewModel = viewModel(auth)
        viewModel.onStart()

        viewModel.onConfirm()
        viewModel.onConfirm()
        advanceUntilIdle()

        assertEquals(1, auth.deleteCalls)
    }
}
