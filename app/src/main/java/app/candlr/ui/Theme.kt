package app.candlr.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.candlr.data.Preferences

private val Paper =
    lightColorScheme(
        primary = Color(0xFF62694B),
        onPrimary = Color(0xFFF8F6ED),
        primaryContainer = Color(0xFFE1E4D3),
        onPrimaryContainer = Color(0xFF373D27),
        background = Color(0xFFF2F0E3),
        onBackground = Color(0xFF2E2E2E),
        surface = Color(0xFFF2F0E3),
        onSurface = Color(0xFF2E2E2E),
        surfaceVariant = Color(0xFFE8E5D8),
        onSurfaceVariant = Color(0xFF666457),
        surfaceContainer = Color(0xFFECEADE),
        surfaceContainerHigh = Color(0xFFE8E5D8),
        surfaceContainerHighest = Color(0xFFE1DED1),
        surfaceContainerLow = Color(0xFFF7F5EA),
        surfaceContainerLowest = Color(0xFFFAF8EF),
        surfaceBright = Color(0xFFF7F5EA),
        surfaceDim = Color(0xFFE1DED1),
        surfaceTint = Color(0xFF62694B),
        inverseSurface = Color(0xFF2E2E2E),
        inverseOnSurface = Color(0xFFF2F0E3),
        inversePrimary = Color(0xFFBEC69F),
        outline = Color(0xFF7A796B),
        outlineVariant = Color(0xFFD5D2C5),
        secondaryContainer = Color(0xFFE1E4D3),
        secondary = Color(0xFF62694B),
        onSecondary = Color(0xFFF8F6ED),
        onSecondaryContainer = Color(0xFF373D27),
        error = Color(0xFFA33C34),
        onError = Color.White,
    )
private val Evening =
    darkColorScheme(
        primary = Color(0xFFBEC69F),
        onPrimary = Color(0xFF292F1E),
        primaryContainer = Color(0xFF363D2B),
        onPrimaryContainer = Color(0xFFD9DFC4),
        background = Color(0xFF1F1F1F),
        onBackground = Color(0xFFD1CFC0),
        surface = Color(0xFF1F1F1F),
        onSurface = Color(0xFFD1CFC0),
        surfaceVariant = Color(0xFF292925),
        onSurfaceVariant = Color(0xFFAAA899),
        surfaceContainer = Color(0xFF252522),
        surfaceContainerHigh = Color(0xFF30302B),
        surfaceContainerHighest = Color(0xFF383830),
        surfaceContainerLow = Color(0xFF242421),
        surfaceContainerLowest = Color(0xFF191917),
        surfaceBright = Color(0xFF383830),
        surfaceDim = Color(0xFF1F1F1F),
        surfaceTint = Color(0xFFBEC69F),
        inverseSurface = Color(0xFFD1CFC0),
        inverseOnSurface = Color(0xFF1F1F1F),
        inversePrimary = Color(0xFF62694B),
        outline = Color(0xFF949383),
        outlineVariant = Color(0xFF414139),
        secondaryContainer = Color(0xFF363D2B),
        secondary = Color(0xFFBEC69F),
        onSecondary = Color(0xFF292F1E),
        onSecondaryContainer = Color(0xFFD9DFC4),
        error = Color(0xFFF2A59C),
        onError = Color(0xFF501810),
    )
val BookSerif = FontFamily.Serif
private val BookType =
    Typography(
        displayLarge =
            TextStyle(
                fontFamily = BookSerif,
                fontSize = 48.sp,
                lineHeight = 54.sp,
                letterSpacing = (-1.5).sp,
            ),
        headlineLarge =
            TextStyle(
                fontFamily = BookSerif,
                fontSize = 36.sp,
                lineHeight = 42.sp,
                letterSpacing = (-0.6).sp,
            ),
        headlineMedium = TextStyle(fontFamily = BookSerif, fontSize = 28.sp, lineHeight = 34.sp),
        titleLarge = TextStyle(fontFamily = BookSerif, fontSize = 23.sp, lineHeight = 30.sp),
        titleMedium =
            TextStyle(fontSize = 17.sp, lineHeight = 25.sp, fontWeight = FontWeight.Medium),
        bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 25.sp),
        bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 22.sp),
        labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    )

@Composable
fun CandlrTheme(preferences: Preferences, content: @Composable () -> Unit) {
    val dark = preferences.theme == "dark" || preferences.theme == "system" && isSystemInDarkTheme()
    CompositionLocalProvider(
        LocalReduceMotion provides (preferences.reduceMotion || systemMotionDisabled())
    ) {
        MaterialTheme(
            colorScheme = if (dark) Evening else Paper,
            typography = BookType,
            content = content,
        )
    }
}
