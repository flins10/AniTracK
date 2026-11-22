package com.example.desktop

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jetbrains.skia.Image as SkiaImage
import java.io.File
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

data class DesktopCommunityEmoji(
    val id: String,
    val bucketName: String = "community_emojis",
    val storagePath: String = "",
    val fileName: String = "",
    val mimeType: String = "image/gif",
    val usageCount: Long = 0L,
    val url: String = "",
    val score: Long = usageCount
)

internal object DesktopEmojiCacheService {
    private const val META_TTL_MS = 15L * 60L * 1000L
    private val SUPPORTED_BUCKETS = listOf("community_emojis", "community-emojis")
    private val jsonMedia = "application/json".toMediaType()
    val EMOJI_TOKEN_REGEX = Regex("""\[emoji:([a-zA-Z0-9_-]{1,64})]""")

    private val cacheRoot = File(System.getProperty("user.home"), ".anime_universe")
    private val metaFile = File(cacheRoot, "emoji_meta_v1.json")
    private val usageFile = File(cacheRoot, "emoji_usage_v1.json")
    private val imgCacheDir = File(cacheRoot, "emoji_cache")

    private val fetchMutex = Mutex()
    private val bgScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var memoryList: List<DesktopCommunityEmoji> = emptyList()

    @Volatile
    private var memoryLoadedAt: Long = 0L

    private val bitmapCache = ConcurrentHashMap<String, ImageBitmap>()
    private val inflightDownloads = ConcurrentHashMap<String, Deferred<ImageBitmap?>>()

    fun extractEmojiIds(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        return EMOJI_TOKEN_REGEX.findAll(text).map { it.groupValues[1] }.toList()
    }

    fun formatPreviewSnippet(text: String): String {
        if (text.isBlank()) return ""
        return EMOJI_TOKEN_REGEX.replace(text, "✨").trim()
    }

    fun getCachedBitmap(emojiId: String): ImageBitmap? = bitmapCache[emojiId]

    fun readCachedMetaSync(): List<DesktopCommunityEmoji> {
        if (memoryList.isNotEmpty()) return applyUsageScores(memoryList)
        if (!metaFile.exists()) return emptyList()
        val parsed = runCatching {
            val root = JsonParser.parseString(metaFile.readText()).asJsonObject
            memoryLoadedAt = root.get("updated_at")?.asLong ?: 0L
            val arr = root.getAsJsonArray("emojis") ?: JsonArray()
            arr.mapNotNull { el ->
                val o = el.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
                val id = o.get("id")?.asString.orEmpty()
                val url = o.get("url")?.asString.orEmpty()
                if (id.isBlank() || url.isBlank()) null else DesktopCommunityEmoji(
                    id = id,
                    bucketName = o.get("bucket_name")?.asString ?: "community_emojis",
                    storagePath = o.get("storage_path")?.asString.orEmpty(),
                    fileName = o.get("file_name")?.asString.orEmpty(),
                    mimeType = o.get("mime_type")?.asString ?: "image/gif",
                    usageCount = o.get("usage_count")?.asLong ?: 0L,
                    url = url
                )
            }
        }.getOrDefault(emptyList())
        if (parsed.isNotEmpty()) memoryList = parsed
        return applyUsageScores(parsed)
    }

    suspend fun getEmojis(
        session: DesktopPersistedState? = null,
        forceRefresh: Boolean = false
    ): List<DesktopCommunityEmoji> = withContext(Dispatchers.IO) {
        val cached = readCachedMetaSync()
        val fresh = memoryLoadedAt > 0L && (System.currentTimeMillis() - memoryLoadedAt) < META_TTL_MS && memoryList.isNotEmpty()
        if (!forceRefresh && cached.isNotEmpty() && fresh) {
            return@withContext cached
        }
        fetchMutex.withLock {
            val nowFresh = memoryLoadedAt > 0L && (System.currentTimeMillis() - memoryLoadedAt) < META_TTL_MS && memoryList.isNotEmpty()
            if (!forceRefresh && nowFresh) return@withLock applyUsageScores(memoryList)

            val remote = fetchFromRpc(session) ?: fetchFromStorageBuckets(session)
            if (remote != null && remote.isNotEmpty()) {
                memoryList = remote
                memoryLoadedAt = System.currentTimeMillis()
                persistMeta(remote, memoryLoadedAt)
                return@withLock applyUsageScores(remote)
            }
            return@withLock cached
        }
    }

