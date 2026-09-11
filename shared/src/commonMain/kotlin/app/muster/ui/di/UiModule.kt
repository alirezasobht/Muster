package app.muster.ui.di

import app.muster.ui.screens.launch.LaunchViewModel
import app.muster.ui.screens.setname.SetNameViewModel
import app.muster.ui.screens.signin.EnterCodeViewModel
import app.muster.ui.screens.signin.RequestCodeViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val uiModule = module {
    viewModelOf(::LaunchViewModel)
    viewModelOf(::RequestCodeViewModel)
    viewModelOf(::SetNameViewModel)
    // Email comes from the EnterCode route, not the graph.
    viewModel { (email: String) -> EnterCodeViewModel(email, get(), get()) }
}
