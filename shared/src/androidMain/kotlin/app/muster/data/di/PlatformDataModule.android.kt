package app.muster.data.di

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformDataModule: Module = module {
    single<Settings> {
        SharedPreferencesSettings(androidContext().getSharedPreferences("muster", Context.MODE_PRIVATE))
    }
}
