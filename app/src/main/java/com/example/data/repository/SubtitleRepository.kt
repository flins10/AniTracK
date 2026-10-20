package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.util.LruCache
import com.example.data.remote.ArchiveFileItem
import com.example.data.remote.InternetArchiveApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.tukaani.xz.XZInputStream
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream
import java.util.zip.ZipInputStream

data class SubtitleTrackItem(
    val id: String,
    val title: String,
    val languageCode: String,
    val languageName: String,
    val source: SubtitleSource,
    val remoteUrl: String? = null,
    val localFileUri: String? = null,
    val format: String = "srt", // srt, vtt, ass
    val isEmbedded: Boolean = false,
    val trackGroupIndex: Int? = null,
    val trackIndex: Int? = null,
    val matchedEpisode: Int? = null
)

enum class SubtitleSource {
    INTERNET_ARCHIVE,
    OPENSUBTITLES,
    EXTERNAL_SUBTITLE_INDEX,
    EXTERNAL_SUBTITLE_FEED,
    LOCAL_DEVICE,
    CUSTOM_URL,
    EMBEDDED
}

data class SubtitleStyleConfig(
    val fontSizeSp: Float = 18f,
    val textColor: Long = 0xFFFFFFFF, // White
    val backgroundColor: Long = 0x88000000, // Semi-transparent black
    val offsetMs: Long = 0L // Timing offset in ms (-2000ms to +2000ms)
)

class SubtitleRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) {
    companion object {
        private val directoryHtmlCache = LruCache<String, String>(12)
        private val searchResultsCache = LruCache<String, List<SubtitleTrackItem>>(60)
    }

    /**
     * Extracts all verified subtitle tracks associated with the media item and files.
     * Does not expose any provider names in user-facing labels.
     */
    fun extractArchiveSubtitles(
        identifier: String,
        files: List<ArchiveFileItem>,
        requestedEpisode: Int?
    ): List<SubtitleTrackItem> {
        val result = mutableListOf<SubtitleTrackItem>()

        val subtitleFiles = files.filter { file ->
            val name = file.name.lowercase(Locale.ROOT)
            val fmt = file.format?.lowercase(Locale.ROOT).orEmpty()
            name.endsWith(".srt") || name.endsWith(".vtt") || name.endsWith(".ass") || name.endsWith(".ssa") ||
                    fmt.contains("subrip") || fmt.contains("vtt") || fmt.contains("subtitle") || fmt.contains("substation")
        }

        for (file in subtitleFiles) {
            val name = file.name
            val epNum = detectSubtitleEpisode(name)

            if (requestedEpisode != null && epNum != null && epNum != requestedEpisode) {
                continue
            }

            val langInfo = detectLanguageFromText(name + " " + (file.title.orEmpty()))
            val ext = when {
                name.endsWith(".vtt", ignoreCase = true) -> "vtt"
                name.endsWith(".ass", ignoreCase = true) || name.endsWith(".ssa", ignoreCase = true) -> "ass"
                else -> "srt"
            }

            val downloadUrl = InternetArchiveApiService.getDirectDownloadUrl(identifier, name)

            result.add(
                SubtitleTrackItem(
                    id = "sub_item_${identifier}_${name}",
                    title = cleanSubtitleDisplayTitle(file.title?.takeIf { it.isNotBlank() } ?: name, langInfo.second, epNum),
                    languageCode = langInfo.first,
                    languageName = langInfo.second,
                    source = SubtitleSource.INTERNET_ARCHIVE,
                    remoteUrl = downloadUrl,
                    format = ext,
                    matchedEpisode = epNum
                )
            )
        }

        return result
    }

