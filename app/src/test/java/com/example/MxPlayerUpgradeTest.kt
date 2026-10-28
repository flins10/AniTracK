package com.example

import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.PlaybackRepository
import com.example.data.repository.SubtitleRepository
import com.example.viewmodel.PlayerViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MxPlayerUpgradeTest {

    private lateinit var playbackRepository: PlaybackRepository
    private lateinit var subtitleRepository: SubtitleRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        playbackRepository = PlaybackRepository(context)
        playbackRepository.clearLocalVideoHistory()
        subtitleRepository = SubtitleRepository()
    }

    @Test
    fun localVideoHistory_persistsAndIsolatesFromAnimeResumeProgress() {
        // Save anime streaming episode progress
        playbackRepository.savePositionMs(
            animeId = 101,
            episodeNumber = 4,
            positionMs = 420_000L
        )

        // Save two local device videos
        val localUri1 = "content://com.android.providers.media.documents/document/video%3A1001"
        val localUri2 = "content://com.android.providers.media.documents/document/video%3A1002"

        playbackRepository.saveLocalVideoProgress(
            uriString = localUri1,
            displayName = "Episode_01_BD_1080p.mkv",
            positionMs = 185_000L,
            durationMs = 1_420_000L,
            mimeType = MimeTypes.VIDEO_MATROSKA
        )

        playbackRepository.saveLocalVideoProgress(
            uriString = localUri2,
            displayName = "Local_Movie_Clip.mp4",
            positionMs = 65_000L,
            durationMs = 600_000L,
            mimeType = MimeTypes.VIDEO_MP4
        )

        val recentLocal = playbackRepository.getRecentLocalVideos()
        assertEquals(2, recentLocal.size)
        assertEquals("Local_Movie_Clip.mp4", recentLocal[0].displayName)
        assertEquals(65_000L, playbackRepository.getLocalVideoSavedPositionMs(localUri2))
        assertEquals(185_000L, playbackRepository.getLocalVideoSavedPositionMs(localUri1))

        // Verify anime progress is untouched
        assertEquals(420_000L, playbackRepository.getSavedPositionMs(101, 4))

        // Remove one local video and verify only the other remains
        playbackRepository.removeLocalVideoFromHistory(localUri2)
        val afterRemoval = playbackRepository.getRecentLocalVideos()
        assertEquals(1, afterRemoval.size)
        assertEquals(localUri1, afterRemoval[0].uriString)
        assertEquals(0L, playbackRepository.getLocalVideoSavedPositionMs(localUri2))
        assertEquals(420_000L, playbackRepository.getSavedPositionMs(101, 4))
    }

    @Test
    fun skipIncrementSeconds_clampsAndPersists() {
        playbackRepository.skipIncrementSeconds = 30
        assertEquals(30, playbackRepository.skipIncrementSeconds)

        playbackRepository.skipIncrementSeconds = 1 // below minimum 5s
        assertEquals(5, playbackRepository.skipIncrementSeconds)

        playbackRepository.skipIncrementSeconds = 120 // above maximum 90s
        assertEquals(90, playbackRepository.skipIncrementSeconds)
    }

    @Test
    fun subtitleRepository_appliesPositiveAndNegativeTimingOffsetsAndClampsAtZero() {
        val rawSrt = """
            1
            00:00:01,500 --> 00:00:04,000
            First line of dialogue

            2
            00:01:10,250 --> 00:01:13,750
            Second line of dialogue
        """.trimIndent()

        // Shift forward by +1250ms (+1.25s)
        val shiftedForward = subtitleRepository.applyTimingOffsetToSubtitleText(rawSrt, 1250L)
        assertTrue(shiftedForward.contains("00:00:02,750 --> 00:00:05,250"))
        assertTrue(shiftedForward.contains("00:01:11,500 --> 00:01:15,000"))

        // Shift backward by -2000ms (-2.0s), verifying clamping at 00:00:00,000
        val shiftedBackward = subtitleRepository.applyTimingOffsetToSubtitleText(rawSrt, -2000L)
        assertTrue(shiftedBackward.contains("00:00:00,000 --> 00:00:02,000"))
        assertTrue(shiftedBackward.contains("00:01:08,250 --> 00:01:11,750"))
    }

    @Test
    fun subtitleRepository_convertsAssToCleanSrt() {
        val sampleAss = """
            [Events]
            Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
            Dialogue: 0,0:00:05.50,0:00:08.25,Default,,0,0,0,,{\pos(192,220)\b1}Hello\NWorld!{\b0}
        """.trimIndent()

        val srt = subtitleRepository.convertAssToSrt(sampleAss)
        assertTrue(srt.contains("00:00:05,500 --> 00:00:08,250"))
        assertTrue(srt.contains("Hello\nWorld!"))
        assertTrue(!srt.contains("{\\pos"))
    }

    @Test
    fun playerViewModel_resolvesMediaMimeTypesAccurately() {
        assertEquals(
            MimeTypes.APPLICATION_M3U8,
            PlayerViewModel.resolveMediaMimeType("https://cdn.example.com/stream/master.m3u8?token=abc")
        )
        assertEquals(
            MimeTypes.APPLICATION_MPD,
            PlayerViewModel.resolveMediaMimeType("https://cdn.example.com/dash/manifest.mpd")
        )
        assertEquals(
            MimeTypes.VIDEO_WEBM,
            PlayerViewModel.resolveMediaMimeType("https://archive.org/download/item/video.webm")
        )
        assertEquals(
            MimeTypes.VIDEO_MATROSKA,
            PlayerViewModel.resolveMediaMimeType("content://media/external/video/episode_01.mkv")
        )
        assertEquals(
            MimeTypes.VIDEO_MP4,
            PlayerViewModel.resolveMediaMimeType("https://archive.org/download/item/movie.mp4")
        )
        // Generic content:// URI without file extension should return null so Media3 sniffs ContentResolver
        assertNull(
            PlayerViewModel.resolveMediaMimeType("content://com.android.providers.media.documents/document/video%3A42")
        )
        // Explicit mimeType from ContentResolver should be respected
        assertEquals(
            MimeTypes.VIDEO_MATROSKA,
            PlayerViewModel.resolveMediaMimeType(
                "content://com.android.providers.media.documents/document/video%3A42",
                "video/x-matroska"
            )
        )
    }

    @Test
    fun playerViewModel_mapsPlaybackErrorsToClearMessages() {
        val codecError = PlaybackException(
            "Decoder init failed",
            null,
            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED
        )
        val codecMsg = PlayerViewModel.mapPlaybackErrorToUserMessage(codecError, isLocalVideo = true)
        assertTrue(codecMsg.contains("Unsupported video/audio codec", ignoreCase = true))

        val containerError = PlaybackException(
            "Container unsupported",
            null,
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED
        )
        val containerMsg = PlayerViewModel.mapPlaybackErrorToUserMessage(containerError, isLocalVideo = true)
        assertTrue(containerMsg.contains("Unsupported media container format", ignoreCase = true))

        val missingLocalError = PlaybackException(
            "File not found",
            null,
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND
        )
        val missingMsg = PlayerViewModel.mapPlaybackErrorToUserMessage(missingLocalError, isLocalVideo = true)
        assertTrue(missingMsg.contains("Local video file was not found", ignoreCase = true))
    }
}
