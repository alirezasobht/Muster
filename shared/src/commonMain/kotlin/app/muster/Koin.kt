package app.muster

import app.muster.data.di.dataModule
import app.muster.domain.di.domainModule
import app.muster.ui.di.uiModule
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatform

fun initKoin() {
    // iOS may call MainViewController() more than once; Koin throws if started twice.
    if (KoinPlatform.getKoinOrNull() != null) return
    startKoin {
        modules(dataModule, domainModule, uiModule)
    }
}
