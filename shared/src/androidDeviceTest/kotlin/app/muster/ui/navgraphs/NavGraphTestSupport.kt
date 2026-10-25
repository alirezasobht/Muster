package app.muster.ui.navgraphs

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import app.muster.data.fake.FAKE_EMAIL
import app.muster.data.fake.FAKE_USER_ID
import app.muster.data.fake.FakeAuthRepository
import app.muster.data.fake.FakeEventRepository
import app.muster.data.fake.FakeGroupRepository
import app.muster.data.fake.FakeMemberRepository
import app.muster.data.fake.FakeProfileRepository
import app.muster.domain.di.domainModule
import app.muster.domain.event.DataChanges
import app.muster.domain.model.Event
import app.muster.domain.model.EventDetail
import app.muster.domain.model.Group
import app.muster.domain.model.GroupRole
import app.muster.domain.model.Member
import app.muster.domain.model.SessionState
import app.muster.domain.repository.AuthRepository
import app.muster.domain.repository.EventRepository
import app.muster.domain.repository.GroupRepository
import app.muster.domain.repository.MemberRepository
import app.muster.domain.repository.ProfileRepository
import app.muster.ui.di.uiModule
import app.muster.ui.theme.MusterTheme
import kotlin.time.Instant
import org.koin.compose.KoinApplication
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.mp.KoinPlatform

const val NAV_WAIT_MILLIS = 5_000L

val TestGroup = Group(id = "g1", name = "Sunday League")

val TestEvent = Event(
    id = "e1",
    groupId = "g1",
    title = "Weekly 7s",
    startsAt = Instant.parse("2099-01-01T09:00:00Z"),
    location = "Westgate Pitch 2",
    capacity = 10,
    inCount = 0,
    pendingCount = 0,
    myStatus = null
)

// Real screens behind Koin with the repository fakes. Graphs that navigate into another
// graph are given a stub for that route by the test, so only the navigation is asserted.
fun fakeRepositoriesModule(
    myRole: GroupRole = GroupRole.Admin,
    session: SessionState = SessionState.SignedIn(userId = FAKE_USER_ID, email = FAKE_EMAIL)
) = module {
    single { DataChanges() }
    single<AuthRepository> { FakeAuthRepository(initial = session, latency = 0) }
    single<ProfileRepository> { FakeProfileRepository(name = "Alex", canCreateGroups = true, latency = 0) }
    single<GroupRepository> { FakeGroupRepository(groups = listOf(TestGroup), group = TestGroup, myRole = myRole, latency = 0) }
    single<MemberRepository> {
        FakeMemberRepository(
            members = listOf(Member(profileId = FAKE_USER_ID, name = "Alex", role = GroupRole.Admin)),
            latency = 0
        )
    }
    single<EventRepository> {
        FakeEventRepository(
            events = listOf(TestEvent),
            eventDetail = EventDetail(event = TestEvent, roster = emptyList(), standby = emptyList()),
            latency = 0
        )
    }
}

class NavGraphHarness(val rule: ComposeContentTestRule) {

    lateinit var navController: NavHostController

    // KoinApplication starts the global Koin and never stops it, so a later test would
    // re-attach to the first test's modules and fakes.
    private fun startFreshKoin() {
        if (KoinPlatform.getKoinOrNull() != null) stopKoin()
    }

    fun show(
        start: Any,
        modules: List<Module> = listOf(fakeRepositoriesModule()),
        graph: NavGraphBuilder.(NavHostController) -> Unit
    ) {
        startFreshKoin()
        rule.setContent {
            MusterTheme {
                KoinApplication(application = { modules(domainModule, uiModule, *modules.toTypedArray()) }) {
                    navController = rememberNavController()
                    NavHost(navController, startDestination = start) { graph(navController) }
                }
            }
        }
    }

    // For content that owns its NavHost, such as the root NavGraph.
    fun showApp(
        modules: List<Module>,
        content: @Composable (NavHostController) -> Unit
    ) {
        startFreshKoin()
        rule.setContent {
            MusterTheme {
                KoinApplication(application = { modules(domainModule, uiModule, *modules.toTypedArray()) }) {
                    navController = rememberNavController()
                    content(navController)
                }
            }
        }
    }

    fun waitForText(text: String) {
        rule.waitUntil(NAV_WAIT_MILLIS) { rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }

    inline fun <reified T : Any> assertAt() {
        try {
            rule.waitUntil(NAV_WAIT_MILLIS) {
                rule.runOnIdle { navController.currentBackStackEntry?.destination?.hasRoute<T>() == true }
            }
        } catch (e: ComposeTimeoutException) {
            val stack = rule.runOnIdle { navController.currentBackStack.value.map { it.destination.route } }
            throw AssertionError("Expected ${T::class.simpleName} but the back stack is $stack", e)
        }
    }

    inline fun <reified T : Any> route(): T = rule.runOnIdle { navController.currentBackStackEntry!!.toRoute<T>() }

    inline fun <reified T : Any> onBackStack(): Boolean = rule.runOnIdle {
        navController.currentBackStack.value.any { it.destination.hasRoute<T>() }
    }
}
