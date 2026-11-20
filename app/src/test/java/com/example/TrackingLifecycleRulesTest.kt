package com.example

import com.example.data.local.AnimeEntity
import com.example.model.EpisodeInputValidation
import com.example.model.ProgressRules
import com.example.model.WatchStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying Parts 1–5 across Anime Universe:
 * - Part 1: Independent Home, Library, and Community membership
 * - Part 2: Strict Automatic Anime Completion (official FINISHED + known total + watched >= total)
 * - Part 3: Dropped anime auto-removal from Home while preserving Library/progress/notes/ratings
 * - Part 4: Remove from Home without affecting Library or Community data
 * - Part 5: Custom episode number input validation & status preservation
 */
class TrackingLifecycleRulesTest {

    // -------------------------------------------------------------------------
    // PART 2: AUTOMATIC ANIME COMPLETION (Spec Table Examples A–E + Edge Cases)
    // -------------------------------------------------------------------------

    @Test
    fun animeA_finished_12of12_autoCompletesAndRemovesFromHome() {
        assertTrue(ProgressRules.shouldAutoComplete("FINISHED", totalEpisodes = 12, watchedEpisodes = 12))
        assertTrue(ProgressRules.shouldRemoveFromHome(WatchStatus.WATCHING, "FINISHED", 12, 12))
    }

    @Test
    fun animeB_finished_15of24_staysOnHome() {
        assertFalse(ProgressRules.shouldAutoComplete("FINISHED", totalEpisodes = 24, watchedEpisodes = 15))
        assertFalse(ProgressRules.shouldRemoveFromHome(WatchStatus.WATCHING, "FINISHED", 24, 15))
    }

    @Test
    fun animeC_ongoing_12of12_neverAutoCompletesAndStaysOnHome() {
        assertFalse(ProgressRules.shouldAutoComplete("RELEASING", totalEpisodes = 12, watchedEpisodes = 12))
        assertFalse(ProgressRules.shouldRemoveFromHome(WatchStatus.WATCHING, "RELEASING", 12, 12))
    }

    @Test
    fun animeD_finished_11of12_staysOnHome() {
        assertFalse(ProgressRules.shouldAutoComplete("FINISHED", totalEpisodes = 12, watchedEpisodes = 11))
        assertFalse(ProgressRules.shouldRemoveFromHome(WatchStatus.WATCHING, "FINISHED", 12, 11))
    }

    @Test
    fun animeE_finished_24of24_autoCompletesAndRemovesFromHome() {
        assertTrue(ProgressRules.shouldAutoComplete("FINISHED_AIRING", totalEpisodes = 24, watchedEpisodes = 24))
        assertTrue(ProgressRules.shouldRemoveFromHome(WatchStatus.WATCHING, "FINISHED_AIRING", 24, 24))
    }

    @Test
    fun unknownTotalEpisodes_neverAutoCompletesEvenIfFinished() {
        assertFalse(ProgressRules.shouldAutoComplete("FINISHED", totalEpisodes = null, watchedEpisodes = 1150))
        assertFalse(ProgressRules.shouldAutoComplete("FINISHED", totalEpisodes = 0, watchedEpisodes = 100))
        assertFalse(ProgressRules.shouldRemoveFromHome(WatchStatus.WATCHING, "FINISHED", null, 1150))
    }

    @Test
    fun incrementEpisode_ongoingAtLastReleasedEpisode_doesNotAutoComplete() {
        val (nextEp, autoCompleted) = ProgressRules.incrementEpisode(
            current = 11,
            totalEpisodes = 12,
            officialStatus = "RELEASING"
        )
        assertEquals(12, nextEp)
        assertFalse(autoCompleted)
    }

    @Test
    fun incrementEpisode_finishedAtFinalEpisode_triggersAutoComplete() {
        val (nextEp, autoCompleted) = ProgressRules.incrementEpisode(
            current = 11,
            totalEpisodes = 12,
            officialStatus = "FINISHED"
        )
        assertEquals(12, nextEp)
        assertTrue(autoCompleted)
    }

    // -------------------------------------------------------------------------
    // PART 3: DROPPED ANIME
    // -------------------------------------------------------------------------

    @Test
    fun droppedAnime_removesFromHomeWhilePreservingLibraryProgressNotesAndRating() {
        val tracked = AnimeEntity(
            id = 21,
            titleEnglish = "One Piece",
            titleRomaji = "ONE PIECE",
            coverImage = "",
            status = "RELEASING",
            totalEpisodes = 1150,
            inWatchlist = true,
            inHome = true,
            userStatus = WatchStatus.WATCHING.dbValue,
            watchedEpisodeCount = 850,
            userScore = 90,
            userNotes = "Egghead arc is peak"
        )

        val shouldRemove = ProgressRules.shouldRemoveFromHome(
            watchStatus = WatchStatus.DROPPED,
            officialStatus = tracked.status,
            totalEpisodes = tracked.totalEpisodes,
            watchedEpisodes = tracked.watchedEpisodeCount
        )
        assertTrue(shouldRemove)

        val afterDrop = tracked.copy(
            userStatus = WatchStatus.DROPPED.dbValue,
            inHome = !shouldRemove
        )
        assertFalse(afterDrop.inHome)
        assertTrue(afterDrop.inWatchlist)
        assertEquals(WatchStatus.DROPPED, afterDrop.watchStatusEnum)
        assertEquals(850, afterDrop.watchedEpisodeCount)
        assertEquals(90, afterDrop.userScore)
        assertEquals("Egghead arc is peak", afterDrop.userNotes)
    }

