package app.muster.ui.screens.event

import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import app.muster.ui.common.util.LocalAnimatedVisibilityScope
import app.muster.ui.screens.event.addplayers.AddPlayersRoute
import app.muster.ui.screens.event.editevent.EditEventInitial
import app.muster.ui.screens.event.editevent.EditEventRoute
import kotlin.time.Instant
import kotlinx.serialization.Serializable

// Carries everything the caller already knows from the Events tab card
@Serializable
data class Event(
    val groupId: String,
    val groupName: String,
    val eventId: String,
    val title: String,
    val date: String,
    val location: String,
    val capacity: Int,
    val inCount: Int,
    val pendingCount: Int
)

// startsAt is an ISO-8601 instant
@Serializable
data class EditEvent(
    val eventId: String,
    val groupId: String,
    val title: String,
    val startsAt: String,
    val location: String
)

@Serializable
data class AddPlayers(
    val eventId: String,
    val groupId: String
)

fun NavGraphBuilder.eventGraph(navController: NavController) {
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
                },
                onEditEvent = { summary, startsAt ->
                    navController.navigate(
                        EditEvent(
                            eventId = summary.eventId,
                            groupId = summary.groupId,
                            title = summary.title,
                            startsAt = startsAt.toString(),
                            location = summary.location
                        )
                    )
                }
            )
        }
    }
    composable<EditEvent> { entry ->
        val route = entry.toRoute<EditEvent>()
        EditEventRoute(
            initial = EditEventInitial(
                eventId = route.eventId,
                groupId = route.groupId,
                title = route.title,
                startsAt = Instant.parse(route.startsAt),
                location = route.location
            ),
            onBack = { navController.popBackStack() },
            onSaved = { navController.popBackStack() }
        )
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
