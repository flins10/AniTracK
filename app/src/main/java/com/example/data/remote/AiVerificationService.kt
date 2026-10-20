package com.example.data.remote

object AiVerificationService {
    suspend fun craftFeatureWithAi(prompt: String): String {
        val p = prompt.lowercase()
        return when {
            p.contains("recommend") || p.contains("suggestion") || p.contains("watch") -> """
                  **Recommendations**:
                
                  **Smart Auto-Queue**: Based on your watchlist, try *Demon Slayer: Kimetsu no Yaiba*, *Frieren: Beyond Journey's End*, or *Jujutsu Kaisen*.
                  **Filler Filter Engine**: Flags filler episodes so you can skip non-canon content effortlessly.
                  **Simulcast Airing Predictor**: Predicts subtitle translation release times down to the minute.
            """.trimIndent()
            p.contains("theme") || p.contains("color") || p.contains("design") -> """
                  **Theme Suggestions**:
                
                  **Obsidian Dark & Electric Light Blue**: High contrast M3 layout with dynamic notification dots.
                  **Ruby Crimson Accent**: Vibrant red highlights for active airing status and new episode alerts.
                  **Emerald Green Watched Pill**: Soft pastel green indicator when caught up with all aired episodes.
            """.trimIndent()
            else -> """
                  **Feature Recommendations**:
                
                  **Live MAL Sync**: Automatically updates all episode watch counts when logging into your account.
                  **Episode Stepper (+ / -)**: Instant single-tap watched counter updates right on the main schedule card.
                  **Live Countdown Resets**: Schedule countdown dynamically advances to future weeks so release timers never stall.
            """.trimIndent()
        }
    }

    suspend fun fetchEpisodeInfoFromAi(title: String): AiEpisodeInfo? = null
}

data class AiEpisodeInfo(
    val latestAiredEpisode: Int?,
    val totalEpisodes: Int?,
    val nextEpisode: Int?,
    val status: String?,
    val nextAiringTimeUnix: Long?
)
