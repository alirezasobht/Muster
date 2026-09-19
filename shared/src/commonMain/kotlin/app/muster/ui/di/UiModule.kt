package app.muster.ui.di

import app.muster.ui.screens.addbyemail.AddMemberByEmailViewModel
import app.muster.ui.screens.event.EventSummary
import app.muster.ui.screens.event.EventViewModel
import app.muster.ui.screens.group.GroupViewModel
import app.muster.ui.screens.group.events.EventsViewModel
import app.muster.ui.screens.group.members.MembersViewModel
import app.muster.ui.screens.home.HomeViewModel
import app.muster.ui.screens.launch.LaunchViewModel
import app.muster.ui.screens.newevent.NewEventViewModel
import app.muster.ui.screens.newgroup.NewGroupViewModel
import app.muster.ui.screens.setname.SetNameViewModel
import app.muster.ui.screens.settings.SettingsViewModel
import app.muster.ui.screens.signin.EnterCodeViewModel
import app.muster.ui.screens.signin.RequestCodeViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val uiModule = module {
    viewModelOf(::LaunchViewModel)
    viewModelOf(::RequestCodeViewModel)
    viewModelOf(::SetNameViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::NewGroupViewModel)
    // Email comes from the EnterCode route, not the graph.
    viewModel { (email: String) -> EnterCodeViewModel(email, get(), get()) }
    // Group id and name come from the Group route, not the graph.
    viewModel { (groupId: String, groupName: String) -> GroupViewModel(groupId, groupName, get(), get(), get()) }
    // Group id comes from the Members tab's host (GroupRoute), not the graph.
    viewModel { (groupId: String) ->
        MembersViewModel(groupId, get(), get(), get(), get(), get(), get(), get(), get())
    }
    // Group id comes from the Events tab's host (GroupRoute), not the graph.
    viewModel { (groupId: String) -> EventsViewModel(groupId, get(), get(), get()) }
    // Group id comes from the AddMemberByEmail route, not the graph.
    viewModel { (groupId: String) -> AddMemberByEmailViewModel(groupId, get()) }
    // Group id comes from the New event route, not the graph.
    viewModel { (groupId: String) -> NewEventViewModel(groupId, get()) }
    // The initial summary comes from the Event route, not the graph.
    viewModel { (summary: EventSummary) -> EventViewModel(summary, get(), get(), get(), get(), get(), get()) }
}
