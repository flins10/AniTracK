package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_anime")
data class SavedAnimeEntity(
    @PrimaryKey val id: Int,
    val titleRomaji: String,
    val titleEnglish: String? = null,
    val titleNative: String? = null,
    val titlePinyin: String? = null,
    val metadataSource: String = "anilist",
    val coverImage: String,
    val bannerImage: String? = null,
    val format: String? = "TV",
    val status: String? = "RELEASING",
    val totalEpisodes: Int? = null,
    val currentEpisode: Int = 1,
    val watchedEpisodeCount: Int = 0,
    @ColumnInfo(defaultValue = "1") val userSeason: Int = 1,
    val userStatus: String? = null,
    val userScore: Int? = null,
    val startDate: String? = null,
    val finishDate: String? = null,
    val airingDayOfWeek: Int = 1,
    val airingTimeUnix: Long? = null,
    val nextEpisodeNumber: Int? = null,
    val genres: String = "",
    @ColumnInfo(defaultValue = "") val studios: String = "",
    val season: String? = null,
    val seasonYear: Int? = null,
    val score: Int? = null,
    val popularity: Int? = null,
    val synopsis: String? = null,
    val countryOfOrigin: String? = "JP",
    val inWatchlist: Boolean = false,
    val isWishlist: Boolean = false,
    val isFavorite: Boolean = false,
    @ColumnInfo(defaultValue = "0") val isPinned: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val isWatched: Boolean = false,
    val lastWatchedTime: Long? = null,
    val userNotes: String? = null,
    val savedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val hasActiveUserState: Boolean
        get() = inWatchlist || isWishlist || isFavorite || isPinned ||
            watchedEpisodeCount > 0 || !userStatus.isNullOrBlank() ||
            !userNotes.isNullOrBlank()
}

fun AnimeEntity.toSavedAnimeEntity(existingSavedAt: Long? = null): SavedAnimeEntity {
    val now = System.currentTimeMillis()
    return SavedAnimeEntity(
        id = id,
        titleRomaji = titleRomaji,
        titleEnglish = titleEnglish,
        titleNative = titleNative,
        titlePinyin = titlePinyin,
        metadataSource = metadataSource,
        coverImage = coverImage,
        bannerImage = bannerImage,
        format = format,
        status = status,
        totalEpisodes = totalEpisodes,
        currentEpisode = currentEpisode,
        watchedEpisodeCount = watchedEpisodeCount,
        userSeason = userSeason,
        userStatus = userStatus,
        userScore = userScore,
        startDate = startDate,
        finishDate = finishDate,
        airingDayOfWeek = airingDayOfWeek,
        airingTimeUnix = airingTimeUnix,
        nextEpisodeNumber = nextEpisodeNumber,
        genres = genres,
        studios = studios,
        season = season,
        seasonYear = seasonYear,
        score = score,
        popularity = popularity,
        synopsis = synopsis,
        countryOfOrigin = countryOfOrigin,
        inWatchlist = inWatchlist,
        isWishlist = isWishlist,
        isFavorite = isFavorite,
        isPinned = isPinned,
        notificationsEnabled = notificationsEnabled,
        isWatched = isWatched,
        lastWatchedTime = lastWatchedTime,
        userNotes = userNotes,
        savedAt = existingSavedAt ?: now,
        updatedAt = now
    )
}

fun SavedAnimeEntity.toAnimeEntity(): AnimeEntity {
    return AnimeEntity(
        id = id,
        titleRomaji = titleRomaji,
        titleEnglish = titleEnglish,
        titleNative = titleNative,
        titlePinyin = titlePinyin,
        metadataSource = metadataSource,
        coverImage = coverImage,
        bannerImage = bannerImage,
        format = format,
        status = status,
        totalEpisodes = totalEpisodes,
        currentEpisode = currentEpisode,
        watchedEpisodeCount = watchedEpisodeCount,
        userSeason = userSeason,
        userStatus = userStatus,
        userScore = userScore,
        startDate = startDate,
        finishDate = finishDate,
        airingDayOfWeek = airingDayOfWeek,
        airingTimeUnix = airingTimeUnix,
        nextEpisodeNumber = nextEpisodeNumber,
        genres = genres,
        studios = studios,
        season = season,
        seasonYear = seasonYear,
        score = score,
        popularity = popularity,
        synopsis = synopsis,
        countryOfOrigin = countryOfOrigin,
        inWatchlist = inWatchlist,
        isWishlist = isWishlist,
        isFavorite = isFavorite,
        isPinned = isPinned,
        notificationsEnabled = notificationsEnabled,
        isWatched = isWatched,
        lastWatchedTime = lastWatchedTime,
        userNotes = userNotes,
        lastCachedAt = updatedAt
    )
}
