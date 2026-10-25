package app.muster.ui.navgraphs

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.muster.data.fake.FAKE_EMAIL
import app.muster.data.fake.FAKE_USER_ID
import app.muster.data.fake.FakeAuthRepository
import app.muster.data.fake.FakeGroupRepository
import app.muster.data.fake.FakeProfileRepository
import app.muster.domain.event.DataChanges
import app.muster.domain.model.SessionState
import app.muster.domain.repository.AuthRepository
import app.muster.domain.repository.GroupRepository
import app.muster.domain.repository.ProfileRepository
import app.muster.ui.navigation.NavGraph
import app.muster.ui.screens.auth.Launch
import app.muster.ui.screens.auth.RequestCode
import app.muster.ui.screens.auth.SetName
import app.muster.ui.screens.home.Home
import app.muster.ui.screens.home.Settings
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.koin.dsl.module

// The whole NavGraph, so the session routing in NavGraph.kt runs against the real graphs.
// The launch screen holds for a minimum of one second, which the waits below absorb.
class RootNavGraphTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val harness = NavGraphHarness(composeRule)

    private val signedIn = SessionState.SignedIn(userId = FAKE_USER_ID, email = FAKE_EMAIL)

    private fun show(
        auth: FakeAuthRepository,
        profiles: FakeProfileRepository = FakeProfileRepository(name = "Alex", latency = 0)
    ) {
        val fakes = module {
            single { DataChanges() }
            single<AuthRepository> { auth }
            single<ProfileRepository> { profiles }
            single<GroupRepository> { FakeGroupRepository(groups = listOf(TestGroup), latency = 0) }
        }
        harness.showApp(listOf(fakes)) { nav -> NavGraph(navController = nav) }
    }

    @Test
    fun signedOutRoutesToRequestCode() {
        show(FakeAuthRepository(initial = SessionState.SignedOut, latency = 0))
        harness.assertAt<RequestCode>()
    }

    @Test
    fun signedInWithoutANameRoutesToSetName() {
        show(
            FakeAuthRepository(initial = signedIn, latency = 0),
            FakeProfileRepository(name = null, latency = 0)
        )
        harness.assertAt<SetName>()
    }

    @Test
    fun signedInWithANameRoutesToHome() {
        show(FakeAuthRepository(initial = signedIn, latency = 0))
        harness.assertAt<Home>()
    }

    @Test
    fun anUnreachableSessionStaysOnLaunchWithTheFailedScreen() {
        show(FakeAuthRepository(initial = SessionState.Unreachable, latency = 0))
        composeRule.waitUntil(NAV_WAIT_MILLIS) {
            composeRule.onAllNodesWithText("Couldn’t reach Muster").fetchSemanticsNodes().isNotEmpty()
        }
        harness.assertAt<Launch>()
    }

    @Test
    fun signingInClearsTheBackStackAndRoutesToHome() {
        val auth = FakeAuthRepository(initial = SessionState.SignedOut, latency = 0)
        show(auth)
        harness.assertAt<RequestCode>()

        auth.emit(signedIn)

        harness.assertAt<Home>()
        assertTrue(!harness.onBackStack<RequestCode>())
    }

    @Test
    fun signingOutFromSettingsClearsTheBackStackAndRoutesToRequestCode() {
        show(FakeAuthRepository(initial = signedIn, latency = 0))
        harness.assertAt<Home>()
        composeRule.onNodeWithText("Settings").performClick()
        harness.assertAt<Settings>()

        composeRule.onNodeWithText("Sign out").performClick()

        harness.assertAt<RequestCode>()
        assertTrue(!harness.onBackStack<Home>())
        assertTrue(!harness.onBackStack<Settings>())
    }
}
