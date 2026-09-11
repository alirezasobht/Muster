package app.muster.data.di

import app.muster.data.supabase.createClient
import io.github.jan.supabase.SupabaseClient
import org.koin.dsl.module

val dataModule = module {
    single<SupabaseClient> { createClient() }
}
