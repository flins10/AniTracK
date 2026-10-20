package com.example.data.repository

data class AnimeSeasonInfo(
    val seasonNumber: Int,
    val title: String,
    val animeId: Int,
    val idMal: Int? = null,
    val totalEpisodes: Int? = null,
    val coverImage: String? = null,
    val bannerImage: String? = null,
    val format: String? = "TV",
    val isCurrentSeason: Boolean = false
)
