package app.muster.data.di

import com.russhwolf.settings.Settings
import com.russhwolf.settings.StorageSettings
import org.koin.core.module.Module
import org.koin.dsl.module

// Web has only one localStorage, shared with supabase-kt's session. So there is no separate "muster" store; every key we save starts with "muster." instead.
actual val platformDataModule: Module = module {
    single<Settings> { StorageSettings() }
}
