package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.example.data.local.AnimeEntity
import com.example.model.ThemeModePreference
import com.example.ui.screens.DetailEpisodeStepperControl
import com.example.ui.theme.AnimeUniverseTheme
import com.example.ui.widgets.QuickContextActionSheet
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp-xxhdpi")
class AnimeDetailsAndSheetRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `QuickContextActionSheet shows Remove from List instead of Season when in user list`() {
        val sampleAnime = AnimeEntity(
            id = 4201,
            titleRomaji = "Frieren",
            titleEnglish = "Frieren: Beyond Journey's End",
            coverImage = "",
            totalEpisodes = 28,
            watchedEpisodeCount = 12,
            inWatchlist = true
        )

        composeTestRule.setContent {
            AnimeUniverseTheme(themeMode = ThemeModePreference.DARK) {
                QuickContextActionSheet(
                    anime = sampleAnime,
                    isInUserList = true,
                    onDismiss = {},
                    onSave = { _, _, _, _, _ -> },
                    onRemove = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("context_sheet_remove").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Remove from List").assertIsDisplayed()
        composeTestRule.onNodeWithTag("context_episode_input").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("context_decrement_ep").assertIsDisplayed()
        composeTestRule.onNodeWithTag("context_increment_ep").assertIsDisplayed()

        // Season stepper/input must no longer exist in long-press sheet
        composeTestRule.onNodeWithTag("context_season_input").assertDoesNotExist()
        composeTestRule.onNodeWithText("SEASON").assertDoesNotExist()
    }

    @Test
    fun `QuickContextActionSheet hides Remove from List for unadded catalog anime and omits Season`() {
        val unaddedAnime = AnimeEntity(
            id = 4202,
            titleRomaji = "Dandadan",
            titleEnglish = "DAN DA DAN",
            coverImage = "",
            totalEpisodes = 12,
            watchedEpisodeCount = 0,
            inWatchlist = false
        )

        composeTestRule.setContent {
            AnimeUniverseTheme(themeMode = ThemeModePreference.DARK) {
                QuickContextActionSheet(
                    anime = unaddedAnime,
                    isInUserList = false,
                    onDismiss = {},
                    onSave = { _, _, _, _, _ -> },
                    onRemove = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("context_sheet_remove").assertDoesNotExist()
        composeTestRule.onNodeWithTag("context_season_input").assertDoesNotExist()
        composeTestRule.onNodeWithText("SEASON").assertDoesNotExist()
        composeTestRule.onNodeWithTag("context_episode_input").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `DetailEpisodeStepperControl removes inline Type episode row and opens custom dialog when middle episode is clicked`() {
        var appliedEpisode: Int? = null

        composeTestRule.setContent {
            AnimeUniverseTheme(themeMode = ThemeModePreference.DARK) {
                DetailEpisodeStepperControl(
                    watchedEpisodeCount = 3,
                    totalEpisodes = 25,
                    subtitle = "EPISODES WATCHED",
                    canDecrement = true,
                    canIncrement = true,
                    onDecrement = {},
                    onIncrement = {},
                    onSetCustomEpisode = { appliedEpisode = it }
                )
            }
        }
        composeTestRule.waitForIdle()

        // Inline "Type episode #" and "SET EP" are gone; middle stepper counter is displayed
        composeTestRule.onNodeWithText("Type episode #").assertDoesNotExist()
        composeTestRule.onNodeWithText("SET EP").assertDoesNotExist()
        composeTestRule.onNodeWithTag("detail_custom_episode_dialog").assertDoesNotExist()
        composeTestRule.onNodeWithTag("detail_decrement_ep").assertIsDisplayed()
        composeTestRule.onNodeWithTag("detail_increment_ep").assertIsDisplayed()
        composeTestRule.onNodeWithTag("detail_episode_center").assertIsDisplayed()

        // Click middle of - and + -> opens Set Episode dialog
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.onNodeWithTag("detail_episode_center").performClick()
        composeTestRule.mainClock.advanceTimeBy(300L)

        composeTestRule.onNodeWithTag("detail_custom_episode_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("detail_episode_input").performTextReplacement("18")
        composeTestRule.mainClock.advanceTimeBy(150L)
        composeTestRule.onNodeWithTag("detail_set_episode_button").performClick()
        composeTestRule.mainClock.advanceTimeBy(300L)
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()

        assertEquals(18, appliedEpisode)
        composeTestRule.onNodeWithTag("detail_custom_episode_dialog").assertDoesNotExist()
    }
}