    fun preloadTopEmojis(emojis: List<DesktopCommunityEmoji>, count: Int = 8) {
        if (emojis.isEmpty()) return
        emojis.take(count.coerceAtLeast(1)).forEach { emoji ->
            bgScope.launch { loadEmojiBitmap(emoji) }
        }
    }

    suspend fun loadEmojiBitmap(emoji: DesktopCommunityEmoji): ImageBitmap? {
        bitmapCache[emoji.id]?.let { return it }
        val existing = inflightDownloads[emoji.id]
        if (existing != null) return existing.await()

        val deferred = bgScope.async {
            runCatching {
                if (!imgCacheDir.exists()) imgCacheDir.mkdirs()
                val safeName = emoji.id.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                val diskFile = File(imgCacheDir, "$safeName.bin")
                val bytes = if (diskFile.exists() && diskFile.length() > 0L) {
                    diskFile.readBytes()
                } else {
                    val req = Request.Builder()
                        .url(emoji.url)
                        .addHeader("Cache-Control", "public, max-age=31536000, immutable")
                        .get()
                        .build()
                    httpClient.newCall(req).execute().use { resp ->
                        if (!resp.isSuccessful) return@use null
                        val bodyBytes = resp.body?.bytes() ?: return@use null
                        if (bodyBytes.isNotEmpty()) {
                            runCatching { diskFile.writeBytes(bodyBytes) }
                        }
                        bodyBytes
                    }
                } ?: return@runCatching null

                val bmp = SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
                bitmapCache[emoji.id] = bmp
                bmp
            }.getOrNull()
        }

        val prev = inflightDownloads.putIfAbsent(emoji.id, deferred)
        val active = prev ?: deferred
        return try {
            active.await()
        } finally {
            if (prev == null) inflightDownloads.remove(emoji.id, deferred)
        }
    }

    suspend fun recordUsage(session: DesktopPersistedState?, emojiIds: List<String>): List<DesktopCommunityEmoji> =
        withContext(Dispatchers.IO) {
            val cleanIds = emojiIds.filter { it.isNotBlank() }.take(20)
            if (cleanIds.isEmpty()) return@withContext readCachedMetaSync()

            val local = readLocalUsage().toMutableMap()
            cleanIds.forEach { id -> local[id] = (local[id] ?: 0L) + 1L }
            persistLocalUsage(local)

            if (session != null && !session.isGuest && session.accessToken.isNotBlank()) {
                bgScope.launch {
                    runCatching {
                        val payload = JsonObject().apply {
                            val arr = JsonArray()
                            cleanIds.forEach { arr.add(it) }
                            add("p_emoji_ids", arr)
                        }
                        val req = Request.Builder()
                            .url("$SUPABASE_URL/rest/v1/rpc/increment_emoji_usage")
                            .addHeader("apikey", SUPABASE_ANON_KEY)
                            .addHeader("Authorization", "Bearer ${session.accessToken}")
                            .addHeader("Content-Type", "application/json")
                            .post(gson.toJson(payload).toRequestBody(jsonMedia))
                            .build()
                        httpClient.newCall(req).execute().close()
                    }
                }
            }

            if (memoryList.isNotEmpty()) {
                val counts = cleanIds.groupingBy { it }.eachCount()
                memoryList = memoryList.map { e ->
                    val add = counts[e.id] ?: 0
                    if (add > 0) e.copy(usageCount = e.usageCount + add) else e
                }
            }
            applyUsageScores(memoryList)
        }

