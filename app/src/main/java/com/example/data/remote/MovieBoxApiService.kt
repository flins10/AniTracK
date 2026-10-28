package com.example.data.remote

import com.example.BuildConfig
import com.example.data.repository.ArchivePlayableMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * Movie/Series object defined in the MovieBox API documentation:
 * https://github.com/shamsullah01/moviebox/blob/refs/heads/master/docs/API.md
 */
data class MovieBoxItem(
    val id: Int,
    val title: String,
    val year: String,
    val rating: String,
    val genre: String,
    val genreArray: List<String>,
    val poster: String,
    val backdrop: String,
    val type: String, // "Movie" | "Series"
    val overview: String,
    val language: String = "EN",
    val runtime: Int? = null,
    val cast: List<MovieBoxCastMember> = emptyList(),
    val director: String = "Unknown",
    val totalSeasons: Int = 1,
    val totalEpisodes: Int = 1,
    val archiveIdentifier: String? = null
) {
    val isSeries: Boolean
        get() = type.equals("Series", ignoreCase = true) || type.equals("tv", ignoreCase = true)

    /**
     * Returns the clean display title without bracketed tags like `[Hindi]` or `[CAM]`.
     */
    val cleanTitle: String
        get() = title.replace(Regex("\\[.*?]"), "").trim()
}

data class MovieBoxCastMember(
    val name: String,
    val character: String
)

data class MovieBoxSubtitleTrack(
    val language: String,
    val label: String,
    val url: String,
    val format: String = "srt"
)

data class MovieBoxQualityStream(
    val quality: String,
    val streamUrl: String,
    val format: String = "MP4",
    val sizeBytes: Long? = null
)

data class MovieBoxAuthInfo(
    val authKey: String,
    val videoHash: String,
    val timestamp: Long,
    val streamingUrl: String? = null
)

/**
 * Streaming Data Object defined in `docs/API.md`:
 * {
 *   streamUrl: string,
 *   quality: string,
 *   subtitles: array,
 *   downloadUrl: string,
 *   source: string,
 *   isDirectLink: boolean,
 *   authInfo: { authKey, videoHash, timestamp },
 *   movieTitle: string,
 *   message: string
 * }
 */
data class MovieBoxStreamingData(
    val streamUrl: String,
    val quality: String,
    val subtitles: List<MovieBoxSubtitleTrack> = emptyList(),
    val downloadUrl: String? = null,
    val source: String,
    val isDirectLink: Boolean,
    val authInfo: MovieBoxAuthInfo,
    val movieTitle: String,
    val message: String,
    val isVerifiedPlayableMedia: Boolean = false,
    val playableMedia: ArchivePlayableMedia? = null,
    val qualityStreams: List<MovieBoxQualityStream> = emptyList(),
    val searchUrl: String? = null,
    val homepageUrl: String? = null,
    val utmSource: String = "moviebox-clone"
)

/**
 * Implementation of the documented `MovieBoxAPI` service from:
 * https://github.com/shamsullah01/moviebox/blob/refs/heads/master/docs/API.md
 *
 * Supports:
 * 1. `getTrending(page)`
 * 2. `getLatestMovies(page)`
 * 3. `getTVSeries(page)`
 * 4. `search(query, page)`
 * 5. `getMovieDetails(id)`
 * 6. `getStreamingUrls(id, type)`
 *
 * Includes 3-attempt retry with backoff (`1s, 2s, 3s`), JSON and HTML/JSON-LD extraction,
 * TMDB & OMDb secondary/tertiary fallbacks, and authKey / videoHash generation.
 */
