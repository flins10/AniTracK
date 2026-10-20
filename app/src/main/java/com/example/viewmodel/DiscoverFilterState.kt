package com.example.viewmodel

data class DiscoverFilterState(
    val mediaType: String = "ALL", // "ALL", "ANIME", "DONGHUA"
    val status: String = "ALL", // "ALL", "RELEASING", "UPCOMING", "FINISHED"
    val season: String? = null, // null = All, "WINTER", "SPRING", "SUMMER", "FALL"
    val yearFrom: Int? = null,
    val yearTo: Int? = null,
    val selectedGenres: Set<String> = emptySet(),
    val genreMatchAll: Boolean = false,
    val format: String = "ALL", // "ALL", "TV", "MOVIE", "OVA", "ONA"
    val country: String = "ALL", // "ALL", "JP", "CN"
    val malAvailability: String = "ALL", // "ALL", "MAL_ONLY", "LOCAL_ONLY"
    val sortBy: String = "POPULARITY_DESC" // "POPULARITY_DESC", "SCORE_DESC", "TRENDING_DESC", "START_DATE_DESC", "TITLE_ROMAJI"
) {
    val activeFilterCount: Int
        get() {
            var count = 0
            if (mediaType != "ALL") count++
            if (status != "ALL") count++
            if (season != null) count++
            if (yearFrom != null || yearTo != null) count++
            if (selectedGenres.isNotEmpty()) count++
            if (format != "ALL") count++
            if (country != "ALL") count++
            if (malAvailability != "ALL") count++
            return count
        }
}
