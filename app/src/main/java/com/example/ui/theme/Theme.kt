package com.example.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val BaseDarkColorScheme = darkColorScheme(
    primary = BentoPrimary,
    onPrimary = BentoOnPrimary,
    primaryContainer = BentoPrimaryContainer,
    onPrimaryContainer = BentoOnPrimaryContainer,
    secondary = BentoSecondary,
    onSecondary = BentoOnSecondary,
    tertiary = BentoAccentRed,
    onTertiary = Color.White,
    background = BentoBackground,
    onBackground = Color(0xFFF8FAFC),
    surface = BentoSurface,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = BentoSurfaceVariant,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = BentoBorder
)

@Composable
fun WeeklyAnimePlannerTheme(
    themeName: String = "LIGHT_BLUE",
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val baseScheme = BaseDarkColorScheme
    val customScheme = when (themeName.uppercase()) {
        "RUBY_RED" -> baseScheme.copy(
            primary = RubyRedPrimary,
            primaryContainer = RubyRedContainer,
            onPrimaryContainer = RubyRedOnContainer
        )
        "EMERALD_GREEN" -> baseScheme.copy(
            primary = EmeraldGreenPrimary,
            primaryContainer = EmeraldGreenContainer,
            onPrimaryContainer = EmeraldGreenOnContainer
        )
        "SUNSET_AMBER" -> baseScheme.copy(
            primary = SunsetAmberPrimary,
            primaryContainer = SunsetAmberContainer,
            onPrimaryContainer = SunsetAmberOnContainer
        )
        "OBSIDIAN" -> baseScheme.copy(
            primary = ObsidianPrimary,
            primaryContainer = ObsidianContainer,
            onPrimaryContainer = ObsidianOnContainer
        )
        else -> baseScheme // LIGHT_BLUE default
    }

    MaterialTheme(
        colorScheme = customScheme,
        typography = Typography,
        content = content
    )
}
