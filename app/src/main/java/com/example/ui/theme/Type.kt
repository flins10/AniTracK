package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val CormorantSerifFamily = FontFamily(
    Font(R.font.cormorant_garamond, FontWeight.Normal),
    Font(R.font.cormorant_garamond, FontWeight.Medium),
    Font(R.font.cormorant_garamond, FontWeight.SemiBold),
    Font(R.font.cormorant_garamond, FontWeight.Bold)
)

val InterUiFamily = FontFamily(
    Font(R.font.inter, FontWeight.Light),
    Font(R.font.inter, FontWeight.Normal),
    Font(R.font.inter, FontWeight.Medium),
    Font(R.font.inter, FontWeight.SemiBold),
    Font(R.font.inter, FontWeight.Bold),
    Font(R.font.inter, FontWeight.ExtraBold)
)

// Backward-compatible aliases
val ManropeHeadingFamily = CormorantSerifFamily
val OutfitDisplayFamily = CormorantSerifFamily
val PlusJakartaBodyFamily = InterUiFamily

// Editorial Cormorant serif numeral styles with tabular alignment
val CormorantDisplayNumeral = TextStyle(
    fontFamily = CormorantSerifFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 44.sp,
    lineHeight = 46.sp,
    letterSpacing = (-1.2).sp,
    fontFeatureSettings = "tnum"
)

val TabularNumLarge = TextStyle(
    fontFamily = InterUiFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 20.sp,
    lineHeight = 24.sp,
    fontFeatureSettings = "tnum"
)

val TabularNumMedium = TextStyle(
    fontFamily = InterUiFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 15.sp,
    lineHeight = 20.sp,
    fontFeatureSettings = "tnum"
)

val TabularNumSmall = TextStyle(
    fontFamily = InterUiFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 11.5.sp,
    lineHeight = 15.sp,
    fontFeatureSettings = "tnum"
)

val MicroUpperLabel = TextStyle(
    fontFamily = InterUiFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 9.sp,
    lineHeight = 12.sp,
    letterSpacing = 1.8.sp
)

// Legacy aliases
val TabularMonoLarge = TabularNumLarge
val TabularMonoMedium = TabularNumMedium
val TabularMonoSmall = TabularNumSmall

val Typography = Typography(
    displaySmall = TextStyle(
        fontFamily = CormorantSerifFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 34.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.8).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = CormorantSerifFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = CormorantSerifFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = CormorantSerifFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.3).sp
    ),
    titleLarge = TextStyle(
        fontFamily = CormorantSerifFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp,
        lineHeight = 25.sp,
        letterSpacing = (-0.2).sp
    ),
    titleMedium = TextStyle(
        fontFamily = InterUiFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 21.sp
    ),
    titleSmall = TextStyle(
        fontFamily = InterUiFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.5.sp,
        lineHeight = 18.5.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = InterUiFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = InterUiFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp
    ),
    bodySmall = TextStyle(
        fontFamily = InterUiFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.5.sp,
        lineHeight = 16.5.sp
    ),
    labelLarge = TextStyle(
        fontFamily = InterUiFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.5.sp,
        lineHeight = 17.sp
    ),
    labelMedium = TextStyle(
        fontFamily = InterUiFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.3.sp
    ),
    labelSmall = TextStyle(
        fontFamily = InterUiFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 9.5.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.8.sp
    )
)