    /**
     * Searches subtitle repositories (AnimeTosho, Kitsunekko, OpenSubtitles) concurrently
     * for exact episode subtitle files (.ass, .srt, .vtt, .xz, .zip) without mentioning provider names anywhere.
     */
    suspend fun searchExternalSubtitles(
        animeTitle: String,
        episodeNumber: Int?,
        seasonNumber: Int? = 1,
        year: Int? = null,
        languages: List<String> = listOf("en", "ja", "zh", "hi"),
        alternateTitles: List<String> = emptyList()
    ): List<SubtitleTrackItem> = withContext(Dispatchers.IO) {
        val allTitles = (listOf(animeTitle) + alternateTitles)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        if (allTitles.isEmpty()) return@withContext emptyList()

        val cacheKey = "${allTitles.first().lowercase(Locale.ROOT)}_ep_${episodeNumber ?: 0}"
        searchResultsCache.get(cacheKey)?.let { cached ->
            if (cached.isNotEmpty()) return@withContext cached
        }

        val combined = coroutineScope {
            val toshoDeferred = async {
                searchToshoSubtitles(allTitles, episodeNumber)
            }
            val kitsuDeferred = async {
                searchKitsunekkoSubtitles(allTitles, episodeNumber)
            }
            val openSubsDeferred = async {
                searchOpenSubtitles(allTitles.first(), episodeNumber, seasonNumber, year, languages)
            }
            awaitAll(toshoDeferred, kitsuDeferred, openSubsDeferred).flatten()
        }

        val deduplicated = combined
            .distinctBy { it.remoteUrl ?: it.id }
            .sortedWith(
                compareBy<SubtitleTrackItem> {
                    when (it.languageCode) {
                        "en" -> 0
                        "ja" -> 1
                        else -> 2
                    }
                }.thenBy {
                    if (it.matchedEpisode == episodeNumber) 0 else 1
                }
            )

        if (deduplicated.isNotEmpty()) {
            searchResultsCache.put(cacheKey, deduplicated)
        }
        deduplicated
    }

    /**
     * Queries feed.animetosho.org JSON API for matching anime episode releases and extracts direct subtitle tracks.
     */
    private fun searchToshoSubtitles(
        titles: List<String>,
        episodeNumber: Int?
    ): List<SubtitleTrackItem> {
        val results = mutableListOf<SubtitleTrackItem>()
        val epPadded = if (episodeNumber != null && episodeNumber > 0) {
            String.format(Locale.ROOT, "%02d", episodeNumber)
        } else null

        val matchService = VideoMatchService()

        for (rawTitle in titles.take(3)) {
            val cleanTitle = cleanSearchTitle(rawTitle)
            if (cleanTitle.isBlank()) continue

            val queryStr = if (epPadded != null) "$cleanTitle $epPadded" else cleanTitle
            val searchUrl = "https://feed.animetosho.org/json?t=search&q=${Uri.encode(queryStr)}"

            try {
                val body = fetchString(searchUrl) ?: continue
                val entries = JSONArray(body)
                val matchingEntryIds = mutableListOf<Pair<Int, String>>()

                for (i in 0 until minOf(entries.length(), 20)) {
                    val obj = entries.optJSONObject(i) ?: continue
                    val entryId = obj.optInt("id", 0)
                    val releaseTitle = obj.optString("title", "")
                    if (entryId <= 0 || releaseTitle.isBlank()) continue

                    if (matchService.containsExcludedKeywords(releaseTitle, "", "")) continue

                    val sim = matchService.calculateStrictTitleSimilarity(releaseTitle, "", titles)
                    if (sim < 0.60f) continue

                    val detectedEp = detectSubtitleEpisode(releaseTitle) ?: matchService.detectEpisodeNumber(releaseTitle)
                    if (episodeNumber != null && episodeNumber > 0 && detectedEp != episodeNumber) {
                        continue
                    }

                    val numSubs = obj.optInt("num_subtitles", 1)
                    if (numSubs > 0) {
                        matchingEntryIds.add(entryId to releaseTitle)
                    }
                    if (matchingEntryIds.size >= 3) break
                }

                for ((entryId, releaseTitle) in matchingEntryIds) {
                    val detailUrl = "https://feed.animetosho.org/json?show=torrent&id=$entryId"
                    val detailBody = fetchString(detailUrl) ?: continue
                    val detailJson = JSONObject(detailBody)

                    // Check "subtitles" or "files" -> "subtitles" in torrent detail response
                    val subArrays = mutableListOf<JSONArray>()
                    detailJson.optJSONArray("subtitles")?.let { subArrays.add(it) }
                    detailJson.optJSONArray("attachments")?.let { subArrays.add(it) }
                    detailJson.optJSONArray("files")?.let { filesArr ->
                        for (fIdx in 0 until filesArr.length()) {
                            filesArr.optJSONObject(fIdx)?.optJSONArray("subtitles")?.let { subArrays.add(it) }
                        }
                    }

                    for (subArr in subArrays) {
                        for (sIdx in 0 until subArr.length()) {
                            val subObj = subArr.optJSONObject(sIdx) ?: continue
                            val subUrl = subObj.optString("url").takeIf { it.isNotBlank() }
                                ?: subObj.optString("link").takeIf { it.isNotBlank() }
                                ?: subObj.optInt("id", 0).takeIf { it > 0 }?.let { subId ->
                                    "https://animetosho.org/storage/attach/${ String.format(Locale.ROOT, "%08x", subId) }/attachment.ass.xz"
                                }
                                ?: continue

                            val langRaw = subObj.optString("lang").ifBlank {
                                subObj.optString("language").ifBlank {
                                    subObj.optString("name", "eng")
                                }
                            }
                            val langPair = detectLanguageFromText(langRaw)
                            val formatStr = subObj.optString("format", "ass").lowercase(Locale.ROOT).let {
                                if (it.contains("srt") || it.contains("subrip")) "srt"
                                else if (it.contains("vtt")) "vtt"
                                else "ass"
                            }

                            val trackLabel = buildCleanSubtitleLabel(
                                langName = langPair.second,
                                format = formatStr,
                                index = results.count { it.languageCode == langPair.first } + 1
                            )

                            results.add(
                                SubtitleTrackItem(
                                    id = "ext_feed_${entryId}_$sIdx",
                                    title = cleanSubtitleDisplayTitle(releaseTitle, langPair.second, episodeNumber),
                                    languageCode = langPair.first,
                                    languageName = trackLabel,
                                    source = SubtitleSource.EXTERNAL_SUBTITLE_FEED,
                                    remoteUrl = subUrl,
                                    format = formatStr,
                                    matchedEpisode = episodeNumber
                                )
                            )
                        }
                    }
                }

                if (results.isNotEmpty()) break
            } catch (_: Exception) {
                // Continue to next title or source
            }
        }

        return results
    }

