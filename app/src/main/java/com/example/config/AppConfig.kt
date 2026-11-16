package com.example.config

import com.example.BuildConfig

object AppConfig {
    const val APP_NAME = "ANIME UNIVERSE"
    const val APP_SUBTITLE = "Track anime. Share taste."
    const val APP_VERSION = BuildConfig.VERSION_NAME
    const val APP_VERSION_CODE = BuildConfig.VERSION_CODE
    const val MAX_COMMUNITY_MEMBERS = 50
    const val ANILIST_GRAPHQL_URL = "https://graphql.anilist.co"

    @Volatile
    var runtimeSupabaseUrl: String = ""

    @Volatile
    var runtimeSupabaseAnonKey: String = ""

    private val builtInUrl: String by lazy {
        VaultMaterial.decodeEndpoint().trim().trimEnd('/')
    }

    private val builtInCredential: String by lazy {
        VaultMaterial.decodeCredential().trim()
    }

    val hasBuiltInVaultConfig: Boolean
        get() = isValidUrl(builtInUrl) && isValidKey(builtInCredential)

    fun updateRuntimeSupabaseConfig(url: String, anonKey: String) {
        runtimeSupabaseUrl = url.trim().trimEnd('/')
        runtimeSupabaseAnonKey = anonKey.trim()
    }

    internal fun isValidUrl(url: String): Boolean {
        if (url.isBlank()) return false
        if (url.contains("your-project.supabase.co", ignoreCase = true)) return false
        return url.startsWith("https://")
    }

    internal fun isValidKey(key: String): Boolean {
        if (key.isBlank()) return false
        if (key.contains("your-supabase", ignoreCase = true)) return false
        return key.length > 20
    }

    internal val supabaseUrl: String
        get() = if (isValidUrl(builtInUrl)) builtInUrl else runtimeSupabaseUrl.trim().trimEnd('/')

    internal val supabaseAnonKey: String
        get() = if (isValidKey(builtInCredential)) builtInCredential else runtimeSupabaseAnonKey.trim()

    val isSupabaseConfigured: Boolean
        get() = isValidUrl(supabaseUrl) && isValidKey(supabaseAnonKey)
}
