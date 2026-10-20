package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AnimeEntity
import com.example.data.local.AppDatabase
import com.example.data.remote.ArchiveDocItem
import com.example.data.remote.ArchiveFileItem
import com.example.data.remote.ArchiveItemMetadata
import com.example.data.remote.ArchiveMetadataResponse
import com.example.data.repository.AnimeRepository
import com.example.data.repository.ArchiveSearchRepository
import com.example.data.repository.MatchConfidence
import com.example.data.repository.PlaybackRepository
import com.example.data.repository.VideoMatchService
import com.example.viewmodel.PlayerUiState
import com.example.viewmodel.PlayerViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class InternetArchiveWorkflowTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var animeRepository: AnimeRepository
    private lateinit var videoMatchService: VideoMatchService
    private lateinit var playbackRepository: PlaybackRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = AppDatabase.getInstance(context)
        animeRepository = AnimeRepository(
            animeDao = database.animeDao(),
            historyDao = database.episodeHistoryDao(),
            apiCacheDao = database.apiCacheDao()
        )
        videoMatchService = VideoMatchService()
        playbackRepository = PlaybackRepository(context)
    }

    @Test
    fun testEpisodeNumberDetection() {
        // Test various standard and anime release filename conventions
        assertEquals(1, videoMatchService.detectEpisodeNumber("Frieren - Episode 01 [1080p].mp4"))
        assertEquals(2, videoMatchService.detectEpisodeNumber("Sousou no Frieren Ep. 02.mkv"))
        assertEquals(3, videoMatchService.detectEpisodeNumber("[SubsPlease] Frieren - 03 (1080p) [ABCD1234].mkv"))
        assertEquals(4, videoMatchService.detectEpisodeNumber("Frieren_E04_HD.mp4"))
        assertEquals(12, videoMatchService.detectEpisodeNumber("Anime_Series_Ep12_Final.mp4"))
        assertNull(videoMatchService.detectEpisodeNumber("Official_Trailer_2024.mp4"))
        assertNull(videoMatchService.detectEpisodeNumber("Soundtrack_OST_Full.mp3"))
    }

    @Test
    fun testVideoFileFilteringExcludesNonMediaAndStubs() {
        val files = listOf(
            ArchiveFileItem(name = "metadata.xml", size = "1024", format = "Metadata"),
            ArchiveFileItem(name = "item_files.xml", size = "5000", format = "Metadata"),
            ArchiveFileItem(name = "poster.jpg", size = "80000", format = "JPEG"),
            ArchiveFileItem(name = "archive.torrent", size = "12000", format = "Torrent"),
            ArchiveFileItem(name = "stub_preview.mp4", size = "100000", format = "MPEG4"), // < 500KB stub
            ArchiveFileItem(name = "frieren_ep01.mp4", size = "150000000", format = "h.264"),
            ArchiveFileItem(name = "frieren_ep01.m3u8", size = "2000000", format = "hls"),
            ArchiveFileItem(name = "subtitles_en.srt", size = "45000", format = "SubRip")
        )

        val compatible = videoMatchService.filterCompatibleVideoFiles(files)
        assertEquals(2, compatible.size)
        assertTrue(compatible.any { it.name == "frieren_ep01.mp4" })
        assertTrue(compatible.any { it.name == "frieren_ep01.m3u8" })
        assertFalse(compatible.any { it.name.endsWith(".xml") })
        assertFalse(compatible.any { it.name.endsWith(".torrent") })
        assertFalse(compatible.any { it.name == "stub_preview.mp4" })
    }

    @Test
    fun testTitleSimilarityMatching() {
        val targetTitles = listOf("Sousou no Frieren", "Frieren: Beyond Journey's End")

        // Exact & strong containment match
        val scoreExact = videoMatchService.calculateTitleSimilarity("Sousou no Frieren Episode 01", targetTitles)
        assertTrue(scoreExact >= 0.70f)

        val scoreEnglish = videoMatchService.calculateTitleSimilarity("Frieren Beyond Journeys End Ep 1", targetTitles)
        assertTrue(scoreEnglish >= 0.70f)

        // Completely unrelated titles
        val scoreUnrelated = videoMatchService.calculateTitleSimilarity("Attack on Titan Season 4", targetTitles)
        assertTrue(scoreUnrelated < 0.25f)
    }

    @Test
    fun testCandidateEvaluationAndRanking() {
        val testAnime = AnimeEntity(
            id = 154587,
            idMal = 52991,
            titleRomaji = "Sousou no Frieren",
            titleEnglish = "Frieren: Beyond Journey's End",
            coverImage = "https://example.com/cover.jpg",
            totalEpisodes = 28,
            currentEpisode = 28,
            airingDayOfWeek = 5
        )

        val matchingDoc = ArchiveDocItem(
            identifier = "frieren-ep-01-archive-item",
            title = "Frieren: Beyond Journey's End Episode 01",
            description = "Frieren Episode 1 public stream",
            mediatype = "movies"
        )

        val metadata = ArchiveMetadataResponse(
            files = listOf(
                ArchiveFileItem(name = "frieren_ep01_1080p.mp4", size = "250000000", format = "h.264", length = "1440"),
                ArchiveFileItem(name = "frieren_ep01_720p.mp4", size = "120000000", format = "h.264", length = "1440")
            ),
            metadata = ArchiveItemMetadata(
                title = "Frieren: Beyond Journey's End Episode 01",
                licenseurl = "https://creativecommons.org/licenses/publicdomain/"
            )
        )

        val candidate = videoMatchService.evaluateCandidate(
            doc = matchingDoc,
            metadata = metadata,
            anime = testAnime,
            requestedEpisode = 1
        )

        assertNotNull(candidate)
        assertEquals("frieren-ep-01-archive-item", candidate?.identifier)
        assertEquals(1, candidate?.matchedEpisode)
        assertTrue(candidate?.isEpisodeSpecific == true)
        assertEquals(MatchConfidence.HIGH, candidate?.matchConfidence)
        assertEquals("frieren_ep01_1080p.mp4", candidate?.bestPlayableFile?.name)
        assertTrue(candidate?.streamUrl?.contains("archive.org/download/frieren-ep-01-archive-item") == true)
    }

    @Test
    fun testPromoClipsExcludedWhenSearchingFullEpisode() {
        val testAnime = AnimeEntity(
            id = 154587,
            titleRomaji = "Sousou no Frieren",
            titleEnglish = "Frieren: Beyond Journey's End",
            coverImage = "https://example.com/cover.jpg",
            airingDayOfWeek = 5
        )

        val trailerDoc = ArchiveDocItem(
            identifier = "frieren-official-trailer",
            title = "Frieren Beyond Journey's End Official Trailer",
            mediatype = "movies"
        )

        val metadata = ArchiveMetadataResponse(
            files = listOf(ArchiveFileItem(name = "trailer.mp4", size = "25000000", format = "h.264"))
        )

        val candidate = videoMatchService.evaluateCandidate(
            doc = trailerDoc,
            metadata = metadata,
            anime = testAnime,
            requestedEpisode = 1
        )

        // Must be excluded for full episode requests
        assertNull(candidate)
    }

    @Test
    fun testPlaybackRepositoryPositionAndFallback() {
        val animeId = 998877
        val episodeNumber = 5

        // Save position
        playbackRepository.savePositionMs(animeId, episodeNumber, 45000L)
        val loaded = playbackRepository.getSavedPositionMs(animeId, episodeNumber)
        assertEquals(45000L, loaded)

        // Threshold test: very early position (< 5s) ignored
        playbackRepository.savePositionMs(animeId, episodeNumber, 2000L)
        assertEquals(0L, playbackRepository.getSavedPositionMs(animeId, episodeNumber))

        // Finished near end reset
        playbackRepository.savePositionMs(animeId, episodeNumber, 1190000L)
        assertEquals(0L, playbackRepository.getSavedPositionMs(animeId, episodeNumber, durationMs = 1200000L))

        // Fallback file test
        val files = listOf(
            ArchiveFileItem(name = "ep05_1080p.mp4", size = "300000000", format = "MP4"),
            ArchiveFileItem(name = "ep05_720p.mp4", size = "150000000", format = "MP4")
        )
        val fallback = playbackRepository.getFallbackStream(
            identifier = "archive_item_123",
            allCompatibleFiles = files,
            failedFileName = "ep05_1080p.mp4"
        )

        assertNotNull(fallback)
        assertEquals("ep05_720p.mp4", fallback?.first?.name)
        assertTrue(fallback?.second?.contains("ep05_720p.mp4") == true)

        // Clear resume position
        playbackRepository.clearResumePosition(animeId, episodeNumber)
        assertEquals(0L, playbackRepository.getSavedPositionMs(animeId, episodeNumber))
    }

    @Test
    fun testTrackerProgressIntegration() = runBlocking {
        val anime = AnimeEntity(
            id = 445566,
            titleRomaji = "Dungeon Meshi",
            titleEnglish = "Delicious in Dungeon",
            coverImage = "https://example.com/dungeon.jpg",
            totalEpisodes = 24,
            watchedEpisodeCount = 2,
            airingDayOfWeek = 4
        )

        database.animeDao().insertAnime(anime)

        val initial = animeRepository.getAnimeByIdSync(445566)
        assertEquals(2, initial?.watchedEpisodeCount)

        // Confirm episode completion updates tracker
        animeRepository.updateWatchedEpisodeCount(445566, 3)
        val updated = animeRepository.getAnimeByIdSync(445566)
        assertEquals(3, updated?.watchedEpisodeCount)
    }

    @Test
    fun testQualityOptionsExtraction() {
        val files = listOf(
            ArchiveFileItem(name = "anime_ep01_1080p.mp4", size = "400000000", format = "h.264", height = "1080"),
            ArchiveFileItem(name = "anime_ep01_720p.mp4", size = "200000000", format = "h.264", height = "720"),
            ArchiveFileItem(name = "anime_ep01_480p.mp4", size = "100000000", format = "h.264", height = "480"),
            ArchiveFileItem(name = "anime_ep01.m3u8", size = "5000000", format = "hls")
        )

        val options = videoMatchService.extractQualityOptions("test-item-id", files, 1)
        assertEquals(4, options.size)
        assertEquals("1080p (Full HD)", options[0].label)
        assertEquals("720p (HD)", options[1].label)
        assertEquals("480p (SD)", options[2].label)
        assertTrue(options.any { it.label.contains("HLS") })
    }

    @Test
    fun testMultilingualSubtitleDetection() {
        val subRepo = com.example.data.repository.SubtitleRepository()
        val files = listOf(
            ArchiveFileItem(name = "ep01_japanese_ja.srt", format = "SubRip"),
            ArchiveFileItem(name = "ep01_chinese_simplified_zh.vtt", format = "WebVTT"),
            ArchiveFileItem(name = "ep01_chinese_traditional_zh-tw.srt", format = "SubRip"),
            ArchiveFileItem(name = "ep01_hindi_hi.srt", format = "SubRip"),
            ArchiveFileItem(name = "ep01_english_en.srt", format = "SubRip")
        )

        val subs = subRepo.extractArchiveSubtitles("sub-item-id", files, 1)
        assertEquals(5, subs.size)
        assertTrue(subs.any { it.languageCode == "ja" && it.languageName.contains("Japanese") })
        assertTrue(subs.any { it.languageCode == "zh-Hans" && it.languageName.contains("Simplified") })
        assertTrue(subs.any { it.languageCode == "zh-Hant" && it.languageName.contains("Traditional") })
        assertTrue(subs.any { it.languageCode == "hi" && it.languageName.contains("Hindi") })
        assertTrue(subs.any { it.languageCode == "en" && it.languageName.contains("English") })
    }

    @Test
    fun testPlaybackSpeedRulerIncrements() {
        val speeds = com.example.ui.components.ALL_PLAYBACK_SPEEDS
        assertTrue(speeds.contains(0.5f))
        assertTrue(speeds.contains(0.75f))
        assertTrue(speeds.contains(1.0f))
        assertTrue(speeds.contains(1.1f))
        assertTrue(speeds.contains(1.2f))
        assertTrue(speeds.contains(1.3f))
        assertTrue(speeds.contains(2.5f))
        assertTrue(speeds.contains(3.9f))
        assertTrue(speeds.contains(4.0f))
        assertEquals(33, speeds.size)
    }

    @Test
    fun testDeepSearchRejectsReactionsLiveActionShortClipsAndWrongEpisodes() {
        val testAnime = AnimeEntity(
            id = 154587,
            titleRomaji = "Sousou no Frieren",
            titleEnglish = "Frieren: Beyond Journey's End",
            coverImage = "https://example.com/cover.jpg",
            totalEpisodes = 28,
            currentEpisode = 28,
            airingDayOfWeek = 5
        )

        // 1. Reaction video must be rejected
        val reactionDoc = ArchiveDocItem(
            identifier = "frieren-ep-1-reaction",
            title = "Frieren: Beyond Journey's End Episode 1 Reaction!",
            mediatype = "movies"
        )
        val fullMetadata = ArchiveMetadataResponse(
            files = listOf(ArchiveFileItem(name = "frieren_ep01.mp4", size = "180000000", format = "h.264", length = "1420"))
        )
        assertNull(videoMatchService.evaluateCandidate(reactionDoc, fullMetadata, testAnime, requestedEpisode = 1))

        // 2. Live action video must be rejected
        val liveActionDoc = ArchiveDocItem(
            identifier = "frieren-live-action-ep1",
            title = "Frieren: Beyond Journey's End Live Action Episode 1",
            mediatype = "movies"
        )
        assertNull(videoMatchService.evaluateCandidate(liveActionDoc, fullMetadata, testAnime, requestedEpisode = 1))

        // 3. 2-3 minute clip (150 seconds) must be rejected when searching for Episode 1
        val shortClipDoc = ArchiveDocItem(
            identifier = "frieren-ep-01-clip",
            title = "Frieren: Beyond Journey's End Episode 01",
            mediatype = "movies"
        )
        val shortClipMetadata = ArchiveMetadataResponse(
            files = listOf(ArchiveFileItem(name = "frieren_ep01.mp4", size = "25000000", format = "h.264", length = "150"))
        )
        assertNull(videoMatchService.evaluateCandidate(shortClipDoc, shortClipMetadata, testAnime, requestedEpisode = 1))

        // 4. Wrong episode (e.g., archive only has Episode 1 and 2, but user selected Episode 5) must be rejected, NEVER fallback to Episode 1!
        val ep1And2Doc = ArchiveDocItem(
            identifier = "frieren-archive-batch",
            title = "Sousou no Frieren",
            mediatype = "movies"
        )
        val ep1And2Meta = ArchiveMetadataResponse(
            files = listOf(
                ArchiveFileItem(name = "Sousou no Frieren - 01 [1080p].mp4", size = "250000000", format = "h.264", length = "1440"),
                ArchiveFileItem(name = "Sousou no Frieren - 02 [1080p].mp4", size = "250000000", format = "h.264", length = "1440")
            )
        )
        assertNull(videoMatchService.evaluateCandidate(ep1And2Doc, ep1And2Meta, testAnime, requestedEpisode = 5))

        // 5. Requesting Episode 2 on the same batch returns the exact Episode 2 file
        val exactEp2Match = videoMatchService.evaluateCandidate(ep1And2Doc, ep1And2Meta, testAnime, requestedEpisode = 2)
        assertNotNull(exactEp2Match)
        assertEquals(2, exactEp2Match?.matchedEpisode)
        assertEquals("Sousou no Frieren - 02 [1080p].mp4", exactEp2Match?.bestPlayableFile?.name)

        // 6. Underscore and camelCase reaction/live-action filenames or descriptions must be rejected
        assertTrue(videoMatchService.containsExcludedKeywords("One_Piece_Live_Action_S01E01.mp4"))
        assertTrue(videoMatchService.containsExcludedKeywords("SousouNoFrierenReactionEp02.mp4"))
        assertTrue(
            videoMatchService.containsExcludedKeywords(
                title = "Sousou no Frieren Episode 2",
                description = "Full uncut Patreon reaction and watchalong"
            )
        )

        // 7. Long 45-minute (2700s) reaction video or 110-minute (6600s) live-action movie must be rejected for Episode 2
        val longVideoDoc = ArchiveDocItem(
            identifier = "frieren-ep-02-long",
            title = "Sousou no Frieren Episode 02",
            mediatype = "movies"
        )
        val longReactionMetadata = ArchiveMetadataResponse(
            files = listOf(ArchiveFileItem(name = "Sousou no Frieren - 02.mp4", size = "450000000", format = "h.264", length = "2700"))
        )
        assertNull(videoMatchService.evaluateCandidate(longVideoDoc, longReactionMetadata, testAnime, requestedEpisode = 2))
    }

    @Test
    fun testAssToSrtSubtitleConversion() {
        val subRepo = com.example.data.repository.SubtitleRepository()
        val sampleAss = """
            [Script Info]
            Title: Episode 01
            [Events]
            Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
            Dialogue: 0,0:00:05.20,0:00:08.50,Default,,0,0,0,,{\i1}Hello world!\NSecond line{\i0}
        """.trimIndent()

        val srt = subRepo.convertAssToSrt(sampleAss)
        assertTrue(srt.contains("00:00:05,200 --> 00:00:08,500"))
        assertTrue(srt.contains("Hello world!\nSecond line"))
        assertFalse(srt.contains("{\\i1}"))
    }
}


