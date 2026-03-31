
package com.xakep1c.familyshop

import com.xakep1c.familyshop.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.functions.Functions
import kotlinx.serialization.json.Json

val supabase = createSupabaseClient(
    supabaseUrl = BuildConfig.SUPABASE_URL,
    supabaseKey = BuildConfig.SUPABASE_KEY
) {
    install(Postgrest) {
        serializer = io.github.jan.supabase.serializer.KotlinXSerializer(
            Json { ignoreUnknownKeys = true }
        )
    }
    install(Auth)
    install(Realtime)
    install(Functions)
}