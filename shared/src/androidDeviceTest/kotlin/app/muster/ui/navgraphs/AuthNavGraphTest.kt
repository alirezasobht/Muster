package app.muster.ui.navgraphs

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import app.muster.data.fake.FAKE_EMAIL
import app.muster.data.fake.FAKE_USER_ID
import app.muster.data.fake.FakeAuthRepository
import app.muster.data.fake.FakeProfileRepository
import app.muster.domain.model.SessionState
import app.muster.domain.usecase.GetMyProfileUseCase
import app.muster.domain.usecase.ObserveSessionUseCase
import app.muster.domain.usecase.RetrySessionUseCase
import app.muster.domain.usecase.SignOutUseCase
import app.muster.ui.screens.auth.EnterCode
import app.muster.ui.screens.auth.Launch
import app.muster.ui.screens.auth.RequestCode
import app.muster.ui.screens.auth.SetName
import app.muster.ui.screens.auth.authGraph
import app.muster.ui.screens.auth.launch.LaunchUiState
import app.muster.ui.screens.auth.launch.LaunchViewModel
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test

class AuthNavGraphTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val harness = NavGraphHarness(composeRule)

    private fun show(
        start: Any = RequestCode,
        session: SessionState = SessionState.SignedOut
    ): LaunchViewModel {
        val auth = FakeAuthRepository(initial = session, latency = 0)
        val launchViewModel = LaunchViewModel(
            ObserveSessionUseCase(auth),
            GetMyProfileUseCase(FakeProfileRepository(latency = 0)),
            RetrySessionUseCase(auth),
            SignOutUseCase(auth)
        )
        harness.show(start) { nav -> authGraph(nav, launchViewModel) }
        return launchViewModel
    }

    @Test
    fun sendingACodeOpensEnterCodeWithTheEmail() {
        show()
        composeRule.onNode(hasSetTextAction()).performTextInput("alex.doyle@gmail.com")
        composeRule.onNodeWithText("Send code").performClick()

        harness.assertAt<EnterCode>()
        assertEquals(EnterCode(email = "alex.doyle@gmail.com"), harness.route<EnterCode>())
    }

    @Test
    fun backFromEnterCodeReturnsToRequestCode() {
        show()
        composeRule.onNode(hasSetTextAction()).performTextInput("alex.doyle@gmail.com")
        composeRule.onNodeWithText("Send code").performClick()
        harness.assertAt<EnterCode>()

        composeRule.onNodeWithContentDescription("Back").performClick()

        harness.assertAt<RequestCode>()
    }

    @Test
    fun setNameShowsItsScreen() {
        show(start = SetName)
        harness.waitForText("Continue")
        harness.assertAt<SetName>()
    }

    @Test
    fun savingANameHandsTheProfileToTheLaunchViewModel() {
        val launchViewModel = show(start = SetName, session = SessionState.SignedIn(FAKE_USER_ID, FAKE_EMAIL))
        harness.waitForText("Continue")
        composeRule.onNode(hasSetTextAction()).performTextInput("Alex Doyle")
        composeRule.onNodeWithText("Continue").performClick()

        composeRule.waitUntil(NAV_WAIT_MILLIS) { launchViewModel.state.value is LaunchUiState.Ready }
        assertEquals("Alex Doyle", (launchViewModel.state.value as LaunchUiState.Ready).profile.name)
    }

    @Test
    fun launchShowsItsScreenWhenTheSessionIsUnreachable() {
        show(start = Launch, session = SessionState.Unreachable)
        harness.waitForText("Couldn’t reach Muster")
        harness.assertAt<Launch>()
    }
}
