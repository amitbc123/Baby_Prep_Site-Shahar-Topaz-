package com.oryareach.core.network

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

/**
 * Builds the Supabase client from build configuration.
 *
 * The anon key is a public identifier, not a secret: it grants nothing on its own, because
 * every table is protected by RLS and `anon` holds no table privileges at all. Authorization
 * comes from the signed-in user's JWT.
 */
object SupabaseClientProvider {

    fun create(
        url: String = BuildConfig.SUPABASE_URL,
        anonKey: String = BuildConfig.SUPABASE_ANON_KEY,
    ): SupabaseClient {
        require(url.isNotBlank() && anonKey.isNotBlank()) {
            "Supabase is not configured. Set supabaseUrl and supabaseAnonKey in " +
                "android/local.properties or as Gradle properties."
        }

        return createSupabaseClient(supabaseUrl = url, supabaseKey = anonKey) {
            install(Auth)
            install(Postgrest)
        }
    }
}