    // -------------------------------------------------------------------------
    // PART 1 & PART 4: INDEPENDENT HOME vs LIBRARY
    // -------------------------------------------------------------------------

    @Test
    fun removeFromHomeOnly_preservesLibraryEntryProgressNotesAndRating() {
        val tracked = AnimeEntity(
            id = 154587,
            titleEnglish = "Frieren",
            titleRomaji = "Sousou no Frieren",
            coverImage = "",
            status = "FINISHED",
            totalEpisodes = 28,
            inWatchlist = true,
            inHome = true,
            userStatus = WatchStatus.WATCHING.dbValue,
            watchedEpisodeCount = 14,
            userScore = 100,
            userNotes = "Masterpiece soundtrack"
        )

        val removedFromHomeOnly = tracked.copy(inHome = false)
        assertFalse(removedFromHomeOnly.inHome)
        assertTrue(removedFromHomeOnly.inWatchlist)
        assertEquals(WatchStatus.WATCHING, removedFromHomeOnly.watchStatusEnum)
        assertEquals(14, removedFromHomeOnly.watchedEpisodeCount)
        assertEquals(100, removedFromHomeOnly.userScore)
        assertEquals("Masterpiece soundtrack", removedFromHomeOnly.userNotes)
    }

    @Test
    fun defaultAnimeEntity_isNotInHomeUnlessExplicitlyAdded() {
        val catalogItem = AnimeEntity(
            id = 5114,
            titleEnglish = "Fullmetal Alchemist: Brotherhood",
            titleRomaji = "Hagane no Renkinjutsushi",
            coverImage = ""
        )
        assertFalse(catalogItem.inHome)
        assertFalse(catalogItem.inWatchlist)
    }

    // -------------------------------------------------------------------------
    // PART 5: CUSTOM EPISODE NUMBER INPUT VALIDATION
    // -------------------------------------------------------------------------

    @Test
    fun customEpisodeInput_onePiece_850to900of1150_succeeds() {
        val res = ProgressRules.validateEpisodeInput("900", totalEpisodes = 1150)
        assertTrue(res is EpisodeInputValidation.Valid)
        assertEquals(900, (res as EpisodeInputValidation.Valid).episodes)
    }

    @Test
    fun customEpisodeInput_zeroEpisodes_succeeds() {
        val res = ProgressRules.validateEpisodeInput("0", totalEpisodes = 24)
        assertTrue(res is EpisodeInputValidation.Valid)
        assertEquals(0, (res as EpisodeInputValidation.Valid).episodes)
    }

    @Test
    fun customEpisodeInput_exactTotalEpisodes_succeeds() {
        val res = ProgressRules.validateEpisodeInput("1150", totalEpisodes = 1150)
        assertTrue(res is EpisodeInputValidation.Valid)
        assertEquals(1150, (res as EpisodeInputValidation.Valid).episodes)
    }

    @Test
    fun customEpisodeInput_exceedingKnownTotal_isRejected() {
        val res = ProgressRules.validateEpisodeInput("1151", totalEpisodes = 1150)
        assertTrue(res is EpisodeInputValidation.Invalid)
    }

    @Test
    fun customEpisodeInput_unknownTotal_allowsReasonableLargePositiveNumbers() {
        val res = ProgressRules.validateEpisodeInput("1122", totalEpisodes = null)
        assertTrue(res is EpisodeInputValidation.Valid)
        assertEquals(1122, (res as EpisodeInputValidation.Valid).episodes)
    }

    @Test
    fun customEpisodeInput_negativeDecimalLettersBlank_allRejected() {
        assertTrue(ProgressRules.validateEpisodeInput("-1", 12) is EpisodeInputValidation.Invalid)
        assertTrue(ProgressRules.validateEpisodeInput("12.5", 24) is EpisodeInputValidation.Invalid)
        assertTrue(ProgressRules.validateEpisodeInput("12,5", 24) is EpisodeInputValidation.Invalid)
        assertTrue(ProgressRules.validateEpisodeInput("abc", 24) is EpisodeInputValidation.Invalid)
        assertTrue(ProgressRules.validateEpisodeInput("12a", 24) is EpisodeInputValidation.Invalid)
        assertTrue(ProgressRules.validateEpisodeInput("   ", 24) is EpisodeInputValidation.Invalid)
    }
}
