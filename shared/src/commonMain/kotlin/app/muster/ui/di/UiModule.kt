package app.muster.ui.di

import app.muster.ui.screens.auth.launch.LaunchViewModel
import app.muster.ui.screens.auth.setname.SetNameViewModel
import app.muster.ui.screens.auth.signin.EnterCodeViewModel
import app.muster.ui.screens.auth.signin.RequestCodeViewModel
import app.muster.ui.screens.event.EventSummary
import app.muster.ui.screens.event.EventViewModel
import app.muster.ui.screens.event.addplayers.AddPlayersViewModel
import app.muster.ui.screens.event.editevent.EditEventInitial
import app.muster.ui.screens.event.editevent.EditEventViewModel
import app.muster.ui.screens.group.GroupViewModel
import app.muster.ui.screens.group.editgroup.EditGroupViewModel
import app.muster.ui.screens.group.events.EventsViewModel
import app.muster.ui.screens.group.events.newevent.NewEventViewModel
import app.muster.ui.screens.group.members.MembersViewModel
import app.muster.ui.screens.group.members.addbyemail.AddMemberByEmailViewModel
import app.muster.ui.screens.group.members.TagNameViewModel
import app.muster.ui.screens.home.HomeViewModel
import app.muster.ui.screens.home.newgroup.NewGroupViewModel
import app.muster.ui.screens.home.settings.SettingsViewModel
import app.muster.ui.screens.home.settings.deleteaccount.DeleteAccountViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val uiModule = module {
    viewModelOf(::LaunchViewModel)
    viewModelOf(::RequestCodeViewModel)
    viewModelOf(::SetNameViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::DeleteAccountViewModel)
    viewModelOf(::NewGroupViewModel)
    // Email comes from the EnterCode route, not the graph.
    viewModel { (email: String) -> EnterCodeViewModel(email, get(), get()) }
    // Group id and name come from the Group route, not the graph.
    viewModel { (groupId: String, groupName: String) ->
        GroupViewModel(groupId, groupName, get(), get(), get(), get(), get())
    }
    // Group id and current name come from the Edit group route, not the graph.
    viewModel { (groupId: String, groupName: String) -> EditGroupViewModel(groupId, groupName, get()) }
    // Group id comes from the Members tab's host (GroupRoute), not the graph.
    viewModel { (groupId: String) ->
        MembersViewModel(groupId, get(), get(), get(), get(), get(), get(), get(), get(), get())
    }
    // Group id comes from the Members tab's host (GroupRoute), not the graph.
    viewModel { (groupId: String) -> TagNameViewModel(groupId, get(), get()) }
    // Group id comes from the Events tab's host (GroupRoute), not the graph.
    viewModel { (groupId: String) -> EventsViewModel(groupId, get(), get(), get()) }
    // Group id comes from the AddMemberByEmail route, not the graph.
    viewModel { (groupId: String) -> AddMemberByEmailViewModel(groupId, get()) }
    // Group id comes from the New event route, not the graph.
    viewModel { (groupId: String) -> NewEventViewModel(groupId, get()) }
    // The initial summary comes from the Event route, not the graph.
    viewModel { (summary: EventSummary) -> EventViewModel(summary, get(), get(), get(), get(), get(), get(), get(), get()) }
    // The event's current values come from the Edit event route, not the graph.
    viewModel { (initial: EditEventInitial) -> EditEventViewModel(initial, get()) }
    // Event id and group id come from the Add players route, not the graph.
    viewModel { (eventId: String, groupId: String) -> AddPlayersViewModel(eventId, groupId, get(), get()) }
}
