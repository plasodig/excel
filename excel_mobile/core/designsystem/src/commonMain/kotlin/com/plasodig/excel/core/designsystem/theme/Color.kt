package com.plasodig.excel.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Excel brand green family (selaras dengan warna ikon resmi Microsoft Excel).
private val ExcelGreen = Color(0xFF107C41)        // primary — dark Excel green
private val ExcelGreenDeep = Color(0xFF0B5C30)    // emphasis / pressed state
private val ExcelGreenBright = Color(0xFF21A366)  // hover/active accent
private val ExcelGreenSoft = Color(0xFFC8EAD2)    // primaryContainer light
private val ExcelGreenInk = Color(0xFF002912)     // onPrimaryContainer light

// Teal sebagai secondary — complement yang professional, tetap di palet hijau.
private val SpreadsheetTeal = Color(0xFF0F766E)
private val SpreadsheetTealSoft = Color(0xFFB5E5DA)
private val SpreadsheetTealInk = Color(0xFF003731)

// Amber subtle untuk tertiary (warning/highlight chip).
private val WarningAmber = Color(0xFFF2A900)

// Neutral surface dengan rona hijau sangat tipis biar tidak datar putih murni.
private val PaperLight = Color(0xFFF7FBF7)
private val InkDark = Color(0xFF111815)
private val SurfaceVariantLight = Color(0xFFE3EDE6)
private val OnSurfaceVariantLight = Color(0xFF445349)
private val OutlineLight = Color(0xFF748A7E)

val LightColors = lightColorScheme(
    primary = ExcelGreen,
    onPrimary = Color.White,
    primaryContainer = ExcelGreenSoft,
    onPrimaryContainer = ExcelGreenInk,
    secondary = SpreadsheetTeal,
    onSecondary = Color.White,
    secondaryContainer = SpreadsheetTealSoft,
    onSecondaryContainer = SpreadsheetTealInk,
    tertiary = WarningAmber,
    onTertiary = InkDark,
    background = PaperLight,
    onBackground = InkDark,
    surface = Color.White,
    onSurface = InkDark,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    outline = OutlineLight,
)

val DarkColors = darkColorScheme(
    primary = Color(0xFF6FD49B),
    onPrimary = Color(0xFF00391C),
    primaryContainer = ExcelGreenBright,
    onPrimaryContainer = ExcelGreenSoft,
    secondary = Color(0xFF8DCFC3),
    onSecondary = Color(0xFF003731),
    secondaryContainer = SpreadsheetTeal,
    onSecondaryContainer = SpreadsheetTealSoft,
    tertiary = Color(0xFFFFCA66),
    onTertiary = InkDark,
    background = Color(0xFF0E1A12),
    onBackground = Color(0xFFE3EDE6),
    surface = Color(0xFF16241B),
    onSurface = Color(0xFFE3EDE6),
    surfaceVariant = Color(0xFF2D3A32),
    onSurfaceVariant = Color(0xFFC4D2C9),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    outline = Color(0xFF8FA398),
)
