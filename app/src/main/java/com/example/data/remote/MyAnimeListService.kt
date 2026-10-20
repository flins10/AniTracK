package com.example.data.remote

import android.util.Log
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class MalUserListStatus(
    @Json(name = "status") val status: String? = "watching",
    @Json(name = "score") val score: Int? = 0,
    @Json(name = "num_episodes_watched") val numEpisodesWatched: Int? = 0,
    @Json(name = "is_rewatching") val isRewatching: Boolean? = false,
    @Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class MalBroadcast(
    @Json(name = "day_of_the_week") val dayOfTheWeek: String? = null,
    @Json(name = "start_time") val startTime: String? = null
)

@JsonClass(generateAdapter = true)
data class MalAnimeNode(
    @Json(name = "id") val id: Int = 0,
    @Json(name = "title") val title: String = "",
    @Json(name = "main_picture") val mainPicture: MalPicture? = null,
    @Json(name = "num_episodes") val numEpisodes: Int? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "broadcast") val broadcast: MalBroadcast? = null,
    @Json(name = "mean") val mean: Double? = null,
    @Json(name = "synopsis") val synopsis: String? = null
)

@JsonClass(generateAdapter = true)
data class MalPicture(
    @Json(name = "medium") val medium: String? = null,
    @Json(name = "large") val large: String? = null
)

@JsonClass(generateAdapter = true)
data class MalAnimeListItem(
    @Json(name = "node") val node: MalAnimeNode,
    @Json(name = "list_status") val listStatus: MalUserListStatus? = null
)

