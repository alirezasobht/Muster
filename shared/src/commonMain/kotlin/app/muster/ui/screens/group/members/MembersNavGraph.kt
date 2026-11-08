package app.muster.ui.screens.group.members

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.toRoute
import app.muster.ui.screens.group.members.addbyemail.AddMemberByEmailRoute
import app.muster.ui.screens.group.members.membertagname.MemberTagNameDialogRoute
import app.muster.ui.screens.group.members.membertagname.MemberTagNameMode
import kotlinx.serialization.Serializable

@Serializable
data class AddMemberByEmail(
    val groupId: String,
    val groupName: String
)

@Serializable
data class CreateMemberTag(
    val groupId: String,
    val memberId: String?
)

@Serializable
data class RenameMemberTag(
    val groupId: String,
    val tagId: String,
    val tagName: String,
    val memberCount: Int
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
    dialog<CreateMemberTag> { entry ->
        val route = entry.toRoute<CreateMemberTag>()

        MemberTagNameDialogRoute(
            groupId = route.groupId,
            mode = MemberTagNameMode.New(route.memberId),
            onDismiss = { navController.popBackStack() }
        )
    }
    dialog<RenameMemberTag> { entry ->
        val route = entry.toRoute<RenameMemberTag>()

        MemberTagNameDialogRoute(
            groupId = route.groupId,
            mode = MemberTagNameMode.Rename(route.tagId, route.tagName, route.memberCount),
            onDismiss = { navController.popBackStack() }
        )
    }
}