    /**
     * Queries kitsunekko.net subtitle directories (English and Japanese) for matching anime and episode subtitle files.
     */
    private fun searchKitsunekkoSubtitles(
        titles: List<String>,
        episodeNumber: Int?
    ): List<SubtitleTrackItem> {
        val results = mutableListOf<SubtitleTrackItem>()
        val matchService = VideoMatchService()

        val directories = listOf(
            "subtitles%2Fenglish%2F" to ("en" to "English"),
            "subtitles%2Fjapanese%2F" to ("ja" to "Japanese (日本語)")
        )

        val dirLinkRegex = Regex("""href=["']/?dirlist\.php\?dir=([^"']+)["'][^>]*>(?:<strong>)?([^<]+)(?:</strong>)?</a>""", RegexOption.IGNORE_CASE)
        val fileLinkRegex = Regex("""href=["']((?:/)?subtitles/[^"']+\.(?:srt|ass|ssa|vtt|zip))["'][^>]*>([^<]*)</a>""", RegexOption.IGNORE_CASE)

        for ((dirParam, langDefault) in directories) {
            try {
                val indexUrl = "https://kitsunekko.net/dirlist.php?dir=$dirParam"
                val indexHtml = directoryHtmlCache.get(indexUrl) ?: fetchString(indexUrl)?.also {
                    directoryHtmlCache.put(indexUrl, it)
                } ?: continue

                var bestDirSlug: String? = null
                var bestDirScore = 0f

                for (match in dirLinkRegex.findAll(indexHtml)) {
                    val slug = match.groupValues[1]
                    val folderName = match.groupValues[2].trim()
                    val sim = matchService.calculateStrictTitleSimilarity(folderName, slug, titles)
                    if (sim > bestDirScore && sim >= 0.65f) {
                        bestDirScore = sim
                        bestDirSlug = slug
                    }
                }

                if (bestDirSlug == null) continue

                val folderUrl = "https://kitsunekko.net/dirlist.php?dir=$bestDirSlug"
                val folderHtml = directoryHtmlCache.get(folderUrl) ?: fetchString(folderUrl)?.also {
                    directoryHtmlCache.put(folderUrl, it)
                } ?: continue

                val candidateFiles = mutableListOf<Triple<String, String, Int?>>()
                for (fMatch in fileLinkRegex.findAll(folderHtml)) {
                    val relativeHref = fMatch.groupValues[1].trimStart('/')
                    val fileLabel = fMatch.groupValues[2].trim().ifBlank { relativeHref.substringAfterLast('/') }
                    val detectedEp = detectSubtitleEpisode(fileLabel) ?: matchService.detectEpisodeNumber(fileLabel)
                    candidateFiles.add(Triple(relativeHref, fileLabel, detectedEp))
                }

                val filteredFiles = if (episodeNumber != null && episodeNumber > 0) {
                    val exactEp = candidateFiles.filter { it.third == episodeNumber && !it.first.endsWith(".zip", ignoreCase = true) }
                    if (exactEp.isNotEmpty()) {
                        exactEp
                    } else {
                        // Also allow .zip archive packs if they contain episodes or match the season
                        candidateFiles.filter { it.third == episodeNumber || it.first.endsWith(".zip", ignoreCase = true) }.take(2)
                    }
                } else {
                    candidateFiles.take(4)
                }

                for ((index, triple) in filteredFiles.take(4).withIndex()) {
                    val (relativeHref, fileLabel, detectedEp) = triple
                    val encodedPath = relativeHref.split("/").joinToString("/") { segment ->
                        Uri.encode(segment)
                    }
                    val fullUrl = "https://kitsunekko.net/$encodedPath"
                    val ext = when {
                        relativeHref.endsWith(".vtt", ignoreCase = true) -> "vtt"
                        relativeHref.endsWith(".ass", ignoreCase = true) || relativeHref.endsWith(".ssa", ignoreCase = true) -> "ass"
                        else -> "srt"
                    }

                    val trackLabel = buildCleanSubtitleLabel(
                        langName = langDefault.second,
                        format = ext,
                        index = index + 1
                    )

                    results.add(
                        SubtitleTrackItem(
                            id = "ext_idx_${langDefault.first}_${relativeHref.hashCode()}",
                            title = cleanSubtitleDisplayTitle(fileLabel, langDefault.second, detectedEp ?: episodeNumber),
                            languageCode = langDefault.first,
                            languageName = trackLabel,
                            source = SubtitleSource.EXTERNAL_SUBTITLE_INDEX,
                            remoteUrl = fullUrl,
                            format = ext,
                            matchedEpisode = detectedEp ?: episodeNumber
                        )
                    )
                }
            } catch (_: Exception) {
                // Continue gracefully
            }
        }

        return results
    }

