package com.example.data.remote

import com.google.gson.annotations.SerializedName
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Retrofit client and DTO definitions for the public AniKotoAPI:
 * Base URL: https://anikototvapi.vercel.app/api/
 * Repository: https://github.com/Shineii86/AniKotoAPI
 *
 * Enforces strict host validation (anikototvapi.vercel.app over HTTPS) and bounded timeouts.
 */
interface AniKotoApiService {

    @GET("trending")
    suspend fun getTrending(): AniKotoTrendingResponse

    /**
     * Supports `GET /search?q=naruto` as well as `keyword=naruto` required by the upstream Express handler.
     */
    @GET("search")
    suspend fun searchAnime(
        @Query("q") q: String,
        @Query("keyword") keyword: String = q,
        @Query("page") page: Int = 1
    ): AniKotoSearchResponse

    /**
     * Supports `GET /info?link=/anime/naruto` as well as normalized `slug` and `id` query parameters
     * required by the upstream Express handler.
     */
    @GET("info")
    suspend fun getAnimeInfo(
        @Query("link") link: String,
        @Query("slug") slug: String,
        @Query("id") id: String = slug
    ): AniKotoInfoResponse

    @GET("episodes/{animeId}")
    suspend fun getAnimeEpisodes(
        @Path("animeId") animeId: String
    ): AniKotoEpisodesResponse

    /**
     * Fetches available streaming servers for an episode using its encoded `server_ids` token.
     * Endpoint: `GET /servers?ids={server_ids}`
     */
    @GET("servers")
    suspend fun getEpisodeServers(
        @Query("ids") ids: String
    ): AniKotoServersResponse

    /**
     * Resolves a server's `link_id` into a direct HLS (`.m3u8`) or MP4 video stream URL.
     * Endpoint: `GET /stream/resolve?id={link_id}&slug={slug}`
     */
    @GET("stream/resolve")
    suspend fun resolveStreamUrl(
        @Query("id") linkId: String,
        @Query("slug") slug: String? = null
    ): AniKotoStreamResolveResponse

    /**
     * Fetches embed/stream metadata for a server's `link_id`.
     * Endpoint: `GET /stream?id={link_id}`
     */
    @GET("stream")
    suspend fun getStreamInfo(
        @Query("id") linkId: String
    ): AniKotoStreamInfoResponse

    /**
     * Fetches available quality variants from a master M3U8 playlist URL.
     * Endpoint: `GET /stream/qualities?url={playlistUrl}`
     */
    @GET("stream/qualities")
    suspend fun getStreamQualities(
        @Query("url") playlistUrl: String
    ): AniKotoStreamQualitiesResponse

    /**
     * Fetches direct MP4/MKV download & stream links for an episode.
     * Endpoint: `GET /download?slug={slug}&ep={ep}`
     */
    @GET("download")
    suspend fun getDownloadLinks(
        @Query("slug") slug: String,
        @Query("ep") ep: Int
    ): AniKotoDownloadResponse

    companion object {
        const val ALLOWED_HOST = "anikototvapi.vercel.app"
        const val BASE_URL = "https://anikototvapi.vercel.app/api/"
        private const val CONNECT_TIMEOUT_SECONDS = 10L
        private const val READ_TIMEOUT_SECONDS = 12L
        private const val CALL_TIMEOUT_SECONDS = 15L

        /**
         * Builds a proxied M3U8 stream URL via `GET /stream/proxy?url=...` on the trusted AniKotoAPI host.
         */
        fun buildM3u8ProxyUrl(rawM3u8Url: String, referer: String? = null): String {
            val encodedUrl = java.net.URLEncoder.encode(rawM3u8Url.trim(), "UTF-8")
            val baseProxy = "${BASE_URL}stream/proxy?url=$encodedUrl"
            return if (!referer.isNullOrBlank()) {
                "$baseProxy&referer=${java.net.URLEncoder.encode(referer.trim(), "UTF-8")}"
            } else {
                baseProxy
            }
        }

        /**
         * Security interceptor that strictly restricts outbound requests to https://anikototvapi.vercel.app
         * and prevents unrestricted proxying or off-host redirects.
         */
        class StrictHostInterceptor : Interceptor {
            override fun intercept(chain: Interceptor.Chain): Response {
                val request = chain.request()
                val url = request.url
                if (!url.isHttps || !url.host.equals(ALLOWED_HOST, ignoreCase = true)) {
                    throw IOException("Blocked request to untrusted host: ${url.host}")
                }
                val sanitizedRequest = request.newBuilder()
                    .header("Accept", "application/json")
                    .header("User-Agent", "AniTrAk-Android/1.0 (AniKotoAPI-Integration)")
                    .build()
                val response = chain.proceed(sanitizedRequest)
                val responseHost = response.request.url.host
                if (!response.request.url.isHttps || !responseHost.equals(ALLOWED_HOST, ignoreCase = true)) {
                    response.close()
                    throw IOException("Blocked redirect to untrusted host: $responseHost")
                }
                return response
            }
        }

        fun create(baseUrl: String = BASE_URL): AniKotoApiService {
            val client = OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .addInterceptor(StrictHostInterceptor())
                .build()

            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(AniKotoApiService::class.java)
        }
    }
}

// ============================================================================
// Verified Response Schemas (Confirmed against https://anikototvapi.vercel.app/api)
// ============================================================================

data class AniKotoTrendingResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("results") val results: List<AniKotoSummaryDto>? = emptyList(),
    @SerializedName("message") val message: String? = null
)

data class AniKotoSearchResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("results") val results: AniKotoSearchPageDto? = null,
    @SerializedName("message") val message: String? = null
)

