package com.plasodig.excel.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

private val BrandOrange = Color(0xFFE85D04)
private val BrandOrangeDark = Color(0xFFDC4C02)
private val BrandAmber = Color(0xFFFFB703)
private val LeafGreen = Color(0xFF2A9D8F)
private val CreamBg = Color(0xFFFFFBF5)
private val InkDark = Color(0xFF1B1B1B)

val LightColors = lightColorScheme(
    primary = BrandOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE0CC),
    onPrimaryContainer = BrandOrangeDark,
    secondary = LeafGreen,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB7E4D9),
    onSecondaryContainer = Color(0xFF0F5248),
    tertiary = BrandAmber,
    onTertiary = InkDark,
    background = CreamBg,
    onBackground = InkDark,
    surface = Color.White,
    onSurface = InkDark,
    surfaceVariant = Color(0xFFF2EAE0),
    onSurfaceVariant = Color(0xFF55493B),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    outline = Color(0xFF8A7D6E),
)

val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB68C),
    onPrimary = Color(0xFF531F00),
    primaryContainer = BrandOrangeDark,
    onPrimaryContainer = Color(0xFFFFE0CC),
    secondary = Color(0xFF8DCFC3),
    onSecondary = Color(0xFF003731),
    secondaryContainer = Color(0xFF0F5248),
    onSecondaryContainer = Color(0xFFB7E4D9),
    tertiary = Color(0xFFFFCA66),
    onTertiary = InkDark,
    background = Color(0xFF181210),
    onBackground = Color(0xFFEDE7DE),
    surface = Color(0xFF231B17),
    onSurface = Color(0xFFEDE7DE),
    surfaceVariant = Color(0xFF3A322B),
    onSurfaceVariant = Color(0xFFD6C9B9),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    outline = Color(0xFF9E907F),
)
