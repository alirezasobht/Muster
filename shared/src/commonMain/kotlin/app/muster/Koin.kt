package app.muster

import app.muster.data.di.dataModule
import app.muster.data.di.platformDataModule
import app.muster.domain.di.domainModule
import app.muster.ui.di.uiModule
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.mp.KoinPlatform

// Each platform has its own initKoin. Android's needs a Context, so Koin can't start there without one.
internal fun startMusterKoin(appDeclaration: KoinAppDeclaration) {
    // iOS may call MainViewController() more than once; Koin throws if started twice.
    if (KoinPlatform.getKoinOrNull() != null) return
    startKoin {
        appDeclaration()
        modules(platformDataModule, dataModule, domainModule, uiModule)
    }
}
