package app.muster.data.supabase

import app.muster.SupabaseConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

// The publishable key is public by design; RLS is the trust boundary.
// The secret key must never reach the client.
fun createClient(): SupabaseClient = createSupabaseClient(
    supabaseUrl = SupabaseConfig.URL,
    supabaseKey = SupabaseConfig.PUBLISHABLE_KEY,
) {
    install(Auth)
    install(Postgrest)
}
