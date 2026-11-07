package app.muster.ui.screens.group.members

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import app.muster.ui.screens.group.members.addbyemail.AddMemberByEmailRoute
import kotlinx.serialization.Serializable

@Serializable
data class AddMemberByEmail(
    val groupId: String,
    val groupName: String
)

fun NavGraphBuilder.membersGraph(navController: NavController) {
    composable<AddMemberByEmail> { entry ->
        val route = entry.toRoute<AddMemberByEmail>()
        AddMemberByEmailRoute(
            groupId = route.groupId,
            groupName = route.groupName,
            onClose = { navController.popBackStack() }
        )
    }
}