data class AniKotoSearchPageDto(
    @SerializedName("totalPages") val totalPages: Int? = 1,
    @SerializedName("data") val data: List<AniKotoSummaryDto>? = emptyList()
)

data class AniKotoSummaryDto(
    @SerializedName("slug") val slug: String? = null,
    @SerializedName("animeId") val animeId: String? = null,
    @SerializedName("poster") val poster: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("japaneseTitle") val japaneseTitle: String? = null,
    @SerializedName("sub") val sub: Int? = null,
    @SerializedName("dub") val dub: Int? = null,
    @SerializedName("total") val total: Int? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("rating") val rating: String? = null,
    @SerializedName("genres") val genres: List<String>? = emptyList()
)

data class AniKotoInfoResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("results") val results: AniKotoInfoDto? = null,
    @SerializedName("message") val message: String? = null
)

data class AniKotoInfoDto(
    @SerializedName("slug") val slug: String? = null,
    @SerializedName("animeId") val animeId: Any? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("japaneseTitle") val japaneseTitle: String? = null,
    @SerializedName("altNames") val altNames: String? = null,
    @SerializedName("poster") val poster: String? = null,
    @SerializedName("backgroundImage") val backgroundImage: String? = null,
    @SerializedName("synopsis") val synopsis: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("premiered") val premiered: String? = null,
    @SerializedName("aired") val aired: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("malScore") val malScore: String? = null,
    @SerializedName("duration") val duration: String? = null,
    @SerializedName("episodes") val episodes: String? = null,
    @SerializedName("studios") val studios: List<String>? = emptyList(),
    @SerializedName("producers") val producers: List<String>? = emptyList(),
    @SerializedName("genres") val genres: List<String>? = emptyList(),
    @SerializedName("rating") val rating: String? = null,
    @SerializedName("reviewCount") val reviewCount: String? = null
)

data class AniKotoEpisodesResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("results") val results: AniKotoEpisodesResultDto? = null,
    @SerializedName("message") val message: String? = null
)

data class AniKotoEpisodesResultDto(
    @SerializedName("animeId") val animeId: Any? = null,
    @SerializedName("slug") val slug: String? = null,
    @SerializedName("totalEpisodes") val totalEpisodes: Int? = 0,
    @SerializedName("episodes") val episodes: List<AniKotoEpisodeDto>? = emptyList()
)

data class AniKotoEpisodeDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("episode_no") val episodeNo: Int? = null,
    @SerializedName("slug") val slug: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("active") val active: Boolean? = null,
    @SerializedName("server_ids") val serverIds: String? = null,
    @SerializedName("timestamp") val timestamp: String? = null,
    @SerializedName("mal_id") val malId: String? = null
)

data class AniKotoServersResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("results") val results: List<AniKotoServerDto>? = emptyList(),
    @SerializedName("message") val message: String? = null
)

data class AniKotoServerDto(
    @SerializedName("type") val type: String? = null,
    @SerializedName("ep_id") val epId: String? = null,
    @SerializedName("link_id") val linkId: String? = null,
    @SerializedName("cmid") val cmid: String? = null,
    @SerializedName("sv_id") val svId: String? = null,
    @SerializedName("name") val name: String? = null
)

data class AniKotoStreamResolveResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("results") val results: AniKotoStreamResolveDto? = null,
    @SerializedName("message") val message: String? = null
)

data class AniKotoStreamResolveDto(
    @SerializedName("url") val url: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("server") val server: String? = null,
    @SerializedName("subtitles") val subtitles: List<AniKotoSubtitleDto>? = emptyList()
)

data class AniKotoSubtitleDto(
    @SerializedName("url") val url: String? = null,
    @SerializedName("file") val file: String? = null,
    @SerializedName("lang") val lang: String? = null,
    @SerializedName("label") val label: String? = null,
    @SerializedName("kind") val kind: String? = null
)

data class AniKotoStreamInfoResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("results") val results: AniKotoStreamInfoDto? = null,
    @SerializedName("message") val message: String? = null
)

data class AniKotoStreamInfoDto(
    @SerializedName("type") val type: String? = null,
    @SerializedName("url") val url: String? = null,
    @SerializedName("server") val server: String? = null,
    @SerializedName("subtitles") val subtitles: List<AniKotoSubtitleDto>? = emptyList()
)

data class AniKotoStreamQualitiesResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("results") val results: AniKotoStreamQualitiesDto? = null,
    @SerializedName("message") val message: String? = null
)

data class AniKotoStreamQualitiesDto(
    @SerializedName("isMasterPlaylist") val isMasterPlaylist: Boolean? = false,
    @SerializedName("qualities") val qualities: List<AniKotoQualityVariantDto>? = emptyList()
)

data class AniKotoQualityVariantDto(
    @SerializedName("quality") val quality: String? = null,
    @SerializedName("resolution") val resolution: String? = null,
    @SerializedName("bandwidth") val bandwidth: Long? = null,
    @SerializedName("url") val url: String? = null
)

data class AniKotoDownloadResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("results") val results: AniKotoDownloadResultDto? = null,
    @SerializedName("message") val message: String? = null
)

data class AniKotoDownloadResultDto(
    @SerializedName("slug") val slug: String? = null,
    @SerializedName("episode") val episode: Int? = null,
    @SerializedName("totalDownloads") val totalDownloads: Int? = 0,
    @SerializedName("downloads") val downloads: List<AniKotoDownloadItemDto>? = emptyList()
)

data class AniKotoDownloadItemDto(
    @SerializedName("quality") val quality: String? = null,
    @SerializedName("url") val url: String? = null,
    @SerializedName("server") val server: String? = null,
    @SerializedName("type") val type: String? = null
)
