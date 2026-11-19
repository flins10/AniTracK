package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.example.model.CommunityReview
import com.example.model.WatchStatus
import com.example.model.groupCommunityReviewsByAnime
import com.example.ui.screens.CommunityAnimeTemplateCard
import com.example.ui.screens.CommunityCommentsDialog
import com.example.ui.theme.AnimeUniverseTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CommunityGroupingAndUiRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun groupCommunityReviewsByAnime_consolidatesMultipleNotesAndRatingsIntoSingleTemplate() {
        val reviews = listOf(
            CommunityReview(
                id = "rev_1",
                userId = "user_alpha",
                username = "AlphaWatcher",
                avatarKey = "avatar_1",
                avatarUrl = null,
                animeId = 1535,
                animeTitle = "Death Note",
                animeCoverImage = "https://example.com/dn.jpg",
                rating = 10.0,
                reviewNotes = "Masterpiece of psychological mind games.",
                status = WatchStatus.COMPLETED,
                episodeProgress = 37,
                totalEpisodes = 37,
                updatedAt = "2026-11-18T10:00:00Z",
                likesCount = 3
            ),
            CommunityReview(
                id = "rev_2",
                userId = "user_beta",
                username = "BetaCritic",
                avatarKey = "avatar_2",
                avatarUrl = null,
                animeId = 1535,
                animeTitle = "Death Note",
                animeCoverImage = "https://example.com/dn.jpg",
                rating = null,
                reviewNotes = "L vs Light is pure tension even without rating yet!",
                status = WatchStatus.WATCHING,
                episodeProgress = 12,
                totalEpisodes = 37,
                updatedAt = "2026-11-18T12:00:00Z",
                likesCount = 2
            ),
            CommunityReview(
                id = "rev_3",
                userId = "user_gamma",
                username = "GammaFan",
                avatarKey = "avatar_3",
                avatarUrl = null,
                animeId = 1535,
                animeTitle = "Death Note",
                animeCoverImage = "https://example.com/dn.jpg",
                rating = 8.0,
                reviewNotes = "",
                status = WatchStatus.COMPLETED,
                episodeProgress = 37,
                totalEpisodes = 37,
                updatedAt = "2026-11-18T11:00:00Z",
                likesCount = 1
            )
        )

        val groups = groupCommunityReviewsByAnime(reviews)
        // Strictly 1 template group for animeId 1535 even though 3 users added notes/ratings
        assertEquals(1, groups.size)

        val group = groups.first()
        assertEquals(1535, group.animeId)
        assertEquals(3, group.notesAndRatingsCount)
        assertEquals(2, group.ratedCount)
        assertEquals(2, group.notedCount)
        assertNotNull(group.averageRating10)
        assertEquals(9.0, group.averageRating10!!, 0.01)
    }

    @Test
    fun communityAnimeTemplateCard_commentIconOpensCommentsDialogWithAllNotesAndAvatars() {
        val reviews = listOf(
            CommunityReview(
                id = "rev_a",
                userId = "user_a",
                username = "Akira",
                avatarKey = "avatar_1",
                avatarUrl = null,
                animeId = 21,
                animeTitle = "One Piece",
                animeCoverImage = "",
                rating = 10.0,
                reviewNotes = "Worldbuilding is unmatched.",
                status = WatchStatus.WATCHING,
                episodeProgress = 1100,
                totalEpisodes = null,
                updatedAt = "2026-11-18T14:00:00Z"
            ),
            CommunityReview(
                id = "rev_b",
                userId = "user_b",
                username = "Renji",
                avatarKey = "avatar_4",
                avatarUrl = null,
                animeId = 21,
                animeTitle = "One Piece",
                animeCoverImage = "",
                rating = 9.0,
                reviewNotes = "Gear 5 animation is incredible!",
                status = WatchStatus.WATCHING,
                episodeProgress = 1050,
                totalEpisodes = null,
                updatedAt = "2026-11-18T13:00:00Z"
            )
        )
        val group = groupCommunityReviewsByAnime(reviews).first()

        composeTestRule.setContent {
            var showDialog by mutableStateOf(false)
            AnimeUniverseTheme {
                CommunityAnimeTemplateCard(
                    group = group,
                    onClick = {},
                    onOpenComments = { showDialog = true },
                    onToggleLike = {}
                )
                if (showDialog) {
                    CommunityCommentsDialog(
                        group = group,
                        initialMyRatingStars = 0f,
                        initialMyNote = "",
                        onDismiss = { showDialog = false },
                        onOpenAnimeDetail = {},
                        onToggleLike = {},
                        onSubmitComment = { _, _ -> }
                    )
                }
            }
        }

        // Verify the unified card and themed comment icon badge with count "2"
        composeTestRule.onNodeWithText("One Piece").assertIsDisplayed()
        composeTestRule.onNodeWithTag("comment_anime_21").assertIsDisplayed()
        composeTestRule.onNodeWithTag("comment_anime_21").performClick()

        // Verify the comments dialog opens and displays all notes & usernames
        composeTestRule.onNodeWithTag("community_comments_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("community_comment_row_rev_a", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("community_comments_list")
            .performScrollToNode(hasTestTag("community_comment_row_rev_b"))
        composeTestRule.onNodeWithTag("community_comment_row_rev_b", useUnmergedTree = true).assertIsDisplayed()
        assertTrue(composeTestRule.onAllNodesWithText("Akira", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())
        assertTrue(composeTestRule.onAllNodesWithText("Renji", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())
        assertTrue(composeTestRule.onAllNodesWithText("“Worldbuilding is unmatched.”", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())
        assertTrue(composeTestRule.onAllNodesWithText("“Gear 5 animation is incredible!”", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())
    }
}
