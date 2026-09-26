package app.muster.ui.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import app.muster.ui.common.util.LocalAnimatedVisibilityScope
import app.muster.ui.common.util.LocalSharedTransitionScope
import app.muster.ui.common.util.toDisplayDate
import app.muster.ui.screens.addbyemail.AddMemberByEmailRoute
import app.muster.ui.screens.addplayers.AddPlayersRoute
import app.muster.ui.screens.event.EventRoute
import app.muster.ui.screens.event.EventSummary
import app.muster.ui.screens.group.GroupRoute
import app.muster.ui.screens.home.HomeRoute
import app.muster.ui.screens.launch.LaunchRoute
import app.muster.ui.screens.launch.LaunchUiState
import app.muster.ui.screens.launch.LaunchViewModel
import app.muster.ui.screens.newevent.NewEventRoute
import app.muster.ui.screens.newgroup.NewGroupRoute
import app.muster.ui.screens.privacy.PrivacyPolicyScreen
import app.muster.ui.screens.setname.SetNameRoute
import app.muster.ui.screens.settings.SettingsRoute
import app.muster.ui.screens.signin.EnterCodeRoute
import app.muster.ui.screens.signin.RequestCodeRoute
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
                    CompositionLocalProvider(LocalAnimatedVisibilityScope provides this@composable) {
                        HomeRoute(
                            onSettingsClick = { navController.navigate(Settings) },
                            onGroupClick = { id, name -> navController.navigate(Group(id, name)) },
                            onNewGroupClick = { navController.navigate(NewGroup) }
                        )
                    }
                }
                composable<Settings> {
                    SettingsRoute(
                        onBack = { navController.popBackStack() },
                        onSignOut = launchViewModel::onSignOut,
                        onOpenPrivacyPolicy = { navController.navigate(PrivacyPolicy) }
                    )
                }
                composable<PrivacyPolicy> {
                    PrivacyPolicyScreen(onBack = { navController.popBackStack() })
                }
                composable<NewGroup> {
                    NewGroupRoute(
                        onBack = { navController.popBackStack() },
                        onGroupCreated = { group ->
                            navController.navigate(Group(group.id, group.name)) {
                                popUpTo(NewGroup) { inclusive = true }
                            }
                        }
                    )
                }
                composable<Group> { entry ->
                    val group = entry.toRoute<Group>()
                    CompositionLocalProvider(LocalAnimatedVisibilityScope provides this@composable) {
                        GroupRoute(
                            groupId = group.id,
                            groupName = group.name,
                            onBack = { navController.popBackStack() },
                            onAddMemberByEmail = { navController.navigate(AddMemberByEmail(group.id, group.name)) },
                            onSelectEvent = { groupId, groupName, event ->
                                navController.navigate(
                                    Event(
                                        groupId = groupId,
                                        groupName = groupName,
                                        eventId = event.id,
                                        title = event.title,
                                        date = event.startsAt.toDisplayDate(),
                                        location = event.location.orEmpty(),
                                        capacity = event.capacity,
                                        inCount = event.inCount,
                                        pendingCount = event.pendingCount
                                    )
                                )
                            },
                            onNewEvent = { navController.navigate(NewEvent(group.id, group.name)) }
                        )
                    }
                }
                composable<AddMemberByEmail> { entry ->
                    val route = entry.toRoute<AddMemberByEmail>()
                    AddMemberByEmailRoute(
                        groupId = route.groupId,
                        groupName = route.groupName,
                        onClose = { navController.popBackStack() }
                    )
                }
                composable<NewEvent> { entry ->
                    val route = entry.toRoute<NewEvent>()
                    NewEventRoute(
                        groupId = route.groupId,
                        onBack = { navController.popBackStack() },
                        onEventCreated = { event ->
                            navController.navigate(
                                Event(
                                    groupId = event.groupId,
                                    groupName = route.groupName,
                                    eventId = event.id,
                                    title = event.title,
                                    date = event.startsAt.toDisplayDate(),
                                    location = event.location.orEmpty(),
                                    capacity = event.capacity,
                                    inCount = event.inCount,
                                    pendingCount = event.pendingCount
                                )
                            ) {
                                popUpTo(route) { inclusive = true }
                            }
                        }
                    )
                }
                composable<Event> { entry ->
                    val route = entry.toRoute<Event>()
                    CompositionLocalProvider(LocalAnimatedVisibilityScope provides this@composable) {
                        EventRoute(
                            initialSummary = EventSummary(
                                groupId = route.groupId,
                                eventId = route.eventId,
                                groupName = route.groupName,
                                title = route.title,
                                date = route.date,
                                location = route.location,
                                capacity = route.capacity,
                                inCount = route.inCount,
                                pendingCount = route.pendingCount
                            ),
                            onBack = { navController.popBackStack() },
                            onAddPlayers = { eventId, groupId ->
                                navController.navigate(AddPlayers(eventId, groupId))
                            }
                        )
                    }
                }
                composable<AddPlayers> { entry ->
                    val route = entry.toRoute<AddPlayers>()
                    AddPlayersRoute(
                        eventId = route.eventId,
                        groupId = route.groupId,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
