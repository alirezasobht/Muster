package app.muster.data.di

import org.koin.core.module.Module

// Gives the app its own Settings store, named "muster". supabase-kt saves its session in the default store, so we keep ours separate.
expect val platformDataModule: Module
