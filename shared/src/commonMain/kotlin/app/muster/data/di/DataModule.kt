package app.muster.data.di

import app.muster.data.repository.AuthRepositoryImpl
import app.muster.data.repository.GroupRepositoryImpl
import app.muster.data.repository.ProfileRepositoryImpl
import app.muster.data.supabase.createClient
import app.muster.domain.repository.AuthRepository
import app.muster.domain.repository.GroupRepository
import app.muster.domain.repository.ProfileRepository
import io.github.jan.supabase.SupabaseClient
import org.koin.dsl.module

val dataModule = module {
    single<SupabaseClient> { createClient() }
    single<AuthRepository> { AuthRepositoryImpl(get()) }
    single<ProfileRepository> { ProfileRepositoryImpl(get()) }
    single<GroupRepository> { GroupRepositoryImpl(get()) }
}