    private fun searchOpenSubtitles(
        animeTitle: String,
        episodeNumber: Int?,
        seasonNumber: Int?,
        year: Int?,
        languages: List<String>
    ): List<SubtitleTrackItem> {
        val cleanTitle = cleanSearchTitle(animeTitle)
        if (cleanTitle.isBlank()) return emptyList()

        val results = mutableListOf<SubtitleTrackItem>()
        try {
            val langParam = languages.joinToString(",")
            val epQuery = if (episodeNumber != null && episodeNumber > 0) "&episode_number=$episodeNumber" else ""
            val seasonQuery = if (seasonNumber != null && seasonNumber > 0) "&season_number=$seasonNumber" else ""
            val yearQuery = if (year != null && year > 1980) "&year=$year" else ""

            val url = "https://api.opensubtitles.com/api/v1/subtitles?query=${Uri.encode(cleanTitle)}&languages=$langParam$epQuery$seasonQuery$yearQuery"
            val body = fetchString(url) ?: return emptyList()

            val json = JSONObject(body)
            val dataArray = json.optJSONArray("data") ?: return emptyList()
            for (i in 0 until minOf(dataArray.length(), 10)) {
                val item = dataArray.getJSONObject(i)
                val attributes = item.optJSONObject("attributes") ?: continue
                val subFiles = attributes.optJSONArray("files")
                val fileId = subFiles?.optJSONObject(0)?.optInt("file_id") ?: continue
                val lang = attributes.optString("language", "en")

                val langName = getLanguageDisplayName(lang)
                val trackLabel = buildCleanSubtitleLabel(
                    langName = langName,
                    format = "srt",
                    index = results.count { it.languageCode == lang } + 1
                )

                results.add(
                    SubtitleTrackItem(
                        id = "ext_sub_${item.optString("id", fileId.toString())}",
                        title = cleanSubtitleDisplayTitle(cleanTitle, langName, episodeNumber),
                        languageCode = lang,
                        languageName = trackLabel,
                        source = SubtitleSource.OPENSUBTITLES,
                        remoteUrl = "https://api.opensubtitles.com/api/v1/download?file_id=$fileId",
                        format = "srt",
                        matchedEpisode = episodeNumber
                    )
                )
            }
        } catch (_: Exception) {
        }
        return results
    }