class MovieBoxApiService(
    val baseURL: String = DEFAULT_API_BASE_URL,
    val webURL: String = DEFAULT_WEB_BASE_URL,
    val tmdbURL: String = DEFAULT_TMDB_BASE_URL,
    val omdbURL: String = DEFAULT_OMDB_BASE_URL,
    private val tmdbApiKey: String = readOptionalBuildConfigSecret("TMDB_API_KEY"),
    private val omdbApiKey: String = readOptionalBuildConfigSecret("OMDB_API_KEY"),
    private val okHttpClient: OkHttpClient = defaultHttpClient()
) {

    companion object {
        const val DEFAULT_API_BASE_URL = "https://moviebox.ng/api"
        const val DEFAULT_WEB_BASE_URL = "https://moviebox.ng"
        const val DEFAULT_TMDB_BASE_URL = "https://api.themoviedb.org/3"
        const val DEFAULT_OMDB_BASE_URL = "https://www.omdbapi.com/"

        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

        fun readOptionalBuildConfigSecret(fieldName: String): String = try {
            val field = BuildConfig::class.java.getField(fieldName)
            (field.get(null) as? String)?.trim().orEmpty()
        } catch (_: Throwable) {
            ""
        }

        fun defaultHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

        /**
         * Generates MovieBox.ng authentication key as documented in `docs/API.md`:
         * Pattern: `timestamp-0-0-hash` where hash is 32-char lowercase hex.
         */
        fun generateAuthKey(nowEpochSeconds: Long = System.currentTimeMillis() / 1000L): String {
            val timestamp = nowEpochSeconds + Random.nextLong(0L, 86_400L)
            val hash = buildString(32) {
                repeat(32) {
                    append(Random.nextInt(16).toString(16))
                }
            }
            return "$timestamp-0-0-$hash"
        }

        /**
         * Generates 32-character hex video hash as documented in `docs/API.md`.
         */
        fun generateVideoHash(): String = buildString(32) {
            repeat(32) {
                append(Random.nextInt(16).toString(16))
            }
        }
    }

    /**
     * Documented `fetchWithRetry(url, retries = 3)` with linear/exponential backoff (1s, 2s, 3s).
     */
    suspend fun fetchWithRetry(
        url: String,
        headers: Map<String, String> = emptyMap(),
        retries: Int = 3,
        baseBackoffMs: Long = 350L
    ): String = withContext(Dispatchers.IO) {
        var lastError: Throwable? = null
        val maxAttempts = retries.coerceAtLeast(1)
        for (i in 0 until maxAttempts) {
            try {
                val requestBuilder = Request.Builder()
                    .url(url)
                    .get()
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "application/json, text/html;q=0.9, */*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Origin", webURL)
                    .header("Referer", "$webURL/")
                headers.forEach { (k, v) -> requestBuilder.header(k, v) }

                okHttpClient.newCall(requestBuilder.build()).execute().use { response ->
                    if (response.code == 429) {
                        throw IllegalStateException("Rate limited (HTTP 429) from $url")
                    }
                    if (!response.isSuccessful) {
                        throw IllegalStateException("HTTP error! status: ${response.code}")
                    }
                    val body = response.body?.string().orEmpty()
                    if (body.isBlank()) {
                        throw IllegalStateException("Empty response body from $url")
                    }
                    return@withContext body
                }
            } catch (e: Throwable) {
                lastError = e
                if (i < maxAttempts - 1) {
                    delay(baseBackoffMs * (i + 1))
                }
            }
        }
        throw lastError ?: IllegalStateException("Failed to fetch $url after $maxAttempts attempts")
    }

    /**
     * Parses either JSON (`results` / `data` / root array) or HTML (`.movie-card` and `application/ld+json`)
     * into a list of [MovieBoxItem] objects. Exclude animation/anime items when used in live-action Watch catalog.
     */
    fun parseMovieListPayload(payload: String, defaultType: String = "Movie"): List<MovieBoxItem> {
        val trimmed = payload.trim()
        if (trimmed.isEmpty()) return emptyList()

        return try {
            when {
                trimmed.startsWith("{") -> {
                    val root = JSONObject(trimmed)
                    val arr = root.optJSONArray("results")
                        ?: root.optJSONArray("data")
                        ?: root.optJSONArray("Search")
                        ?: root.optJSONArray("items")
                    if (arr != null) {
                        formatMovieJsonArray(arr, defaultType)
                    } else {
                        emptyList()
                    }
                }
                trimmed.startsWith("[") -> {
                    val arr = JSONArray(trimmed)
                    formatMovieJsonArray(arr, defaultType)
                }
                else -> parseHtmlResponse(trimmed, defaultType)
            }
        } catch (_: Exception) {
            parseHtmlResponse(trimmed, defaultType)
        }
    }

    /**
     * Formats JSON array from MovieBox.ng, TMDB, or OMDb into [MovieBoxItem] items.
     */
    fun formatMovieJsonArray(arr: JSONArray, defaultType: String = "Movie"): List<MovieBoxItem> {
        val list = mutableListOf<MovieBoxItem>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val item = parseSingleMovieJson(obj, fallbackId = i + 1, defaultType = defaultType)
            if (item != null && !isAnimationContent(item)) {
                list.add(item)
            }
        }
        return list
    }

    fun parseSingleMovieJson(
        obj: JSONObject,
        fallbackId: Int = 1,
        defaultType: String = "Movie"
    ): MovieBoxItem? {
        val rawTitle = obj.optString("title")
            .ifBlank { obj.optString("name") }
            .ifBlank { obj.optString("Title") }
            .trim()
        if (rawTitle.isBlank()) return null

        val cleanTitle = rawTitle.replace(Regex("\\[.*?]"), "").trim()
        val id = obj.optInt("id", 0).takeIf { it > 0 }
            ?: obj.optString("imdbID").removePrefix("tt").toIntOrNull()
            ?: fallbackId

        val yearRaw = obj.optString("year")
            .ifBlank { obj.optString("release_date").take(4) }
            .ifBlank { obj.optString("first_air_date").take(4) }
            .ifBlank { obj.optString("Year").take(4) }
            .ifBlank { "2024" }

        val ratingRaw = when {
            obj.has("rating") && !obj.optString("rating").isNullOrBlank() -> obj.optString("rating")
            obj.has("vote_average") -> String.format(Locale.US, "%.1f", obj.optDouble("vote_average", 7.8))
            obj.has("imdbRating") && obj.optString("imdbRating") != "N/A" -> obj.optString("imdbRating")
            else -> "7.8"
        }

        val genreArray = mutableListOf<String>()
        val jsonGenres = obj.optJSONArray("genreArray") ?: obj.optJSONArray("genres")
        if (jsonGenres != null) {
            for (g in 0 until jsonGenres.length()) {
                val gVal = jsonGenres.optString(g).ifBlank {
                    jsonGenres.optJSONObject(g)?.optString("name").orEmpty()
                }.trim()
                if (gVal.isNotBlank()) genreArray.add(gVal)
            }
        }
        val genreString = obj.optString("genre")
            .ifBlank { obj.optString("Genre") }
            .ifBlank { if (genreArray.isNotEmpty()) genreArray.joinToString(", ") else "Action, Drama" }

        if (genreArray.isEmpty()) {
            genreString.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { genreArray.add(it) }
        }

        val posterRaw = obj.optString("poster")
            .ifBlank { obj.optString("Poster").takeIf { it != "N/A" }.orEmpty() }
            .ifBlank {
                val path = obj.optString("poster_path")
                if (path.isNotBlank()) "https://image.tmdb.org/t/p/w500$path" else ""
            }

        val backdropRaw = obj.optString("backdrop")
            .ifBlank {
                val path = obj.optString("backdrop_path")
                if (path.isNotBlank()) "https://image.tmdb.org/t/p/w1280$path" else posterRaw
            }

        val mediaTypeRaw = obj.optString("type")
            .ifBlank { obj.optString("media_type") }
            .ifBlank { obj.optString("Type") }
            .ifBlank { defaultType }
        val resolvedType = if (
            mediaTypeRaw.equals("Series", ignoreCase = true) ||
            mediaTypeRaw.equals("tv", ignoreCase = true)
        ) "Series" else "Movie"

        val overview = obj.optString("overview")
            .ifBlank { obj.optString("Plot").takeIf { it != "N/A" }.orEmpty() }
            .ifBlank { obj.optString("description") }
            .ifBlank { "$cleanTitle ($yearRaw) live-action ${resolvedType.lowercase(Locale.US)}." }

        val language = obj.optString("language")
            .ifBlank { obj.optString("original_language").uppercase(Locale.US) }
            .ifBlank { "EN" }

        val runtime = obj.optInt("runtime", 0).takeIf { it > 0 }
            ?: obj.optString("Runtime").substringBefore(" ").toIntOrNull()

        val director = obj.optString("director")
            .ifBlank { obj.optString("Director").takeIf { it != "N/A" }.orEmpty() }
            .ifBlank { "Unknown" }

        val castList = mutableListOf<MovieBoxCastMember>()
        val castArr = obj.optJSONArray("cast")
        if (castArr != null) {
            for (c in 0 until castArr.length()) {
                val cObj = castArr.optJSONObject(c)
                if (cObj != null) {
                    val name = cObj.optString("name").trim()
                    val character = cObj.optString("character").trim()
                    if (name.isNotBlank()) castList.add(MovieBoxCastMember(name, character))
                }
            }
        } else {
            val actorsStr = obj.optString("Actors").takeIf { it != "N/A" }.orEmpty()
            actorsStr.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { actor ->
                castList.add(MovieBoxCastMember(name = actor, character = ""))
            }
        }

        return MovieBoxItem(
            id = id,
            title = cleanTitle,
            year = yearRaw,
            rating = ratingRaw,
            genre = genreString,
            genreArray = genreArray,
            poster = posterRaw,
            backdrop = backdropRaw,
            type = resolvedType,
            overview = overview,
            language = language,
            runtime = runtime,
            cast = castList,
            director = director,
            totalSeasons = obj.optInt("totalSeasons", if (resolvedType == "Series") 2 else 1).coerceAtLeast(1),
            totalEpisodes = obj.optInt("totalEpisodes", if (resolvedType == "Series") 16 else 1).coerceAtLeast(1),
            archiveIdentifier = obj.optString("archiveIdentifier").takeIf { it.isNotBlank() }
        )
    }

    /**
     * Parses MovieBox.ng HTML responses using both `.movie-card` regexes and `<script type="application/ld+json">`
     * structured data as documented in `src/services/api.js`.
     */
    fun parseHtmlResponse(html: String, defaultType: String = "Movie"): List<MovieBoxItem> {
        if (html.isBlank()) return emptyList()
        val movies = mutableListOf<MovieBoxItem>()

        try {
            val movieCardRegex = Regex("""<div[^>]*class="[^"]*movie-card[^"]*"[^>]*>[\s\S]*?</div>""", RegexOption.IGNORE_CASE)
            val titleRegex = Regex("""<h[0-9][^>]*>([^<]+)</h[0-9]>""", RegexOption.IGNORE_CASE)
            val imageRegex = Regex("""<img[^>]*src="([^"]+)"[^>]*>""", RegexOption.IGNORE_CASE)
            val yearRegex = Regex("""\b(19\d{2}|20\d{2})\b""")
            val genreRegex = Regex("""(Action|Drama|Comedy|Horror|Thriller|Romance|Adventure|Sci-Fi|Fantasy|Crime|Mystery|Musical|Biography)""", RegexOption.IGNORE_CASE)

            movieCardRegex.findAll(html).forEachIndexed { idx, matchResult ->
                val cardHtml = matchResult.value
                val rawTitle = titleRegex.find(cardHtml)?.groupValues?.getOrNull(1)?.trim().orEmpty()
                if (rawTitle.isNotBlank()) {
                    val cleanTitle = rawTitle.replace(Regex("\\[.*?]"), "").trim()
                    val img = imageRegex.find(cardHtml)?.groupValues?.getOrNull(1)?.trim().orEmpty()
                    val yr = yearRegex.find(cardHtml)?.groupValues?.getOrNull(1) ?: "2024"
                    val genresFound = genreRegex.findAll(cardHtml)
                        .map { it.value.replaceFirstChar { c -> c.uppercase() } }
                        .distinct()
                        .toList()
                        .ifEmpty { listOf("Action", "Drama") }

                    val item = MovieBoxItem(
                        id = idx + 1,
                        title = cleanTitle,
                        year = yr,
                        rating = "8.0",
                        genre = genresFound.joinToString(", "),
                        genreArray = genresFound,
                        poster = img,
                        backdrop = img,
                        type = defaultType,
                        overview = "$cleanTitle ($yr) live-action ${defaultType.lowercase(Locale.US)}.",
                        language = "EN"
                    )
                    if (!isAnimationContent(item)) {
                        movies.add(item)
                    }
                }
            }

            if (movies.isEmpty()) {
                val jsonLdRegex = Regex("""<script[^>]*type=["']application/ld\+json["'][^>]*>([\s\S]*?)</script>""", RegexOption.IGNORE_CASE)
                jsonLdRegex.findAll(html).forEach { match ->
                    val jsonBlock = match.groupValues.getOrNull(1)?.trim().orEmpty()
                    if (jsonBlock.startsWith("{")) {
                        runCatching {
                            val jsonData = JSONObject(jsonBlock)
                            val itemList = jsonData.optJSONArray("itemListElement")
                            if (itemList != null) {
                                for (i in 0 until itemList.length()) {
                                    val elem = itemList.optJSONObject(i) ?: continue
                                    val innerItem = elem.optJSONObject("item") ?: elem
                                    val rawName = innerItem.optString("name").trim()
                                    if (rawName.isNotBlank()) {
                                        val cleanName = rawName.replace(Regex("\\[.*?]"), "").trim()
                                        val img = innerItem.optString("image").trim()
                                        val desc = innerItem.optString("description")
                                            .ifBlank { "$cleanName live-action feature." }
                                        val candidate = MovieBoxItem(
                                            id = movies.size + 1,
                                            title = cleanName,
                                            year = "2024",
                                            rating = "8.0",
                                            genre = "Action, Drama",
                                            genreArray = listOf("Action", "Drama"),
                                            poster = img,
                                            backdrop = img,
                                            type = defaultType,
                                            overview = desc,
                                            language = "EN"
                                        )
                                        if (!isAnimationContent(candidate)) {
                                            movies.add(candidate)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }

        return movies
    }

    private fun isAnimationContent(item: MovieBoxItem): Boolean {
        val lowerTitle = item.title.lowercase(Locale.US)
        val lowerGenre = item.genre.lowercase(Locale.US)
        return lowerTitle.contains("anime") ||
            lowerTitle.contains("sakamoto days") ||
            lowerGenre.contains("animation") ||
            item.genreArray.any { it.equals("Animation", ignoreCase = true) || it.equals("Anime", ignoreCase = true) }
    }

    /**
     * Try fetching from MovieBox.ng primary endpoints, then TMDB secondary endpoint, then OMDb tertiary endpoint.
     */
    suspend fun fetchRemoteCategory(category: String, page: Int = 1): List<MovieBoxItem> {
        val defaultType = if (category.equals("series", ignoreCase = true)) "Series" else "Movie"
        val movieBoxEndpoints = when (category.lowercase(Locale.US)) {
            "trending" -> listOf(
                "$baseURL/trending?page=$page",
                "$webURL/web/ranking-list?id=5514044369772445984&page=$page"
            )
            "movies" -> listOf(
                "$baseURL/movies?page=$page"
            )
            "series" -> listOf(
                "$baseURL/series?page=$page"
            )
            else -> listOf("$baseURL/trending?page=$page")
        }

        // 1. Primary: MovieBox.ng API
        for (endpoint in movieBoxEndpoints) {
            try {
                val payload = fetchWithRetry(endpoint, retries = 2, baseBackoffMs = 250L)
                val parsed = parseMovieListPayload(payload, defaultType = defaultType)
                if (parsed.isNotEmpty()) return parsed
            } catch (_: Throwable) {
            }
        }

        // 2. Secondary: TMDB API (if TMDB_API_KEY is configured and not demo_key)
        if (tmdbApiKey.isNotBlank() && tmdbApiKey != "demo_key") {
            val tmdbPath = when (category.lowercase(Locale.US)) {
                "trending" -> "/trending/all/day"
                "series" -> "/tv/popular"
                else -> "/movie/popular"
            }
            val tmdbUrl = "$tmdbURL$tmdbPath?api_key=${URLEncoder.encode(tmdbApiKey, "UTF-8")}&page=$page"
            try {
                val payload = fetchWithRetry(tmdbUrl, retries = 2, baseBackoffMs = 250L)
                val parsed = parseMovieListPayload(payload, defaultType = defaultType)
                if (parsed.isNotEmpty()) return parsed
            } catch (_: Throwable) {
            }
        }

        // 3. Tertiary: OMDb API (if OMDB_API_KEY is configured)
        if (omdbApiKey.isNotBlank()) {
            val omdbType = if (defaultType == "Series") "series" else "movie"
            val omdbSearchTerm = if (defaultType == "Series") "series" else "movie"
            val omdbEndpoint = "$omdbURL?apikey=${URLEncoder.encode(omdbApiKey, "UTF-8")}&s=$omdbSearchTerm&type=$omdbType&y=2024&page=$page"
            try {
                val payload = fetchWithRetry(omdbEndpoint, retries = 2, baseBackoffMs = 250L)
                val parsed = parseMovieListPayload(payload, defaultType = defaultType)
                if (parsed.isNotEmpty()) return parsed
            } catch (_: Throwable) {
            }
        }

        return emptyList()
    }

    /**
     * Searches MovieBox.ng, TMDB (`/search/multi`), and OMDb (`/?s=...`) for [query] at [page].
     */
    suspend fun fetchRemoteSearch(query: String, page: Int = 1): List<MovieBoxItem> {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return emptyList()
        val encoded = URLEncoder.encode(cleanQuery, "UTF-8")

        val endpoints = mutableListOf(
            "$baseURL/search?q=$encoded&page=$page",
            "$webURL/api/search?query=$encoded&page=$page",
            "$webURL/search?keyword=$encoded"
        )
        if (tmdbApiKey.isNotBlank() && tmdbApiKey != "demo_key") {
            endpoints.add("$tmdbURL/search/multi?api_key=${URLEncoder.encode(tmdbApiKey, "UTF-8")}&query=$encoded&page=$page")
        }
        if (omdbApiKey.isNotBlank()) {
            endpoints.add("$omdbURL?apikey=${URLEncoder.encode(omdbApiKey, "UTF-8")}&s=$encoded&page=$page")
        }

        for (endpoint in endpoints) {
            try {
                val payload = fetchWithRetry(endpoint, retries = 2, baseBackoffMs = 250L)
                val parsed = parseMovieListPayload(payload)
                if (parsed.isNotEmpty()) return parsed
            } catch (_: Throwable) {
            }
        }
        return emptyList()
    }

    /**
     * Fetches Movie/Series detail from MovieBox.ng or TMDB detail endpoints.
     */
    suspend fun fetchRemoteMovieDetails(id: Int): MovieBoxItem? {
        val endpoints = mutableListOf(
            "$baseURL/movie/$id",
            "$baseURL/details/$id",
            "$webURL/api/movie/$id",
            "$webURL/video/$id"
        )
        if (tmdbApiKey.isNotBlank() && tmdbApiKey != "demo_key") {
            endpoints.add("$tmdbURL/movie/$id?api_key=${URLEncoder.encode(tmdbApiKey, "UTF-8")}")
        }

        for (endpoint in endpoints) {
            try {
                val payload = fetchWithRetry(endpoint, retries = 2, baseBackoffMs = 250L)
                val trimmed = payload.trim()
                if (trimmed.startsWith("{")) {
                    val obj = JSONObject(trimmed)
                    val dataObj = obj.optJSONObject("data") ?: obj
                    val parsed = parseSingleMovieJson(dataObj, fallbackId = id)
                    if (parsed != null) return parsed
                }
            } catch (_: Throwable) {
            }
        }
        return null
    }

    /**
     * Queries MovieBox streaming endpoints (`/movie/{id}/stream`, `/series/{id}/stream`, `/stream?id={id}&type={type}`)
     * for a direct playable video stream URL (`.mp4`, `.m3u8`, `.mkv`, `.webm`).
     */
    suspend fun fetchRemoteStreamingData(
        id: Int,
        type: String = "movie",
        episodeNumber: Int? = null
    ): String? {
        val cleanType = if (type.equals("series", ignoreCase = true) || type.equals("tv", ignoreCase = true)) "series" else "movie"
        val epParam = if (episodeNumber != null && episodeNumber > 0) "&episode=$episodeNumber" else ""
        val endpoints = listOf(
            "$baseURL/$cleanType/$id/stream${if (epParam.isNotEmpty()) "?episode=$episodeNumber" else ""}",
            "$baseURL/stream?id=$id&type=$cleanType$epParam",
            "$webURL/api/$cleanType/$id/stream${if (epParam.isNotEmpty()) "?episode=$episodeNumber" else ""}"
        )
        for (endpoint in endpoints) {
            try {
                val payload = fetchWithRetry(endpoint, retries = 1, baseBackoffMs = 200L).trim()
                if (payload.startsWith("{")) {
                    val obj = JSONObject(payload)
                    val dataObj = obj.optJSONObject("data") ?: obj.optJSONObject("results") ?: obj
                    val urlCandidate = dataObj.optString("streamUrl")
                        .ifBlank { dataObj.optString("url") }
                        .ifBlank { dataObj.optString("downloadUrl") }
                        .ifBlank { dataObj.optString("file") }
                        .trim()
                    if (urlCandidate.startsWith("http", ignoreCase = true) &&
                        !urlCandidate.contains("utm_source=", ignoreCase = true) &&
                        (urlCandidate.contains(".mp4", ignoreCase = true) ||
                            urlCandidate.contains(".m3u8", ignoreCase = true) ||
                            urlCandidate.contains(".mkv", ignoreCase = true) ||
                            urlCandidate.contains(".webm", ignoreCase = true))
                    ) {
                        return urlCandidate
                    }
                }
            } catch (_: Throwable) {
            }
        }
        return null
    }

    /**
     * Creates a URL-friendly slug and MovieBox.ng direct video URL as documented in `src/services/api.js`.
     */
    fun createMovieBoxUrl(movieTitle: String, id: Int): String {
        val cleanSlug = movieTitle
            .replace(Regex("\\[.*?]"), "")
            .trim()
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9\\s]"), "")
            .replace(Regex("\\s+"), "-")
            .replace(Regex("-+"), "-")
            .trim('-')
        return "$webURL/video/$cleanSlug-$id"
    }
}
