package app.muster.ui.screens.group.events

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import app.muster.ui.common.util.toDisplayDate
import app.muster.ui.screens.event.Event
import app.muster.ui.screens.group.events.newevent.NewEventRoute
import kotlinx.serialization.Serializable

@Serializable
data class NewEvent(
    val groupId: String,
    val groupName: String
)

fun NavGraphBuilder.eventsGraph(navController: NavController) {
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
}
