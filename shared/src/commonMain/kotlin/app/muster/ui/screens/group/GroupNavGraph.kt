package app.muster.ui.screens.group

import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import app.muster.ui.common.util.LocalAnimatedVisibilityScope
import app.muster.ui.common.util.toDisplayDate
import app.muster.ui.screens.event.Event
import app.muster.ui.screens.group.editgroup.EditGroupRoute
import app.muster.ui.screens.group.events.NewEvent
import app.muster.ui.screens.group.events.eventsGraph
import app.muster.ui.screens.group.members.AddMemberByEmail
import app.muster.ui.screens.group.members.membersGraph
import kotlinx.serialization.Serializable

@Serializable
data class Group(
    val id: String,
    val name: String
)

@Serializable
data class EditGroup(
    val groupId: String,
    val groupName: String
)

fun NavGraphBuilder.groupGraph(navController: NavController) {
    composable<Group> { entry ->
        val group = entry.toRoute<Group>()
        CompositionLocalProvider(LocalAnimatedVisibilityScope provides this@composable) {
            GroupRoute(
                groupId = group.id,
                groupName = group.name,
                onBack = { navController.popBackStack() },
                onEditGroup = { name -> navController.navigate(EditGroup(group.id, name)) },
                onAddMemberByEmail = { name -> navController.navigate(AddMemberByEmail(group.id, name)) },
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
                onNewEvent = { name -> navController.navigate(NewEvent(group.id, name)) }
            )
        }
    }
    composable<EditGroup> { entry ->
        val route = entry.toRoute<EditGroup>()
        EditGroupRoute(
            groupId = route.groupId,
            groupName = route.groupName,
            onBack = { navController.popBackStack() },
            onSaved = { navController.popBackStack() }
        )
    }
    membersGraph(navController)
    eventsGraph(navController)
}
