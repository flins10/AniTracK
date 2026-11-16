package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp-xxhdpi")
class MainActivityUiRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `app launches past splash and displays AuthScreen email or username login without exposed backend keys`() {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.mainClock.advanceTimeBy(3000L)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("auth_email_input").assertIsDisplayed()
        composeTestRule.onNodeWithText("Email or Username").assertIsDisplayed()
        composeTestRule.onNodeWithTag("auth_password_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("auth_submit_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("auth_tab_signup").assertIsDisplayed()
        composeTestRule.onNodeWithTag("local_preview_button").assertIsDisplayed()

        // Backend setup / anon keys must never be exposed in the UI
        composeTestRule.onNodeWithTag("edit_supabase_config_button").assertDoesNotExist()
        composeTestRule.onNodeWithTag("supabase_url_input").assertDoesNotExist()
        composeTestRule.onNodeWithTag("supabase_key_input").assertDoesNotExist()
    }

    @Test
    fun `sign up screen collects email and password only with zero OTP inputs`() {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.mainClock.advanceTimeBy(3000L)
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("auth_tab_signup").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("auth_email_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("auth_password_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("auth_confirm_password_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("auth_submit_button").assertIsDisplayed()

        composeTestRule.onNodeWithTag("auth_otp_input").assertDoesNotExist()
        composeTestRule.onNodeWithTag("auth_verify_otp_button").assertDoesNotExist()
        composeTestRule.onNodeWithTag("onboarding_username_input").assertDoesNotExist()
    }

    @Test
    fun `continuing without account displays HomeScreen with no refresh or settings top buttons`() {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.mainClock.advanceTimeBy(3000L)
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("local_preview_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("home_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_navigation_bar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_search_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_refresh_button").assertDoesNotExist()
        composeTestRule.onNodeWithTag("home_settings_button").assertDoesNotExist()

        composeTestRule.onNodeWithText("ANIME UNIVERSE").assertIsDisplayed()
        composeTestRule.onNodeWithText("This Week").assertIsDisplayed()
        composeTestRule.onNodeWithTag("week_pill_all").assertIsDisplayed()
        composeTestRule.onNodeWithText("All").assertIsDisplayed()
        composeTestRule.onNodeWithText("Trending Now").assertDoesNotExist()
        composeTestRule.onNodeWithText("Community Pulse").assertDoesNotExist()
    }

    @Test
    fun `bottom navigation bar and header profile button navigate across Explore Library Community and Profile`() {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.mainClock.advanceTimeBy(3000L)
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("local_preview_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("nav_item_discover").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("discover_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("discover_search_input").assertIsDisplayed()
        composeTestRule.onNodeWithText("Load More").assertDoesNotExist()

        composeTestRule.onNodeWithTag("nav_item_mylist").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("mylist_screen").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_item_community").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("community_locked_view").assertIsDisplayed()
        composeTestRule.onNodeWithText("Refresh community").assertDoesNotExist()

        composeTestRule.onNodeWithTag("profile_nav_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("profile_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("WATCHED").assertIsDisplayed()
        composeTestRule.onNodeWithText("Taste DNA").assertDoesNotExist()
        composeTestRule.onNodeWithText("RECENT RATINGS & NOTES").assertDoesNotExist()
        composeTestRule.onNodeWithText("APPEARANCE").assertDoesNotExist()
        composeTestRule.onNodeWithText("AVATAR & IDENTITY").assertDoesNotExist()
        composeTestRule.onNodeWithTag("upload_avatar_button").assertDoesNotExist()
        composeTestRule.onNodeWithTag("profile_bio_input").assertDoesNotExist()
        composeTestRule.onNodeWithTag("profile_settings_row_button").assertDoesNotExist()
        composeTestRule.onNodeWithText("Disconnect account").assertDoesNotExist()
    }

    @Test
    fun `home quick search button opens QuickSearchBottomSheet`() {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.mainClock.advanceTimeBy(3000L)
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("local_preview_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("home_search_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("quick_search_input").assertIsDisplayed()
    }

    @Test
    fun `profile top settings button opens category-first SettingsScreen without Privacy Cloud Sync or PFP sections`() {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.mainClock.advanceTimeBy(3000L)
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("local_preview_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("profile_nav_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("profile_settings_button").performClick()
        composeTestRule.waitForIdle()

        // Root shows only categories
        composeTestRule.onNodeWithTag("settings_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_category_account").assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_category_appearance").assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_category_preferences").assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_category_about").assertIsDisplayed()

        // Privacy, Cloud Sync, PFP, and expanded sections are absent on root
        composeTestRule.onNodeWithText("PRIVACY").assertDoesNotExist()
        composeTestRule.onNodeWithText("Privacy").assertDoesNotExist()
        composeTestRule.onNodeWithText("Share Star Ratings & Notes").assertDoesNotExist()
        composeTestRule.onNodeWithText("Share Custom Collections").assertDoesNotExist()
        composeTestRule.onNodeWithText("APPEARANCE").assertDoesNotExist()
        composeTestRule.onNodeWithText("CLOUD SYNC & STORAGE").assertDoesNotExist()
        composeTestRule.onNodeWithText("PROFILE PICTURE").assertDoesNotExist()
        composeTestRule.onNodeWithText("Upload New Photo").assertDoesNotExist()
        composeTestRule.onNodeWithText("COMMUNITY").assertDoesNotExist()
        composeTestRule.onNodeWithTag("supabase_url_input").assertDoesNotExist()
        composeTestRule.onNodeWithTag("supabase_key_input").assertDoesNotExist()

        // Tap Appearance category -> opens Appearance view
        composeTestRule.onNodeWithTag("settings_category_appearance").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("APPEARANCE").assertIsDisplayed()

        // Back returns to categories root
        composeTestRule.onNodeWithTag("settings_back_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_category_account").assertIsDisplayed()

        // Tap Account category -> opens Account view
        composeTestRule.onNodeWithTag("settings_category_account").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("ACCOUNT").assertIsDisplayed()
        composeTestRule.onNodeWithText("Guest mode").assertIsDisplayed()
    }
}