@JsonClass(generateAdapter = true)
data class MalUserAnimeListResponse(
    @Json(name = "data") val data: List<MalAnimeListItem>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class MalUserProfile(
    @Json(name = "id") val id: Int = 0,
    @Json(name = "name") val name: String = "",
    @Json(name = "picture") val picture: String? = null,
    @Json(name = "joined_at") val joinedAt: String? = null
)

data class MalSyncLog(
    val animeTitle: String,
    val malId: Int,
    val httpStatusCode: Int,
    val requestUrl: String,
    val requestMethod: String = "PUT",
    val responseBody: String,
    val success: Boolean,
    val verificationPassed: Boolean,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

object MyAnimeListService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * Converts MyAnimeList broadcast day string ("mondays", "sundays", etc.) into DayOfWeek integer (1=Mon..7=Sun).
     */
    fun parseMalDayOfWeek(day: String?): Int {
        return when (day?.lowercase()?.trim()) {
            "monday", "mondays" -> 1
            "tuesday", "tuesdays" -> 2
            "wednesday", "wednesdays" -> 3
            "thursday", "thursdays" -> 4
            "friday", "fridays" -> 5
            "saturday", "saturdays" -> 6
            "sunday", "sundays" -> 7
            else -> 0
        }
    }

    /**
     * Fetches current authenticated user profile from MyAnimeList.
     */
    suspend fun getUserProfile(accessToken: String): Result<MalUserProfile> = withContext(Dispatchers.IO) {
        try {
            val reqBuilder = Request.Builder()
                .url("${MyAnimeListConfig.API_BASE_URL}users/@me")
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
            val request = reqBuilder.build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val str = response.body?.string() ?: ""
                    val json = JSONObject(str)
                    val profile = MalUserProfile(
                        id = json.optInt("id", 0),
                        name = json.optString("name", "Otaku User"),
                        picture = json.optString("picture", null),
                        joinedAt = json.optString("joined_at", null)
                    )
                    Result.success(profile)
                } else {
                    Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                }
            }
        } catch (e: Exception) {
            Log.e("MyAnimeListService", "Failed to fetch MAL profile: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Fetches authenticated user's anime list from MyAnimeList with broadcast info and details.
     */
    suspend fun getUserAnimeList(
        accessToken: String,
        statusFilter: String? = null
    ): Result<List<MalAnimeListItem>> = withContext(Dispatchers.IO) {
        try {
            val statusParam = if (!statusFilter.isNull_or_empty()) "&status=$statusFilter" else ""
            val url = "${MyAnimeListConfig.API_BASE_URL}users/@me/animelist?fields=list_status,num_episodes,status,broadcast,mean,synopsis&limit=100$statusParam"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val str = response.body?.string() ?: ""
                    val json = JSONObject(str)
                    val dataArray = json.optJSONArray("data")
                    val items = mutableListOf<MalAnimeListItem>()
                    if (dataArray != null) {
                        for (i in 0 until dataArray.length()) {
                            val obj = dataArray.getJSONObject(i)
                            val nodeObj = obj.optJSONObject("node")
                            val statusObj = obj.optJSONObject("list_status")
                            if (nodeObj != null) {
                                val picObj = nodeObj.optJSONObject("main_picture")
                                val broadcastObj = nodeObj.optJSONObject("broadcast")
                                val broadcast = if (broadcastObj != null) {
                                    MalBroadcast(
                                        dayOfTheWeek = broadcastObj.optString("day_of_the_week", null),
                                        startTime = broadcastObj.optString("start_time", null)
                                    )
                                } else null
                                val node = MalAnimeNode(
                                    id = nodeObj.optInt("id"),
                                    title = nodeObj.optString("title", "Untitled"),
                                    mainPicture = MalPicture(
                                        medium = picObj?.optString("medium"),
                                        large = picObj?.optString("large")
                                    ),
                                    numEpisodes = nodeObj.optInt("num_episodes"),
                                    status = nodeObj.optString("status"),
                                    broadcast = broadcast,
                                    mean = nodeObj.optDouble("mean", 0.0),
                                    synopsis = nodeObj.optString("synopsis", null)
                                )
                                val listStatus = if (statusObj != null) {
                                    MalUserListStatus(
                                        status = statusObj.optString("status", "watching"),
                                        score = statusObj.optInt("score", 0),
                                        numEpisodesWatched = statusObj.optInt("num_episodes_watched", 0),
                                        isRewatching = statusObj.optBoolean("is_rewatching", false),
                                        updatedAt = statusObj.optString("updated_at", null)
                                    )
                                } else null
                                items.add(MalAnimeListItem(node = node, listStatus = listStatus))
                            }
                        }
                    }
                    Result.success(items)
                } else {
                    Result.failure(Exception("HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Log.e("MyAnimeListService", "Failed to fetch MAL anime list: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Resolves official MAL mal_id by searching MAL API with anime title.
     */
    suspend fun fetchJikanAnimeDetails(malId: Int): Result<MalAnimeNode> = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.jikan.moe/v4/anime/$malId"
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "OtakuApp/1.0")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                val str = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = JSONObject(str)
                    val data = json.optJSONObject("data")
                    if (data != null) {
                        val titleEng = data.optString("title_english", null)?.takeIf { it.isNotBlank() }
                            ?: data.optString("title", "Untitled")
                        val eps = if (data.has("episodes") && !data.isNull("episodes")) data.optInt("episodes") else null
                        val statusStr = data.optString("status", null)
                        val broadcastObj = data.optJSONObject("broadcast")
                        val dayStr = broadcastObj?.optString("day", null)
                        val node = MalAnimeNode(
                            id = malId,
                            title = titleEng,
                            numEpisodes = eps,
                            status = statusStr,
                            broadcast = MalBroadcast(dayOfTheWeek = dayStr)
                        )
                        Result.success(node)
                    } else {
                        Result.failure(Exception("No data in Jikan response"))
                    }
                } else {
                    Result.failure(Exception("HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches the latest aired episode count from Jikan for a given MAL anime ID.
     */
    suspend fun fetchJikanLatestAiredEpisode(malId: Int): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.jikan.moe/v4/anime/$malId/episodes"
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "OtakuApp/1.0")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                val str = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = JSONObject(str)
                    val data = json.optJSONArray("data")
                    val pagination = json.optJSONObject("pagination")
                    if (data != null && data.length() > 0) {
                        val lastVisiblePage = pagination?.optInt("last_visible_page", 1) ?: 1
                        if (lastVisiblePage > 1) {
                            val lastPageUrl = "https://api.jikan.moe/v4/anime/$malId/episodes?page=$lastVisiblePage"
                            val lastPageRequest = Request.Builder()
                                .url(lastPageUrl)
                                .addHeader("User-Agent", "OtakuApp/1.0")
                                .get()
                                .build()
                            client.newCall(lastPageRequest).execute().use { lpResp ->
                                val lpStr = lpResp.body?.string() ?: ""
                                val lpJson = JSONObject(lpStr)
                                val lpData = lpJson.optJSONArray("data")
                                if (lpData != null && lpData.length() > 0) {
                                    val lastEpObj = lpData.optJSONObject(lpData.length() - 1)
                                    val epNum = lastEpObj?.optInt("mal_id", 0) ?: 0
                                    if (epNum > 0) return@withContext Result.success(epNum)
                                }
                            }
                        }
                        val lastEpObj = data.optJSONObject(data.length() - 1)
                        val epNum = lastEpObj?.optInt("mal_id", 0) ?: 0
                        if (epNum > 0) {
                            return@withContext Result.success(epNum)
                        }
                    }
                }
                Result.failure(Exception("No episode data from Jikan"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches official anime node details directly from MyAnimeList API using Client ID or Access Token.
     */
    suspend fun fetchMalAnimeDetails(malId: Int, accessToken: String? = null): Result<MalAnimeNode> = withContext(Dispatchers.IO) {
        try {
            val url = "${MyAnimeListConfig.API_BASE_URL}anime/$malId?fields=id,title,main_picture,num_episodes,status,broadcast,mean,synopsis"
            val builder = Request.Builder().url(url)
            if (!accessToken.isNullOrEmpty()) {
                builder.addHeader("Authorization", "Bearer $accessToken")
            }
            val request = builder.get().build()
            client.newCall(request).execute().use { response ->
                val str = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = JSONObject(str)
                    val picObj = json.optJSONObject("main_picture")
                    val broadcastObj = json.optJSONObject("broadcast")
                    val broadcast = if (broadcastObj != null) {
                        MalBroadcast(
                            dayOfTheWeek = broadcastObj.optString("day_of_the_week", null),
                            startTime = broadcastObj.optString("start_time", null)
                        )
                    } else null
                    val node = MalAnimeNode(
                        id = json.optInt("id", malId),
                        title = json.optString("title", "Untitled"),
                        mainPicture = MalPicture(
                            medium = picObj?.optString("medium"),
                            large = picObj?.optString("large")
                        ),
                        numEpisodes = if (json.has("num_episodes") && !json.isNull("num_episodes")) json.optInt("num_episodes") else null,
                        status = json.optString("status"),
                        broadcast = broadcast,
                        mean = if (json.has("mean") && !json.isNull("mean")) json.optDouble("mean") else null,
                        synopsis = json.optString("synopsis", null)
                    )
                    Result.success(node)
                } else {
                    Result.failure(Exception("HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Resolves official MAL mal_id by searching MAL API with anime title.
     */
    suspend fun searchMalAnimeByTitle(accessToken: String, query: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
            val url = "${MyAnimeListConfig.API_BASE_URL}anime?q=$encodedQuery&limit=1"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = JSONObject(bodyStr)
                    val data = json.optJSONArray("data")
                    if (data != null && data.length() > 0) {
                        val firstObj = data.getJSONObject(0)
                        val node = firstObj.optJSONObject("node")
                        val malId = node?.optInt("id") ?: 0
                        if (malId > 0) {
                            Result.success(malId)
                        } else {
                            Result.failure(Exception("MAL ID not found in response"))
                        }
                    } else {
                        Result.failure(Exception("No matching anime found on MyAnimeList for title: $query"))
                    }
                } else {
                    Result.failure(Exception("HTTP ${response.code}: $bodyStr"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun normalizeMalStatus(input: String?): String? {
        if (input == null) return null
        return when (input.lowercase().trim()) {
            "watching", "releasing", "currently_airing", "airing", "watchlist" -> "watching"
            "completed", "finished" -> "completed"
            "on_hold", "onhold", "paused" -> "on_hold"
            "dropped", "cancelled" -> "dropped"
            "plan_to_watch", "plantowatch", "wishlist", "planning" -> "plan_to_watch"
            else -> "watching"
        }
    }

    /**
     * Updates an anime watch progress or status on MyAnimeList via official OAuth 2.0 API,
     * immediately verifies the update via GET request to MyAnimeList, and returns detailed log details.
     */
    suspend fun updateAndVerifyAnimeStatusDetailed(
        accessToken: String,
        animeTitle: String,
        malId: Int,
        numEpisodesWatched: Int? = null,
        status: String? = null, // "watching", "completed", "on_hold", "dropped", "plan_to_watch"
        score: Int? = null
    ): MalSyncLog = withContext(Dispatchers.IO) {
        val cleanStatus = normalizeMalStatus(status)
        val putUrl = "${MyAnimeListConfig.API_BASE_URL}anime/$malId/my_list_status"
        val formBuilder = FormBody.Builder()
        if (numEpisodesWatched != null && numEpisodesWatched >= 0) {
            formBuilder.add("num_watched_episodes", numEpisodesWatched.toString())
        }
        if (cleanStatus != null) {
            formBuilder.add("status", cleanStatus)
        }
        if (score != null && score in 1..10) {
            formBuilder.add("score", score.toString())
        }

        val cleanToken = accessToken.trim().removePrefix("Bearer ").trim()
        val request = Request.Builder()
            .url(putUrl)
            .addHeader("Authorization", "Bearer $cleanToken")
            .put(formBuilder.build())
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val responseCode = response.code
                val responseBodyStr = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    val errReason = parseMalErrorReason(responseCode, responseBodyStr)
                    return@withContext MalSyncLog(
                        animeTitle = animeTitle,
                        malId = malId,
                        httpStatusCode = responseCode,
                        requestUrl = putUrl,
                        requestMethod = "PUT",
                        responseBody = responseBodyStr,
                        success = false,
                        verificationPassed = false,
                        message = "MAL Update Failed (HTTP $responseCode): $errReason"
                    )
                }

                // Step 4: Immediately fetch the user's MyAnimeList status to verify
                val verifyUrl = "${MyAnimeListConfig.API_BASE_URL}anime/$malId?fields=my_list_status"
                val verifyRequest = Request.Builder()
                    .url(verifyUrl)
                    .addHeader("Authorization", "Bearer $cleanToken")
                    .get()
                    .build()

                client.newCall(verifyRequest).execute().use { verifyResponse ->
                    val verifyCode = verifyResponse.code
                    val verifyBodyStr = verifyResponse.body?.string() ?: ""

                    if (verifyResponse.isSuccessful) {
                        val json = JSONObject(verifyBodyStr)
                        val listStatusObj = json.optJSONObject("my_list_status")
                        val serverStatus = listStatusObj?.optString("status")
                        val serverWatched = listStatusObj?.optInt("num_episodes_watched", -1)

                        val statusMatches = cleanStatus == null || serverStatus.equals(cleanStatus, ignoreCase = true)
                        val watchedMatches = numEpisodesWatched == null || serverWatched == numEpisodesWatched

                        if (listStatusObj != null && (statusMatches || watchedMatches)) {
                            return@withContext MalSyncLog(
                                animeTitle = animeTitle,
                                malId = malId,
                                httpStatusCode = responseCode,
                                requestUrl = putUrl,
                                requestMethod = "PUT",
                                responseBody = responseBodyStr,
                                success = true,
                                verificationPassed = true,
                                message = "Successfully synced and verified on MyAnimeList (Server Status: $serverStatus, Watched: $serverWatched)"
                            )
                        } else {
                            return@withContext MalSyncLog(
                                animeTitle = animeTitle,
                                malId = malId,
                                httpStatusCode = verifyCode,
                                requestUrl = verifyUrl,
                                requestMethod = "GET (Verification)",
                                responseBody = verifyBodyStr,
                                success = false,
                                verificationPassed = false,
                                message = "Sync failed: Verification check mismatch (Expected status=$cleanStatus, Watched=$numEpisodesWatched; Server status=$serverStatus, Watched=$serverWatched)"
                            )
                        }
                    } else {
                        return@withContext MalSyncLog(
                            animeTitle = animeTitle,
                            malId = malId,
                            httpStatusCode = verifyCode,
                            requestUrl = verifyUrl,
                            requestMethod = "GET (Verification)",
                            responseBody = verifyBodyStr,
                            success = false,
                            verificationPassed = false,
                            message = "Sync failed: Verification request returned HTTP $verifyCode"
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MyAnimeListService", "Exception updating MAL entry $malId: ${e.message}")
            return@withContext MalSyncLog(
                animeTitle = animeTitle,
                malId = malId,
                httpStatusCode = 0,
                requestUrl = putUrl,
                requestMethod = "PUT",
                responseBody = e.localizedMessage ?: "Network/Exception Error",
                success = false,
                verificationPassed = false,
                message = "Sync failed due to network exception: ${e.message}"
            )
        }
    }

    private fun parseMalErrorReason(code: Int, body: String): String {
        return try {
            if (body.contains("{")) {
                val json = JSONObject(body)
                val err = json.optString("error", "")
                val msg = json.optString("message", "")
                if (err.isNotEmpty() || msg.isNotEmpty()) "$err - $msg".trim(' ', '-') else body
            } else {
                when (code) {
                    401 -> "Invalid or expired OAuth token. Please re-authenticate."
                    400 -> "Invalid request parameters or invalid MAL ID."
                    404 -> "Anime not found on MyAnimeList."
                    403 -> "Forbidden or missing permissions."
                    else -> "HTTP $code"
                }
            }
        } catch (e: Exception) {
            "HTTP $code: $body"
        }
    }

    /**
     * Deletes an anime entry from user's MyAnimeList list.
     */
    suspend fun deleteAnimeFromMal(
        accessToken: String,
        malId: Int
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val cleanToken = accessToken.trim().removePrefix("Bearer ").trim()
            val url = "${MyAnimeListConfig.API_BASE_URL}anime/$malId/my_list_status"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $cleanToken")
                .delete()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 404) {
                    Result.success(true)
                } else {
                    Result.failure(Exception("HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Log.e("MyAnimeListService", "Failed to delete MAL entry $malId: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Updates an anime watch progress or status on MyAnimeList.
     */
    suspend fun updateAnimeStatusOnMal(
        accessToken: String,
        animeId: Int,
        numEpisodesWatched: Int? = null,
        status: String? = null, // "watching", "completed", "on_hold", "dropped", "plan_to_watch"
        score: Int? = null
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val cleanStatus = normalizeMalStatus(status)
            val cleanToken = accessToken.trim().removePrefix("Bearer ").trim()
            val url = "${MyAnimeListConfig.API_BASE_URL}anime/$animeId/my_list_status"
            val formBuilder = FormBody.Builder()
            if (numEpisodesWatched != null && numEpisodesWatched >= 0) {
                formBuilder.add("num_watched_episodes", numEpisodesWatched.toString())
            }
            if (cleanStatus != null) {
                formBuilder.add("status", cleanStatus)
            }
            if (score != null && score in 1..10) {
                formBuilder.add("score", score.toString())
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $cleanToken")
                .put(formBuilder.build())
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(true)
                } else {
                    Result.failure(Exception("HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Log.e("MyAnimeListService", "Failed to update MAL status: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Fetches public user profile from Jikan / MAL by username.
     */
    suspend fun fetchUserPublicProfile(username: String): Result<MalUserProfile> = withContext(Dispatchers.IO) {
        try {
            val cleanUser = username.trim()
            val url = "https://api.jikan.moe/v4/users/$cleanUser/full"
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "OtakuApp/1.0")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                val str = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = JSONObject(str)
                    val data = json.optJSONObject("data")
                    if (data != null) {
                        val name = data.optString("username", cleanUser)
                        val images = data.optJSONObject("images")
                        val jpg = images?.optJSONObject("jpg")
                        val pic = jpg?.optString("image_url", null)
                        val joined = data.optString("joined", null)
                        val malId = data.optInt("mal_id", 0)
                        val profile = MalUserProfile(
                            id = malId,
                            name = name,
                            picture = pic,
                            joinedAt = joined
                        )
                        Result.success(profile)
                    } else {
                        Result.success(MalUserProfile(name = cleanUser))
                    }
                } else {
                    // Fallback to minimal profile
                    Result.success(MalUserProfile(name = cleanUser))
                }
            }
        } catch (e: Exception) {
            Log.e("MyAnimeListService", "Error fetching public profile: ${e.message}")
            Result.success(MalUserProfile(name = username.trim()))
        }
    }

    /**
     * Fetches user's public anime list by username using Jikan API.
     */
    suspend fun fetchUserPublicAnimeList(username: String): Result<List<MalAnimeListItem>> = withContext(Dispatchers.IO) {
        try {
            val cleanUser = username.trim()
            val url = "https://api.jikan.moe/v4/users/$cleanUser/animelist/all"
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "OtakuApp/1.0")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                val str = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = JSONObject(str)
                    val dataArray = json.optJSONArray("data")
                    val items = mutableListOf<MalAnimeListItem>()
                    if (dataArray != null) {
                        for (i in 0 until dataArray.length()) {
                            val itemObj = dataArray.getJSONObject(i)
                            val entryObj = itemObj.optJSONObject("entry")
                            val score = itemObj.optInt("score", 0)
                            val watched = itemObj.optInt("episodes_watched", 0)
                            val totalEps = itemObj.optInt("episodes_total", 0).takeIf { it > 0 }
                            val rawStatus = itemObj.optString("watching_status", itemObj.optString("status", "watching"))
                            if (entryObj != null) {
                                val malId = entryObj.optInt("mal_id", 0)
                                val title = entryObj.optString("title", "Untitled")
                                val images = entryObj.optJSONObject("images")
                                val jpg = images?.optJSONObject("jpg")
                                val picUrl = jpg?.optString("large_image_url", jpg?.optString("image_url", null))
                                val node = MalAnimeNode(
                                    id = malId,
                                    title = title,
                                    mainPicture = MalPicture(large = picUrl, medium = picUrl),
                                    numEpisodes = totalEps
                                )
                                val listStatus = MalUserListStatus(
                                    status = normalizeMalStatus(rawStatus),
                                    score = score,
                                    numEpisodesWatched = watched
                                )
                                items.add(MalAnimeListItem(node = node, listStatus = listStatus))
                            }
                        }
                    }
                    Result.success(items)
                } else {
                    Result.failure(Exception("HTTP ${response.code}: Could not fetch anime list for $cleanUser"))
                }
            }
        } catch (e: Exception) {
            Log.e("MyAnimeListService", "Error fetching user animelist: ${e.message}")
            Result.failure(e)
        }
    }
}

private fun String?.isNull_or_empty(): Boolean {
    return this == null || this.trim().isEmpty()
}
