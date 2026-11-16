package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.model.ThemeModePreference

val LocalAppColors = compositionLocalOf { DarkAppColors }
val LocalKinetoscopeIsDark = compositionLocalOf { true }
val LocalDynamicAccentColor = compositionLocalOf { ChampagneGold }

private val ObsidianGoldDarkScheme = darkColorScheme(
    primary = ChampagneGold,
    onPrimary = ChampagneOnGold,
    primaryContainer = ChampagneGoldDeep,
    onPrimaryContainer = ChampagneGoldSoft,
    secondary = ChampagneGoldBright,
    onSecondary = ChampagneOnGold,
    secondaryContainer = ObsidianSurface2,
    onSecondaryContainer = IvoryText,
    tertiary = ChampagneGoldSoft,
    onTertiary = ObsidianBlack,
    background = ObsidianBlack,
    onBackground = IvoryText,
    surface = ObsidianSurface,
    onSurface = IvoryText,
    surfaceVariant = ObsidianSurface2,
    onSurfaceVariant = AshText,
    surfaceContainer = ObsidianSurface2,
    surfaceContainerHigh = ObsidianSurface3,
    surfaceContainerLow = ObsidianSurface,
    outline = HairlineSubtle,
    outlineVariant = HairlineStrong,
    error = ObsidianCrimson,
    onError = Color.White
)

private val WarmIvoryLightScheme = lightColorScheme(
    primary = LightBronzeGold,
    onPrimary = Color.White,
    primaryContainer = LightChampagneTint,
    onPrimaryContainer = LightEspressoInk,
    secondary = LightBronzeSoft,
    onSecondary = Color.White,
    secondaryContainer = LightWarmParchment,
    onSecondaryContainer = LightEspressoInk,
    tertiary = LightBronzeGold,
    onTertiary = Color.White,
    background = LightIvoryCanvas,
    onBackground = LightEspressoInk,
    surface = LightWarmParchment,
    onSurface = LightEspressoInk,
    surfaceVariant = LightElevatedSand,
    onSurfaceVariant = LightMutedUmber,
    surfaceContainer = LightPorcelainCard,
    surfaceContainerHigh = LightElevatedSand,
    surfaceContainerLow = LightWarmParchment,
    outline = LightHairlineBorder,
    outlineVariant = LightHairlineBorder,
    error = LightCrimson,
    onError = Color.White
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(AppRadius.xs),
    small = RoundedCornerShape(AppRadius.sm),
    medium = RoundedCornerShape(AppRadius.md),
    large = RoundedCornerShape(AppRadius.lg),
    extraLarge = RoundedCornerShape(AppRadius.xl)
)

object AppTheme {
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current
}

@Composable
fun AnimeUniverseTheme(
    themeMode: ThemeModePreference = ThemeModePreference.DARK,
    accentTheme: String = "CHAMPAGNE_GOLD",
    dynamicPosterAccent: Color? = null,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeModePreference.SYSTEM -> isSystemInDarkTheme()
        ThemeModePreference.DARK -> true
        ThemeModePreference.LIGHT -> false
    }

    val baseColors = if (isDark) DarkAppColors else LightAppColors
    val baseScheme = if (isDark) ObsidianGoldDarkScheme else WarmIvoryLightScheme

    // Default to Champagne Gold; allow explicit user accent override from Settings
    val resolvedPrimary = when (accentTheme.uppercase()) {
        "SAKURA_PINK" -> if (isDark) Color(0xFFEB9AC0) else Color(0xFFAD2E75)
        "EMERALD_MINT" -> if (isDark) ObsidianEmerald else LightEmerald
        "INDIGO_VIOLET" -> if (isDark) Color(0xFF9C8FFF) else Color(0xFF4F3ED4)
        else -> baseColors.accentPrimary
    }

    val resolvedSecondary = when (accentTheme.uppercase()) {
        "SAKURA_PINK" -> if (isDark) Color(0xFFF5BFE0) else Color(0xFF8C215C)
        "EMERALD_MINT" -> if (isDark) Color(0xFF9BEFC3) else Color(0xFF0E613E)
        "INDIGO_VIOLET" -> if (isDark) Color(0xFFC0B6FF) else Color(0xFF3E2EB8)
        else -> baseColors.accentSecondary
    }

    val resolvedHighlight = when (accentTheme.uppercase()) {
        "SAKURA_PINK" -> if (isDark) Color(0xFFFCE0F0) else Color(0xFF851D57)
        "EMERALD_MINT" -> if (isDark) Color(0xFFC2F7DC) else Color(0xFF0D5939)
        "INDIGO_VIOLET" -> if (isDark) Color(0xFFE0DAFF) else Color(0xFF3B2AB0)
        else -> baseColors.highlight
    }

    val appColors = baseColors.copy(
        accentPrimary = resolvedPrimary,
        accentSecondary = resolvedSecondary,
        highlight = resolvedHighlight
    )
    val finalScheme = baseScheme.copy(
        primary = resolvedPrimary,
        secondary = resolvedSecondary
    )

    CompositionLocalProvider(
        LocalAppColors provides appColors,
        LocalKinetoscopeIsDark provides isDark,
        LocalDynamicAccentColor provides resolvedPrimary
    ) {
        MaterialTheme(
            colorScheme = finalScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}
