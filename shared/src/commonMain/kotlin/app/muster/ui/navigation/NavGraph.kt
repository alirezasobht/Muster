package app.muster.ui.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import app.muster.ui.common.components.MusterIcons
import app.muster.ui.common.components.PhoneWidth
import app.muster.ui.common.util.LocalAnimatedVisibilityScope
import app.muster.ui.common.util.LocalSharedTransitionScope
import app.muster.ui.screens.home.HomeRoute
import app.muster.ui.screens.launch.LaunchRoute
import app.muster.ui.screens.launch.LaunchUiState
import app.muster.ui.screens.launch.LaunchViewModel
import app.muster.ui.screens.newgroup.NewGroupRoute
import app.muster.ui.screens.settings.SettingsRoute
import app.muster.ui.screens.setname.SetNameRoute
import app.muster.ui.screens.signin.EnterCodeRoute
import app.muster.ui.screens.signin.RequestCodeRoute
import app.muster.ui.theme.MusterColors
import muster.shared.generated.resources.Res
import muster.shared.generated.resources.content_description_back
import muster.shared.generated.resources.group_placeholder_body
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    launchViewModel: LaunchViewModel = koinViewModel()
) {
    val session by launchViewModel.state.collectAsStateWithLifecycle()

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
                composable<Launch> {
                    CompositionLocalProvider(LocalAnimatedVisibilityScope provides this@composable) {
                        LaunchRoute(viewModel = launchViewModel)
                    }
                }
                composable<RequestCode> {
                    CompositionLocalProvider(LocalAnimatedVisibilityScope provides this@composable) {
                        RequestCodeRoute(
                            onCodeSent = { navController.navigate(EnterCode(it)) }
                        )
                    }
                }
                composable<EnterCode> { entry ->
                    EnterCodeRoute(
                        email = entry.toRoute<EnterCode>().email,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable<SetName> {
                    SetNameRoute(onNameSet = launchViewModel::onNameSet)
                }
                composable<Home> {
                    HomeRoute(
                        onSettingsClick = { navController.navigate(Settings) },
                        onGroupClick = { groupId -> navController.navigate(Group(groupId)) },
                        onNewGroupClick = { navController.navigate(NewGroup) }
                    )
                }
                composable<Settings> {
                    SettingsRoute(
                        onBack = { navController.popBackStack() },
                        onSignOut = launchViewModel::onSignOut
                    )
                }
                composable<NewGroup> {
                    NewGroupRoute(
                        onBack = { navController.popBackStack() },
                        onGroupCreated = { group ->
                            navController.navigate(Group(group.id)) {
                                popUpTo(NewGroup) { inclusive = true }
                            }
                        }
                    )
                }
                composable<Group> { entry ->
                    GroupPlaceholderRoute(
                        groupId = entry.toRoute<Group>().id,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

// Stand-in until the Group screen (events/members tabs) is built.
@Composable
private fun GroupPlaceholderRoute(groupId: String, onBack: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize()) {
        PhoneWidth {
            Column(modifier = Modifier.safeDrawingPadding().fillMaxSize()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = MusterIcons.ArrowBack,
                            contentDescription = stringResource(Res.string.content_description_back),
                            tint = MusterColors.Ink,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(text = stringResource(Res.string.group_placeholder_body) + " ($groupId)")
                }
            }
        }
    }
}