    private fun fetchFromRpc(session: DesktopPersistedState?): List<DesktopCommunityEmoji>? {
        val token = session?.accessToken?.takeIf { !session.isGuest && it.isNotBlank() } ?: SUPABASE_ANON_KEY
        val req = Request.Builder()
            .url("$SUPABASE_URL/rest/v1/rpc/get_community_emojis")
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
            .post("{}".toRequestBody(jsonMedia))
            .build()
        return runCatching {
            httpClient.newCall(req).execute().use { resp ->
                val raw = resp.body?.string().orEmpty().trim()
                if (!resp.isSuccessful || !raw.startsWith("[")) return@use null
                val arr = JsonParser.parseString(raw).asJsonArray
                if (arr.size() == 0) return@use null
                val byId = LinkedHashMap<String, DesktopCommunityEmoji>()
                arr.forEach { el ->
                    val o = el.takeIf { it.isJsonObject }?.asJsonObject ?: return@forEach
                    val id = o.get("id")?.asString?.trim().orEmpty()
                    val bucket = o.get("bucket_name")?.asString?.ifBlank { "community_emojis" } ?: "community_emojis"
                    val path = o.get("storage_path")?.asString?.trim().orEmpty()
                    if (id.isEmpty() || path.isEmpty()) return@forEach
                    byId[id] = DesktopCommunityEmoji(
                        id = id,
                        bucketName = bucket,
                        storagePath = path,
                        fileName = o.get("file_name")?.asString ?: path.substringAfterLast('/'),
                        mimeType = o.get("mime_type")?.asString ?: "image/gif",
                        usageCount = o.get("usage_count")?.asLong ?: 0L,
                        url = buildPublicUrl(bucket, path)
                    )
                }
                byId.values.toList()
            }
        }.getOrNull()
    }

