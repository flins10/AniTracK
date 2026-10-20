package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class YouTubeSearchResponse(
    val kind: String? = null,
    val etag: String? = null,
    val nextPageToken: String? = null,
    val prevPageToken: String? = null,
    val pageInfo: YouTubePageInfo? = null,
    val items: List<YouTubeSearchResultItem>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class YouTubePageInfo(
    val totalResults: Int? = null,
    val resultsPerPage: Int? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeSearchResultItem(
    val id: YouTubeResourceId? = null,
    val snippet: YouTubeSnippet? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeResourceId(
    val kind: String? = null,
    val videoId: String? = null,
    val channelId: String? = null,
    val playlistId: String? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeSnippet(
    val publishedAt: String? = null,
    val channelId: String? = null,
    val title: String? = null,
    val description: String? = null,
    val thumbnails: YouTubeThumbnails? = null,
    val channelTitle: String? = null,
    val liveBroadcastContent: String? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeThumbnails(
    val default: YouTubeThumbnail? = null,
    val medium: YouTubeThumbnail? = null,
    val high: YouTubeThumbnail? = null,
    val standard: YouTubeThumbnail? = null,
    val maxres: YouTubeThumbnail? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeThumbnail(
    val url: String? = null,
    val width: Int? = null,
    val height: Int? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeVideoListResponse(
    val kind: String? = null,
    val etag: String? = null,
    val items: List<YouTubeVideoItem>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class YouTubeVideoItem(
    val id: String? = null,
    val snippet: YouTubeSnippet? = null,
    val contentDetails: YouTubeContentDetails? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeContentDetails(
    val duration: String? = null, // ISO 8601 duration e.g. PT24M12S
    val dimension: String? = null,
    val definition: String? = null,
    val caption: String? = null,
    val licensedContent: Boolean? = null
)

interface YouTubeApiService {
    @GET("search")
    suspend fun searchVideos(
        @Query("part") part: String = "snippet",
        @Query("type") type: String = "video",
        @Query("q") query: String,
        @Query("maxResults") maxResults: Int = 15,
        @Query("pageToken") pageToken: String? = null,
        @Query("videoEmbeddable") videoEmbeddable: String = "true",
        @Query("regionCode") regionCode: String = "IN",
        @Query("relevanceLanguage") relevanceLanguage: String? = null,
        @Query("key") apiKey: String
    ): Response<YouTubeSearchResponse>

    @GET("videos")
    suspend fun getVideoDetails(
        @Query("part") part: String = "snippet,contentDetails",
        @Query("id") ids: String,
        @Query("key") apiKey: String
    ): Response<YouTubeVideoListResponse>

    companion object {
        private const val BASE_URL = "https://www.googleapis.com/youtube/v3/"

        fun create(): YouTubeApiService {
            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()

            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(YouTubeApiService::class.java)
        }
    }
}
