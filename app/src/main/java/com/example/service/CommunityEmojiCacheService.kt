package com.example.service

import android.content.Context
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.config.AppConfig
import com.example.model.AuthSession
import com.example.model.CommunityEmoji
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Multi-layer custom emoji manager for Supabase Storage buckets ("community_emojis" & "community-emojis"):
 * - Stores ONLY emoji IDs (`[emoji:<id>]`) in chat messages; never stores binaries/URLs in database.
 * - Caches lightweight emoji metadata locally in SharedPreferences (15 min TTL) with stale-while-revalidate.
 * - Reuses a single canonical public Storage URL across all users, messages, replies, and picker slots.
 * - Deduplicates metadata & image requests and preloads top frequently used emojis non-blockingly via Coil
 *   using stable `memoryCacheKey` + `diskCacheKey` ("livtrak_emoji_<id>").
 */
class CommunityEmojiCacheService(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "livtrak_community_emoji_cache_v1"
        private const val KEY_META_JSON = "emoji_meta_json"
        private const val KEY_META_UPDATED_AT = "emoji_meta_updated_at"
        private const val KEY_LOCAL_USAGE_JSON = "emoji_local_usage_json"
        private const val META_TTL_MS = 15L * 60L * 1000L // 15 minutes
        private val SUPPORTED_BUCKETS = listOf("community_emojis", "community-emojis")
        val EMOJI_TOKEN_REGEX = Regex("""\[emoji:([a-zA-Z0-9_-]{1,64})]""")

        fun extractEmojiIds(text: String): List<String> {
            if (text.isBlank()) return emptyList()
            return EMOJI_TOKEN_REGEX.findAll(text).map { it.groupValues[1] }.toList()
        }

        fun formatPreviewSnippet(text: String): String {
            if (text.isBlank()) return ""
            return EMOJI_TOKEN_REGEX.replace(text, "✨").trim()
        }

        fun cacheKeyFor(emojiId: String): String = "livtrak_emoji_$emojiId"
    }

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val fetchMutex = Mutex()
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var memoryList: List<CommunityEmoji> = emptyList()

    @Volatile
    private var memoryLoadedAt: Long = 0L

    private val preloadedIds = LinkedHashSet<String>()

    fun readCachedSync(): List<CommunityEmoji> {
        if (memoryList.isNotEmpty()) return applyUsageScores(memoryList)
        val raw = prefs.getString(KEY_META_JSON, null) ?: return emptyList()
        val parsed = runCatching { parseCachedMetaJson(raw) }.getOrDefault(emptyList())
        if (parsed.isNotEmpty()) {
            memoryList = parsed
            memoryLoadedAt = prefs.getLong(KEY_META_UPDATED_AT, 0L)
        }
        return applyUsageScores(parsed)
    }

    fun isCacheFresh(): Boolean {
        val updatedAt = if (memoryLoadedAt > 0L) memoryLoadedAt else prefs.getLong(KEY_META_UPDATED_AT, 0L)
        return updatedAt > 0L && (System.currentTimeMillis() - updatedAt) < META_TTL_MS && memoryList.isNotEmpty()
    }

    suspend fun getEmojis(
        session: AuthSession? = null,
        forceRefresh: Boolean = false
    ): List<CommunityEmoji> = withContext(Dispatchers.IO) {
        val cached = readCachedSync()
        if (!forceRefresh && cached.isNotEmpty() && isCacheFresh()) {
            return@withContext cached
        }
        fetchMutex.withLock {
            if (!forceRefresh && memoryList.isNotEmpty() && isCacheFresh()) {
                return@withLock applyUsageScores(memoryList)
            }
            if (!AppConfig.isSupabaseConfigured) {
                return@withLock cached
            }
            val remote = fetchFromRpc(session) ?: fetchFromStorageBuckets(session)
            if (remote != null && remote.isNotEmpty()) {
                memoryList = remote
                memoryLoadedAt = System.currentTimeMillis()
                persistMetaJson(remote, memoryLoadedAt)
                return@withLock applyUsageScores(remote)
            }
            return@withLock cached
        }
    }

    /**
     * Non-blocking preload of the top N most frequently used emojis into Coil's memory + disk cache.
     * Never blocks chat messages or community screen rendering.
     */
    fun preloadTopEmojis(emojis: List<CommunityEmoji>, count: Int = 8) {
        if (emojis.isEmpty()) return
        val loader = context.applicationContext.imageLoader
        val top = emojis.take(count.coerceAtLeast(1))
        top.forEach { emoji ->
            if (emoji.url.isBlank()) return@forEach
            val shouldEnqueue = synchronized(preloadedIds) {
                preloadedIds.add(emoji.id)
            }
            if (shouldEnqueue) {
                val key = cacheKeyFor(emoji.id)
                val req = ImageRequest.Builder(context.applicationContext)
                    .data(emoji.url)
                    .memoryCacheKey(key)
                    .diskCacheKey(key)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .networkCachePolicy(CachePolicy.ENABLED)
                    .addHeader("Cache-Control", "public, max-age=31536000, immutable")
                    .size(128)
                    .build()
                loader.enqueue(req)
            }
        }
    }

    /**
     * Immediately bumps local usage counts (so dynamic "Top Emojis" updates instantly)
     * and fires a lightweight Supabase RPC `increment_emoji_usage` in the background.
     */
    suspend fun recordUsage(session: AuthSession?, emojiIds: List<String>): List<CommunityEmoji> = withContext(Dispatchers.IO) {
        val cleanIds = emojiIds.filter { it.isNotBlank() }.take(20)
        if (cleanIds.isEmpty()) return@withContext readCachedSync()

        // Update local usage map immediately
        val localMap = readLocalUsageMap().toMutableMap()
        cleanIds.forEach { id ->
            localMap[id] = (localMap[id] ?: 0L) + 1L
        }
        saveLocalUsageMap(localMap)

        // Fire-and-forget remote counter increment
        if (AppConfig.isSupabaseConfigured && session != null) {
            runCatching {
                val payload = JSONObject().put("p_emoji_ids", JSONArray(cleanIds))
                val req = Request.Builder()
                    .url("${AppConfig.supabaseUrl}/rest/v1/rpc/increment_emoji_usage")
                    .addHeader("apikey", AppConfig.supabaseAnonKey)
                    .addHeader("Authorization", "Bearer ${session.accessToken}")
                    .addHeader("Content-Type", "application/json")
                    .post(payload.toString().toRequestBody("application/json".toMediaType()))
                    .build()
                client.newCall(req).execute().close()
            }
        }

        // Bump in-memory server counts optimistically as well
        if (memoryList.isNotEmpty()) {
            val counts = cleanIds.groupingBy { it }.eachCount()
            memoryList = memoryList.map { e ->
                val add = counts[e.id] ?: 0
                if (add > 0) e.copy(usageCount = e.usageCount + add) else e
            }
        }
        applyUsageScores(memoryList)
    }

    private fun fetchFromRpc(session: AuthSession?): List<CommunityEmoji>? {
        val authToken = session?.accessToken ?: AppConfig.supabaseAnonKey
        val req = Request.Builder()
            .url("${AppConfig.supabaseUrl}/rest/v1/rpc/get_community_emojis")
            .addHeader("apikey", AppConfig.supabaseAnonKey)
            .addHeader("Authorization", "Bearer $authToken")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .post("{}".toRequestBody("application/json".toMediaType()))
            .build()
        return runCatching {
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty().trim()
                if (!resp.isSuccessful) return@use null
                val arr = when {
                    body.startsWith("[") -> JSONArray(body)
                    body.startsWith("{") -> JSONObject(body).optJSONArray("emojis")
                    else -> null
                } ?: return@use null
                if (arr.length() == 0) return@use null
                val byId = LinkedHashMap<String, CommunityEmoji>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val id = o.optString("id").trim()
                    val bucket = o.optString("bucket_name", "community_emojis").ifBlank { "community_emojis" }
                    val path = o.optString("storage_path").trim()
                    if (id.isEmpty() || path.isEmpty()) continue
                    val canonicalUrl = buildPublicStorageUrl(bucket, path)
                    byId[id] = CommunityEmoji(
                        id = id,
                        bucketName = bucket,
                        storagePath = path,
                        fileName = o.optString("file_name", path.substringAfterLast('/')),
                        mimeType = o.optString("mime_type", inferMimeType(path)),
                        usageCount = o.optLong("usage_count", 0L),
                        url = canonicalUrl
                    )
                }
                byId.values.toList()
            }
        }.getOrNull()
    }

    private fun fetchFromStorageBuckets(session: AuthSession?): List<CommunityEmoji>? {
        val authToken = session?.accessToken ?: AppConfig.supabaseAnonKey
        val byId = LinkedHashMap<String, CommunityEmoji>()
        for (bucket in SUPPORTED_BUCKETS) {
            runCatching {
                val payload = JSONObject()
                    .put("prefix", "")
                    .put("limit", 250)
                    .put("offset", 0)
                    .put("sortBy", JSONObject().put("column", "name").put("order", "asc"))
                val req = Request.Builder()
                    .url("${AppConfig.supabaseUrl}/storage/v1/object/list/$bucket")
                    .addHeader("apikey", AppConfig.supabaseAnonKey)
                    .addHeader("Authorization", "Bearer $authToken")
                    .addHeader("Content-Type", "application/json")
                    .post(payload.toString().toRequestBody("application/json".toMediaType()))
                    .build()
                client.newCall(req).execute().use { resp ->
                    val body = resp.body?.string().orEmpty().trim()
                    if (!resp.isSuccessful || !body.startsWith("[")) return@use
                    val arr = JSONArray(body)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val name = obj.optString("name").trim()
                        if (name.isEmpty() || name.endsWith("/") || name.startsWith(".")) continue
                        if (!isSupportedImageFile(name)) continue
                        val id = slugFromFileName(name)
                        if (id.isBlank() || byId.containsKey(id)) continue
                        val meta = obj.optJSONObject("metadata")
                        val mime = meta?.optString("mimetype")?.takeIf { it.isNotBlank() } ?: inferMimeType(name)
                        byId[id] = CommunityEmoji(
                            id = id,
                            bucketName = bucket,
                            storagePath = name,
                            fileName = name.substringAfterLast('/'),
                            mimeType = mime,
                            usageCount = 0L,
                            url = buildPublicStorageUrl(bucket, name)
                        )
                    }
                }
            }
        }
        return byId.values.toList().takeIf { it.isNotEmpty() }
    }

    private fun buildPublicStorageUrl(bucket: String, path: String): String {
        val base = AppConfig.supabaseUrl.trimEnd('/')
        val encodedPath = path.split('/').joinToString("/") { seg ->
            URLEncoder.encode(seg, "UTF-8").replace("+", "%20")
        }
        return "$base/storage/v1/object/public/$bucket/$encodedPath"
    }

    private fun isSupportedImageFile(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(".gif") || lower.endsWith(".webp") || lower.endsWith(".png") ||
            lower.endsWith(".apng") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")
    }

    private fun inferMimeType(path: String): String {
        val lower = path.lowercase()
        return when {
            lower.endsWith(".gif") -> "image/gif"
            lower.endsWith(".webp") -> "image/webp"
            lower.endsWith(".png") || lower.endsWith(".apng") -> "image/png"
            else -> "image/jpeg"
        }
    }

    private fun slugFromFileName(path: String): String {
        val base = path.substringAfterLast('/').substringBeforeLast('.')
        return base.lowercase()
            .replace(Regex("[^a-z0-9_-]+"), "_")
            .trim('_')
            .take(64)
    }

    private fun applyUsageScores(list: List<CommunityEmoji>): List<CommunityEmoji> {
        if (list.isEmpty()) return emptyList()
        val localMap = readLocalUsageMap()
        return list.map { emoji ->
            val localBoost = (localMap[emoji.id] ?: 0L) * 2L
            emoji.copy(score = emoji.usageCount + localBoost)
        }.sortedWith(
            compareByDescending<CommunityEmoji> { it.score }
                .thenBy { it.id }
        )
    }

    private fun persistMetaJson(list: List<CommunityEmoji>, timestamp: Long) {
        val arr = JSONArray()
        list.forEach { e ->
            arr.put(
                JSONObject()
                    .put("id", e.id)
                    .put("bucket_name", e.bucketName)
                    .put("storage_path", e.storagePath)
                    .put("file_name", e.fileName)
                    .put("mime_type", e.mimeType)
                    .put("usage_count", e.usageCount)
                    .put("url", e.url)
            )
        }
        prefs.edit()
            .putString(KEY_META_JSON, arr.toString())
            .putLong(KEY_META_UPDATED_AT, timestamp)
            .apply()
    }

    private fun parseCachedMetaJson(raw: String): List<CommunityEmoji> {
        val arr = JSONArray(raw)
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val id = o.optString("id")
                val url = o.optString("url")
                if (id.isNotBlank() && url.isNotBlank()) {
                    add(
                        CommunityEmoji(
                            id = id,
                            bucketName = o.optString("bucket_name", "community_emojis"),
                            storagePath = o.optString("storage_path"),
                            fileName = o.optString("file_name"),
                            mimeType = o.optString("mime_type", "image/gif"),
                            usageCount = o.optLong("usage_count", 0L),
                            url = url
                        )
                    )
                }
            }
        }
    }

    private fun readLocalUsageMap(): Map<String, Long> {
        val raw = prefs.getString(KEY_LOCAL_USAGE_JSON, null) ?: return emptyMap()
        return runCatching {
            val obj = JSONObject(raw)
            buildMap {
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    put(k, obj.optLong(k, 0L))
                }
            }
        }.getOrDefault(emptyMap())
    }

    private fun saveLocalUsageMap(map: Map<String, Long>) {
        val obj = JSONObject()
        map.forEach { (k, v) -> obj.put(k, v) }
        prefs.edit().putString(KEY_LOCAL_USAGE_JSON, obj.toString()).apply()
    }
}