    private fun fetchFromStorageBuckets(session: DesktopPersistedState?): List<DesktopCommunityEmoji>? {
        val token = session?.accessToken?.takeIf { !session.isGuest && it.isNotBlank() } ?: SUPABASE_ANON_KEY
        val byId = LinkedHashMap<String, DesktopCommunityEmoji>()
        for (bucket in SUPPORTED_BUCKETS) {
            runCatching {
                val body = JsonObject().apply {
                    addProperty("prefix", "")
                    addProperty("limit", 250)
                    addProperty("offset", 0)
                    add("sortBy", JsonObject().apply {
                        addProperty("column", "name")
                        addProperty("order", "asc")
                    })
                }
                val req = Request.Builder()
                    .url("$SUPABASE_URL/storage/v1/object/list/$bucket")
                    .addHeader("apikey", SUPABASE_ANON_KEY)
                    .addHeader("Authorization", "Bearer $token")
                    .addHeader("Content-Type", "application/json")
                    .post(gson.toJson(body).toRequestBody(jsonMedia))
                    .build()
                httpClient.newCall(req).execute().use { resp ->
                    val raw = resp.body?.string().orEmpty().trim()
                    if (!resp.isSuccessful || !raw.startsWith("[")) return@use
                    val arr = JsonParser.parseString(raw).asJsonArray
                    arr.forEach { el ->
                        val o = el.takeIf { it.isJsonObject }?.asJsonObject ?: return@forEach
                        val name = o.get("name")?.asString?.trim().orEmpty()
                        if (name.isEmpty() || name.endsWith("/") || name.startsWith(".")) return@forEach
                        val lower = name.lowercase()
                        if (!(lower.endsWith(".gif") || lower.endsWith(".webp") || lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg"))) {
                            return@forEach
                        }
                        val id = name.substringAfterLast('/').substringBeforeLast('.')
                            .lowercase().replace(Regex("[^a-z0-9_-]+"), "_").trim('_').take(64)
                        if (id.isBlank() || byId.containsKey(id)) return@forEach
                        byId[id] = DesktopCommunityEmoji(
                            id = id,
                            bucketName = bucket,
                            storagePath = name,
                            fileName = name.substringAfterLast('/'),
                            mimeType = if (lower.endsWith(".gif")) "image/gif" else "image/png",
                            usageCount = 0L,
                            url = buildPublicUrl(bucket, name)
                        )
                    }
                }
            }
        }
        return byId.values.toList().takeIf { it.isNotEmpty() }
    }

    private fun buildPublicUrl(bucket: String, path: String): String {
        val base = SUPABASE_URL.trimEnd('/')
        val encoded = path.split('/').joinToString("/") { seg ->
            URLEncoder.encode(seg, "UTF-8").replace("+", "%20")
        }
        return "$base/storage/v1/object/public/$bucket/$encoded"
    }

    private fun applyUsageScores(list: List<DesktopCommunityEmoji>): List<DesktopCommunityEmoji> {
        if (list.isEmpty()) return emptyList()
        val local = readLocalUsage()
        return list.map { e ->
            e.copy(score = e.usageCount + (local[e.id] ?: 0L) * 2L)
        }.sortedWith(compareByDescending<DesktopCommunityEmoji> { it.score }.thenBy { it.id })
    }

    private fun persistMeta(list: List<DesktopCommunityEmoji>, updatedAt: Long) {
        runCatching {
            if (!cacheRoot.exists()) cacheRoot.mkdirs()
            val arr = JsonArray()
            list.forEach { e ->
                arr.add(JsonObject().apply {
                    addProperty("id", e.id)
                    addProperty("bucket_name", e.bucketName)
                    addProperty("storage_path", e.storagePath)
                    addProperty("file_name", e.fileName)
                    addProperty("mime_type", e.mimeType)
                    addProperty("usage_count", e.usageCount)
                    addProperty("url", e.url)
                })
            }
            val root = JsonObject().apply {
                addProperty("updated_at", updatedAt)
                add("emojis", arr)
            }
            metaFile.writeText(gson.toJson(root))
        }
    }

    private fun readLocalUsage(): Map<String, Long> {
        if (!usageFile.exists()) return emptyMap()
        return runCatching {
            val obj = JsonParser.parseString(usageFile.readText()).asJsonObject
            buildMap {
                obj.entrySet().forEach { (k, v) -> put(k, runCatching { v.asLong }.getOrDefault(0L)) }
            }
        }.getOrDefault(emptyMap())
    }

    private fun persistLocalUsage(map: Map<String, Long>) {
        runCatching {
            if (!cacheRoot.exists()) cacheRoot.mkdirs()
            val obj = JsonObject()
            map.forEach { (k, v) -> obj.addProperty(k, v) }
            usageFile.writeText(gson.toJson(obj))
        }
    }
}

@Composable
internal fun DesktopCachedEmojiImage(
    emoji: DesktopCommunityEmoji,
    size: Dp,
    modifier: Modifier = Modifier
) {
    var bitmap by remember(emoji.id) { mutableStateOf(DesktopEmojiCacheService.getCachedBitmap(emoji.id)) }
    var failed by remember(emoji.id) { mutableStateOf(false) }

    LaunchedEffect(emoji.id, emoji.url) {
        if (bitmap == null) {
            val loaded = DesktopEmojiCacheService.loadEmojiBitmap(emoji)
            if (loaded != null) {
                bitmap = loaded
            } else {
                failed = true
            }
        }
    }

    val currentBmp = bitmap
    if (currentBmp != null) {
        Image(
            bitmap = currentBmp,
            contentDescription = emoji.id,
            contentScale = ContentScale.Crop,
            modifier = modifier.size(size).clip(CircleShape)
        )
    } else if (failed) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier.size(size).clip(CircleShape).background(Color(0xFF14141E))
        ) {
            Text(emoji.id.take(2).uppercase(), color = Color(0xFF66667A), fontSize = 8.sp)
        }
    } else {
        val transition = rememberInfiniteTransition()
        val alpha by transition.animateFloat(
            initialValue = 0.25f,
            targetValue = 0.65f,
            animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse)
        )
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(ChampagneGold.copy(alpha = alpha * 0.28f))
        )
    }
}

