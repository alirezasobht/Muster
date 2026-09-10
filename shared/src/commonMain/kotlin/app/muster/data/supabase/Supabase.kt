package app.muster.data.supabase

import app.muster.SupabaseConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

/**
 * The single Supabase client for the app.
 *
 * The publishable key is public by design — RLS is the trust boundary, not this
 * value. It is injected rather than hardcoded so it can be rotated and stays out
 * of git. The secret key never appears in the client.
 */
val supabase: SupabaseClient = createSupabaseClient(
    supabaseUrl = SupabaseConfig.URL,
    supabaseKey = SupabaseConfig.PUBLISHABLE_KEY,
) {
    install(Auth)
    install(Postgrest)
}
