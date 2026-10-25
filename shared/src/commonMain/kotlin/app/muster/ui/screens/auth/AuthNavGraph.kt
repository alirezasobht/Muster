package app.muster.ui.screens.auth

import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import app.muster.ui.common.util.LocalAnimatedVisibilityScope
import app.muster.ui.screens.auth.launch.LaunchRoute
import app.muster.ui.screens.auth.launch.LaunchViewModel
import app.muster.ui.screens.auth.setname.SetNameRoute
import app.muster.ui.screens.auth.signin.EnterCodeRoute
import app.muster.ui.screens.auth.signin.RequestCodeRoute
import kotlinx.serialization.Serializable

@Serializable
data object Launch

@Serializable
data object RequestCode

@Serializable
data class EnterCode(val email: String)

@Serializable
data object SetName

fun NavGraphBuilder.authGraph(
    navController: NavController,
    launchViewModel: LaunchViewModel
) {
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
}