@Composable
internal fun DesktopRichChatText(
    content: String,
    emojiById: Map<String, DesktopCommunityEmoji>,
    mentionColor: Color = ChampagneGold,
    textColor: Color = IvoryText,
    fontSizeSp: Float = 13.5f
) {
    val combinedRegex = remember { Regex("""\[emoji:([a-zA-Z0-9_-]{1,64})]|(@[A-Za-z0-9_.-]{2,32})""") }
    val inlineMap = remember(content, emojiById) {
        val map = LinkedHashMap<String, InlineTextContent>()
        DesktopEmojiCacheService.extractEmojiIds(content).distinct().forEach { eid ->
            val emoji = emojiById[eid]
            if (emoji != null) {
                map["emoji_$eid"] = InlineTextContent(
                    placeholder = Placeholder(28.sp, 28.sp, PlaceholderVerticalAlign.Center)
                ) {
                    DesktopCachedEmojiImage(emoji = emoji, size = 26.dp)
                }
            }
        }
        map
    }

    val annotated = remember(content, emojiById, mentionColor, textColor) {
        buildAnnotatedString {
            var cursor = 0
            combinedRegex.findAll(content).forEach { match ->
                if (match.range.first > cursor) {
                    withStyle(SpanStyle(color = textColor)) {
                        append(content.substring(cursor, match.range.first))
                    }
                }
                val emojiId = match.groups[1]?.value
                val mention = match.groups[2]?.value
                when {
                    emojiId != null && emojiById.containsKey(emojiId) -> {
                        appendInlineContent("emoji_$emojiId", "✨")
                    }
                    emojiId != null -> {
                        withStyle(SpanStyle(color = WarmSilver, fontSize = 11.sp)) {
                            append(":$emojiId:")
                        }
                    }
                    mention != null -> {
                        withStyle(SpanStyle(color = mentionColor, fontWeight = FontWeight.SemiBold)) {
                            append(mention)
                        }
                    }
                }
                cursor = match.range.last + 1
            }
            if (cursor < content.length) {
                withStyle(SpanStyle(color = textColor)) {
                    append(content.substring(cursor))
                }
            }
        }
    }

    Text(
        text = annotated,
        inlineContent = inlineMap,
        fontSize = fontSizeSp.sp,
        lineHeight = (fontSizeSp + 6f).sp
    )
}

@Composable
internal fun DesktopEmojiTray(
    emojis: List<DesktopCommunityEmoji>,
    onSelectEmoji: (DesktopCommunityEmoji) -> Unit
) {
    val topEmojis = remember(emojis) { emojis.take(8) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0E0E16))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (emojis.isEmpty()) {
            Text(
                "Upload emojis to your Supabase \"community_emojis\" or \"community-emojis\" bucket to see them here.",
                color = WarmSilver,
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "TOP EMOJIS",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.6.sp,
                    color = ChampagneGold
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(topEmojis, key = { "top_${it.id}" }) { emoji ->
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF14141E),
                            border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.25f)),
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .clickable { onSelectEmoji(emoji) }
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(4.dp)) {
                                DesktopCachedEmojiImage(emoji = emoji, size = 34.dp)
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = Color(0x14FFFFFF))

            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().height(168.dp)
            ) {
                items(emojis, key = { "grid_${it.id}" }) { emoji ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF14141E))
                            .clickable { onSelectEmoji(emoji) }
                    ) {
                        DesktopCachedEmojiImage(emoji = emoji, size = 34.dp)
                    }
                }
            }
        }
    }
}
