package app.muster

import android.app.Application

// Not MainActivity: its onCreate re-runs on rotation, and Koin must start once.
class MusterApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin()
    }
}
