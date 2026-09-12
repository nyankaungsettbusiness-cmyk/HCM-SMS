package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.MemoryCodeVerifierCache
import io.github.jan.supabase.gotrue.MemorySessionManager
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.storage.Storage
import kotlinx.serialization.json.Json

/**
 * Thread-safe Supabase Client Manager for HCM-SMS.
 * Handles initialization and configuration of the Supabase client instance.
 */
object SupabaseClientManager {

    @Volatile
    private var clientInstance: SupabaseClient? = null

    var supabaseUrl: String = BuildConfig.SUPABASE_URL.ifBlank { "https://gfwdxcmqdgmubotiuxzl.supabase.co" }
    var supabaseKey: String = BuildConfig.SUPABASE_ANON_KEY.ifBlank { "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imdmd2R4Y21xZGdtdWJvdGl1eHpsIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODg4NzY0MjUsImV4cCI6MjEwNDQ1MjQyNX0.l4kxnVAPmeBF1OS6_nCj36bPRZdHKPDuhZiY3yAJU7o" }

    /**
     * Checks whether valid Supabase credentials (URL and Anon Key) are provided.
     */
    fun isConfigured(): Boolean {
        return supabaseUrl.isNotBlank() && supabaseKey.isNotBlank()
    }

    /**
     * Returns the singleton SupabaseClient instance if configured, or null if unconfigured.
     */
    fun getInstance(): SupabaseClient? {
        if (!isConfigured()) return null
        if (clientInstance == null) {
            synchronized(this) {
                if (clientInstance == null) {
                    try {
                        clientInstance = createSupabaseClient(
                            supabaseUrl = supabaseUrl,
                            supabaseKey = supabaseKey
                        ) {
                            defaultSerializer = KotlinXSerializer(Json {
                                encodeDefaults = true
                                explicitNulls = false
                                ignoreUnknownKeys = true
                                isLenient = true
                                coerceInputValues = true
                            })
                            install(Postgrest)
                            install(Auth) {
                                sessionManager = MemorySessionManager()
                                codeVerifierCache = MemoryCodeVerifierCache()
                            }
                            install(Storage)
                            install(Realtime)
                        }
                    } catch (e: Throwable) {
                        Log.e("SupabaseClientManager", "Failed to create Supabase client: ${e.message}", e)
                        e.printStackTrace()
                    }
                }
            }
        }
        return clientInstance
    }

    /**
     * Dynamically update Supabase credentials at runtime (e.g. from School Settings or environment).
     */
    fun configure(url: String, key: String) {
        synchronized(this) {
            supabaseUrl = url.trim()
            supabaseKey = key.trim()
            clientInstance = null
        }
    }
}
