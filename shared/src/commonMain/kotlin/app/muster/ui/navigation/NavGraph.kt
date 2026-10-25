package app.muster.ui.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.muster.ui.common.util.LocalSharedTransitionScope
import app.muster.ui.platform.SystemBarIcons
import app.muster.ui.screens.auth.Launch
import app.muster.ui.screens.auth.RequestCode
import app.muster.ui.screens.auth.SetName
import app.muster.ui.screens.auth.authGraph
import app.muster.ui.screens.auth.launch.LaunchUiState
import app.muster.ui.screens.auth.launch.LaunchViewModel
import app.muster.ui.screens.event.eventGraph
import app.muster.ui.screens.group.groupGraph
import app.muster.ui.screens.home.Home
import app.muster.ui.screens.home.homeGraph
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    launchViewModel: LaunchViewModel = koinViewModel()
) {
    val session by launchViewModel.state.collectAsStateWithLifecycle()

    val current = navController.currentBackStackEntryAsState().value?.destination
    val onGreen = current == null || current.hasRoute<Launch>() || current.hasRoute<RequestCode>()
    SystemBarIcons(dark = !onGreen)

    LaunchedEffect(session) {
        val destination: Any? = when (session) {
            LaunchUiState.Loading -> null
            // Failed is only ever reached before routing completes, so this
            // cannot interrupt a signed-in session. Without it a failure just
            // after verifying would strand the user on a locked 1b.
            LaunchUiState.Failed -> Launch
            LaunchUiState.SignedOut -> RequestCode
            LaunchUiState.NeedsName -> SetName
            is LaunchUiState.Ready -> Home
        }
        // Session changes reset the stack: there is no going back to a screen
        // that belonged to a different sign-in state.
        if (destination != null) {
            navController.navigate(destination) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    SharedTransitionLayout {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            NavHost(navController = navController, startDestination = Launch) {
                authGraph(navController, launchViewModel)
                homeGraph(navController, onSignOut = launchViewModel::onSignOut)
                groupGraph(navController)
                eventGraph(navController)
            }
        }
    }
}