    private fun fetchString(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Android; Mobile; PlayClient/1.0)")
            .header("Accept", "application/json, text/html, */*")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            return response.body?.string()
        }
    }

    /**
     * Downloads, decompresses (.xz, .gz, .zip if applicable), normalizes (.ass/.ssa to clean .srt when needed),
     * and caches a remote subtitle file to disk so ExoPlayer can render it reliably.
     */
    suspend fun downloadAndCacheSubtitle(
        context: Context,
        subtitleUrl: String,
        suggestedFileName: String,
        requestedEpisode: Int? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val subDir = File(context.cacheDir, "subtitles").apply { if (!exists()) mkdirs() }
            val baseCleanName = suggestedFileName
                .replace(Regex("""(?i)\.(xz|gz|zip)$"""), "")
                .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val finalFileName = if (baseCleanName.endsWith(".vtt", ignoreCase = true)) {
                baseCleanName
            } else {
                baseCleanName.replace(Regex("""(?i)\.(ass|ssa|srt)$"""), "") + ".srt"
            }
            val targetFile = File(subDir, finalFileName)

            if (targetFile.exists() && targetFile.length() > 64L) {
                return@withContext Result.success(targetFile)
            }

            val request = Request.Builder()
                .url(subtitleUrl)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile; PlayClient/1.0)")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Failed to download subtitle: HTTP ${response.code}"))
                }

                val rawBytes = response.body?.bytes()
                    ?: return@withContext Result.failure(Exception("Empty subtitle response"))

                val decompressedText = decodeSubtitlePayload(
                    rawBytes = rawBytes,
                    url = subtitleUrl,
                    requestedEpisode = requestedEpisode
                ) ?: return@withContext Result.failure(Exception("Could not decode subtitle content"))

                val normalizedContent = if (isAssOrSsaContent(decompressedText)) {
                    convertAssToSrt(decompressedText)
                } else {
                    decompressedText
                }

                if (normalizedContent.isBlank()) {
                    return@withContext Result.failure(Exception("Decoded subtitle file was empty"))
                }

                FileOutputStream(targetFile).use { output ->
                    output.write(normalizedContent.toByteArray(Charsets.UTF_8))
                }
            }

            Result.success(targetFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Automatically decompresses .xz, .gz, or .zip subtitle archives and extracts the text for the target episode.
     */
    private fun decodeSubtitlePayload(
        rawBytes: ByteArray,
        url: String,
        requestedEpisode: Int?
    ): String? {
        if (rawBytes.isEmpty()) return null
        val lowerUrl = url.lowercase(Locale.ROOT)

        // 1. Check XZ magic bytes (FD 37 7A 58 5A 00) or .xz extension
        val isXz = lowerUrl.endsWith(".xz") || (
                rawBytes.size >= 6 &&
                rawBytes[0] == 0xFD.toByte() &&
                rawBytes[1] == 0x37.toByte() &&
                rawBytes[2] == 0x7A.toByte() &&
                rawBytes[3] == 0x58.toByte() &&
                rawBytes[4] == 0x5A.toByte()
        )
        if (isXz) {
            return try {
                XZInputStream(ByteArrayInputStream(rawBytes)).use { xzIn ->
                    xzIn.readBytes().toString(Charsets.UTF_8)
                }
            } catch (_: Exception) {
                null
            }
        }

        // 2. Check GZIP magic bytes (1F 8B) or .gz extension
        val isGzip = lowerUrl.endsWith(".gz") || (
                rawBytes.size >= 2 &&
                rawBytes[0] == 0x1F.toByte() &&
                rawBytes[1] == 0x8B.toByte()
        )
        if (isGzip) {
            return try {
                GZIPInputStream(ByteArrayInputStream(rawBytes)).use { gzIn ->
                    gzIn.readBytes().toString(Charsets.UTF_8)
                }
            } catch (_: Exception) {
                null
            }
        }

        // 3. Check ZIP magic bytes (50 4B 03 04) or .zip extension
        val isZip = lowerUrl.endsWith(".zip") || (
                rawBytes.size >= 4 &&
                rawBytes[0] == 0x50.toByte() &&
                rawBytes[1] == 0x4B.toByte() &&
                rawBytes[2] == 0x03.toByte() &&
                rawBytes[3] == 0x04.toByte()
        )
        if (isZip) {
            return extractBestSubtitleFromZip(ByteArrayInputStream(rawBytes), requestedEpisode)
        }

        // 4. Plain UTF-8 / UTF-16 subtitle file (.srt, .ass, .ssa, .vtt)
        return stripBomAndDecode(rawBytes)
    }

    private fun extractBestSubtitleFromZip(
        inputStream: InputStream,
        requestedEpisode: Int?
    ): String? {
        val entries = mutableListOf<Pair<String, String>>()
        try {
            ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val name = entry.name
                    if (!entry.isDirectory && (
                        name.endsWith(".srt", ignoreCase = true) ||
                        name.endsWith(".ass", ignoreCase = true) ||
                        name.endsWith(".ssa", ignoreCase = true) ||
                        name.endsWith(".vtt", ignoreCase = true)
                    )) {
                        val baos = ByteArrayOutputStream()
                        val buffer = ByteArray(4096)
                        var count: Int
                        while (zis.read(buffer).also { count = it } != -1) {
                            baos.write(buffer, 0, count)
                        }
                        entries.add(name to stripBomAndDecode(baos.toByteArray()))
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        } catch (_: Exception) {
        }

        if (entries.isEmpty()) return null
        if (requestedEpisode != null && requestedEpisode > 0) {
            val matched = entries.firstOrNull { (name, _) ->
                detectSubtitleEpisode(name) == requestedEpisode
            }
            if (matched != null) return matched.second
        }
        return entries.first().second
    }

    private fun stripBomAndDecode(bytes: ByteArray): String {
        if (bytes.size >= 3 &&
            bytes[0] == 0xEF.toByte() &&
            bytes[1] == 0xBB.toByte() &&
            bytes[2] == 0xBF.toByte()
        ) {
            return String(bytes, 3, bytes.size - 3, Charsets.UTF_8)
        }
        if (bytes.size >= 2 &&
            bytes[0] == 0xFF.toByte() &&
            bytes[1] == 0xFE.toByte()
        ) {
            return String(bytes, 2, bytes.size - 2, Charsets.UTF_16LE)
        }
        if (bytes.size >= 2 &&
            bytes[0] == 0xFE.toByte() &&
            bytes[1] == 0xFF.toByte()
        ) {
            return String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE)
        }
        return String(bytes, Charsets.UTF_8)
    }

    private fun isAssOrSsaContent(text: String): Boolean {
        val head = text.take(1500)
        return head.contains("[Script Info]", ignoreCase = true) ||
                head.contains("[Events]", ignoreCase = true) ||
                head.contains("Dialogue:", ignoreCase = true)
    }

    /**
     * Converts Advanced SubStation Alpha (.ass / .ssa) subtitle script into clean SubRip (.srt)
     * so ExoPlayer's SubtitleView renders every line cleanly with user styling and zero vector tag artifacts.
     */
    fun convertAssToSrt(assText: String): String {
        val lines = assText.lines()
        var startIdx = 1
        var endIdx = 2
        var textIdx = 9
        var formatColumns = 10

        val cues = mutableListOf<Triple<Long, Long, String>>()

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.startsWith("Format:", ignoreCase = true) && line.contains("Start", ignoreCase = true)) {
                val cols = line.substringAfter(":").split(",").map { it.trim().lowercase(Locale.ROOT) }
                formatColumns = cols.size
                val s = cols.indexOf("start")
                val e = cols.indexOf("end")
                val t = cols.indexOf("text")
                if (s >= 0) startIdx = s
                if (e >= 0) endIdx = e
                if (t >= 0) textIdx = t
            } else if (line.startsWith("Dialogue:", ignoreCase = true)) {
                val payload = line.substringAfter(":")
                val parts = payload.split(",", limit = formatColumns.coerceAtLeast(textIdx + 1))
                if (parts.size > textIdx && parts.size > endIdx) {
                    val startMs = parseAssTimestampMs(parts[startIdx].trim()) ?: continue
                    val endMs = parseAssTimestampMs(parts[endIdx].trim()) ?: continue
                    if (endMs <= startMs) continue

                    val rawDialogue = parts.subList(textIdx, parts.size).joinToString(",")
                    // Skip vector drawing lines ({\p1}...{\p0})
                    if (rawDialogue.contains("\\p1") || rawDialogue.contains("\\p2")) continue

                    val cleanText = rawDialogue
                        .replace(Regex("""\{[^}]*\}"""), "") // Strip ASS override tags
                        .replace("\\N", "\n")
                        .replace("\\n", "\n")
                        .replace("\\h", " ")
                        .trim()

                    if (cleanText.isNotBlank()) {
                        cues.add(Triple(startMs, endMs, cleanText))
                    }
                }
            }
        }

        if (cues.isEmpty()) return assText

        val sortedCues = cues.sortedBy { it.first }
        val sb = StringBuilder()
        sortedCues.forEachIndexed { index, (startMs, endMs, text) ->
            sb.append(index + 1).append('\n')
            sb.append(formatSrtTimestamp(startMs)).append(" --> ").append(formatSrtTimestamp(endMs)).append('\n')
            sb.append(text).append("\n\n")
        }
        return sb.toString()
    }

    private fun parseAssTimestampMs(ts: String): Long? {
        // Format: H:MM:SS.cc (centiseconds)
        val match = Regex("""(\d+):(\d{2}):(\d{2})[.:](\d{1,3})""").find(ts) ?: return null
        val h = match.groupValues[1].toLongOrNull() ?: return null
        val m = match.groupValues[2].toLongOrNull() ?: return null
        val s = match.groupValues[3].toLongOrNull() ?: return null
        val fracStr = match.groupValues[4].padEnd(3, '0').take(3)
        val ms = fracStr.toLongOrNull() ?: 0L
        return ((h * 3600L + m * 60L + s) * 1000L) + ms
    }

    private fun formatSrtTimestamp(ms: Long): String {
        val totalSec = ms / 1000L
        val millis = ms % 1000L
        val hours = totalSec / 3600L
        val minutes = (totalSec % 3600L) / 60L
        val seconds = totalSec % 60L
        return String.format(Locale.ROOT, "%02d:%02d:%02d,%03d", hours, minutes, seconds, millis)
    }

    /**
     * Imports a user-selected local .srt, .vtt, or .ass file from Android SAF Uri.
     */
    suspend fun importLocalSubtitle(context: Context, uri: Uri): Result<File> = withContext(Dispatchers.IO) {
        try {
            val subDir = File(context.cacheDir, "subtitles").apply { if (!exists()) mkdirs() }
            val targetFile = File(subDir, "imported_${System.currentTimeMillis()}.srt")

            val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext Result.failure(Exception("Could not read chosen subtitle file"))

            val text = stripBomAndDecode(rawBytes)
            val normalized = if (isAssOrSsaContent(text)) convertAssToSrt(text) else text

            FileOutputStream(targetFile).use { output ->
                output.write(normalized.toByteArray(Charsets.UTF_8))
            }

            Result.success(targetFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun detectSubtitleEpisode(filename: String): Int? {
        val cleaned = filename
            .replace(Regex("""(?i)\.(srt|vtt|ass|ssa|xz|gz|zip|mkv|mp4)$"""), "")
            .replace(Regex("""(?i)\b(1080p|720p|480p|2160p|x264|x265|hevc|av1|aac|flac|10bit|8bit|bd|web)\b"""), " ")
            .replace(Regex("""\[[0-9A-Fa-f]{8}\]"""), " ")

        val sxeRegex = Regex("""(?i)(?:s\d{1,2}[._\s-]*e|(?:\b\d{1,2}x))0*(\d{1,4})(?:\b|v\d|_)""")
        sxeRegex.find(cleaned)?.let { return it.groupValues[1].toIntOrNull() }

        val regex = Regex("""(?i)(?:^|[\s._\-\[\(])(?:ep|episode|e)[.\s_-]*0*(\d{1,4})(?:v\d)?(?:$|[\s._\-\]\)])""")
        regex.find(cleaned)?.let { return it.groupValues[1].toIntOrNull() }

        val dashRegex = Regex("""[\s_]+-\s*0*(\d{1,4})(?:v\d)?(?:$|[\s._\-\[\(])""")
        dashRegex.find(cleaned)?.let { return it.groupValues[1].toIntOrNull() }

        val bracketRegex = Regex("""[_\s\-\[\(]0*(\d{1,3})(?:v\d)?[_\s\-\]\)\.]""")
        val bracketMatch = bracketRegex.find(cleaned)
        if (bracketMatch != null) {
            val num = bracketMatch.groupValues[1].toIntOrNull()
            if (num != null && num in 1..999) return num
        }

        val trailingRegex = Regex("""(?:[_\s\-])0*(\d{1,3})(?:v\d)?$""")
        trailingRegex.find(cleaned.trim())?.let {
            val num = it.groupValues[1].toIntOrNull()
            if (num != null && num in 1..500) return num
        }

        return null
    }

    private fun cleanSearchTitle(title: String): String {
        return title.trim()
            .replace(Regex("(?i)\\b(season|part|cour)\\b.*"), "")
            .replace(Regex("[^a-zA-Z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun buildCleanSubtitleLabel(langName: String, format: String, index: Int): String {
        val fmtUpper = format.uppercase(Locale.ROOT)
        return if (index <= 1) {
            "$langName ($fmtUpper)"
        } else {
            "$langName #$index ($fmtUpper)"
        }
    }

    private fun cleanSubtitleDisplayTitle(rawTitle: String, langName: String, epNum: Int?): String {
        val epLabel = epNum?.let { "Episode $it" } ?: "Full Track"
        return "$langName • $epLabel"
    }

    fun detectLanguageFromText(text: String): Pair<String, String> {
        val lower = text.lowercase(Locale.ROOT)
        return when {
            lower.contains("ja") || lower.contains("jpn") || lower.contains("japanese") || lower.contains("日本語") ->
                Pair("ja", "Japanese (日本語)")
            lower.contains("zh-tw") || lower.contains("traditional") || lower.contains("繁體") || lower.contains("cht") ->
                Pair("zh-Hant", "Chinese (Traditional 繁體)")
            lower.contains("zh") || lower.contains("chi") || lower.contains("simplified") || lower.contains("简体") || lower.contains("chinese") || lower.contains("chs") ->
                Pair("zh-Hans", "Chinese (Simplified 简体)")
            lower.contains("hi") || lower.contains("hin") || lower.contains("hindi") || lower.contains("हिन्दी") ->
                Pair("hi", "Hindi (हिन्दी)")
            lower.contains("es") || lower.contains("spa") || lower.contains("spanish") || lower.contains("español") ->
                Pair("es", "Spanish (Español)")
            lower.contains("fr") || lower.contains("fre") || lower.contains("french") || lower.contains("français") ->
                Pair("fr", "French (Français)")
            lower.contains("de") || lower.contains("ger") || lower.contains("german") || lower.contains("deutsch") ->
                Pair("de", "German (Deutsch)")
            lower.contains("pt") || lower.contains("por") || lower.contains("portuguese") ->
                Pair("pt", "Portuguese")
            lower.contains("ru") || lower.contains("rus") || lower.contains("russian") ->
                Pair("ru", "Russian")
            lower.contains("ar") || lower.contains("ara") || lower.contains("arabic") ->
                Pair("ar", "Arabic")
            lower.contains("id") || lower.contains("ind") || lower.contains("indonesian") ->
                Pair("id", "Indonesian")
            lower.contains("en") || lower.contains("eng") || lower.contains("english") ->
                Pair("en", "English")
            else ->
                Pair("en", "English")
        }
    }

    fun getLanguageDisplayName(code: String): String {
        return when (code.lowercase(Locale.ROOT)) {
            "en", "eng" -> "English"
            "ja", "jpn" -> "Japanese (日本語)"
            "zh-hans", "zh-cn", "chi", "chs" -> "Chinese (Simplified 简体)"
            "zh-hant", "zh-tw", "cht" -> "Chinese (Traditional 繁體)"
            "hi", "hin" -> "Hindi (हिन्दी)"
            "es", "spa" -> "Spanish"
            "fr", "fre" -> "French"
            "de", "ger" -> "German"
            "id", "ind" -> "Indonesian"
            "pt", "por" -> "Portuguese"
            "ru", "rus" -> "Russian"
            "ar", "ara" -> "Arabic"
            else -> Locale(code).displayLanguage.takeIf { it.isNotBlank() } ?: code.uppercase(Locale.ROOT)
        }
    }
}
