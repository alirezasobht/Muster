package app.muster.data.di

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSUserDefaults

actual val platformDataModule: Module = module {
    single<Settings> { NSUserDefaultsSettings(NSUserDefaults(suiteName = "muster")) }
}
