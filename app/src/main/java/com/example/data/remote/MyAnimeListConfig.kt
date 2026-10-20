package com.example.data.remote

/**
 * MyAnimeList API & OAuth Configuration.
 * Note: Personal credentials and Client IDs are user-provided and stored encrypted via Android Keystore.
 */
object MyAnimeListConfig {
    const val AUTH_URL = "https://myanimelist.net/v1/oauth2/authorize"
    const val TOKEN_URL = "https://myanimelist.net/v1/oauth2/token"
    const val API_BASE_URL = "https://api.myanimelist.net/v2/"
    const val DEFAULT_REDIRECT_URI = "anitrack://mal-callback"
    const val SECONDARY_REDIRECT_URI = "anitrack://oauth"
}
