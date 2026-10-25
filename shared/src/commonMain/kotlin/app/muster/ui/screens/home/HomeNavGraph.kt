package app.muster.ui.screens.home

import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.muster.ui.common.util.LocalAnimatedVisibilityScope
import app.muster.ui.screens.group.Group
import app.muster.ui.screens.home.newgroup.NewGroupRoute
import app.muster.ui.screens.home.settings.SettingsRoute
import app.muster.ui.screens.home.settings.privacy.PrivacyPolicyScreen
import kotlinx.serialization.Serializable

@Serializable
data object Home

@Serializable
data object Settings

@Serializable
data object PrivacyPolicy

@Serializable
data object NewGroup

fun NavGraphBuilder.homeGraph(
    navController: NavController,
    onSignOut: () -> Unit
) {
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
            onSignOut = onSignOut,
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
}
