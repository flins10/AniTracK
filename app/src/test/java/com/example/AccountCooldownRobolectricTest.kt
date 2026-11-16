package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.auth.SupabaseAuthService
import com.example.model.AuthSession
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AccountCooldownRobolectricTest {

    @Test
    fun `deleted account enforces 5 minute email and 24 hour username cooldowns across clearSession`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val authService = SupabaseAuthService(context)

        authService.saveSession(
            AuthSession(
                accessToken = "test_access",
                refreshToken = "test_refresh",
                userId = "user-123",
                email = "WsLogin69@Gmail.com",
                expiresAtEpochSec = 9_999_999_999L,
                onboardingCompleted = true
            )
        )

        val deletedAtMs = 1_000_000L
        authService.recordDeletionCooldowns(
            email = "WsLogin69@Gmail.com",
            username = "Lauma",
            nowMs = deletedAtMs
        )
        authService.clearSession()

        // Session is cleared while deletion cooldowns remain intact
        assertNull(authService.getSavedSession())

        // At 4 minutes: email still blocked (~60s left), username still blocked
        val atFourMinutes = deletedAtMs + 4L * 60_000L
        val emailWaitAtFour = authService.checkLocalEmailCooldownSeconds("wslogin69@gmail.com", nowMs = atFourMinutes)
        assertTrue(emailWaitAtFour in 59L..60L)
        assertTrue(authService.checkLocalUsernameCooldownSeconds("lauma", nowMs = atFourMinutes) > 0L)

        // At 5 minutes: email re-registration is allowed, username still blocked until 24 hours
        val atFiveMinutes = deletedAtMs + 5L * 60_000L
        assertEquals(0L, authService.checkLocalEmailCooldownSeconds("wslogin69@gmail.com", nowMs = atFiveMinutes))
        assertTrue(authService.checkLocalUsernameCooldownSeconds("lauma", nowMs = atFiveMinutes) > 0L)

        // At 23 hours: username is still locked
        val atTwentyThreeHours = deletedAtMs + 23L * 60L * 60_000L
        assertTrue(authService.checkLocalUsernameCooldownSeconds("LAUMA", nowMs = atTwentyThreeHours) > 0L)

        // At 24 hours: username is unlocked again
        val atTwentyFourHours = deletedAtMs + 24L * 60L * 60_000L
        assertEquals(0L, authService.checkLocalUsernameCooldownSeconds("Lauma", nowMs = atTwentyFourHours))
    }
}
