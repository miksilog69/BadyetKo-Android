package com.badyetko.app

import androidx.compose.runtime.Composable
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun badyetKoColors(theme: AppTheme): ColorScheme = when (theme) {
    AppTheme.CLEAN_WHITE -> lightColorScheme(primary = Color(0xFF2477E8), secondary = Color(0xFF18A97A), background = Color.White, surface = Color(0xFFF7F9FC))
    AppTheme.WARM_MINIMAL -> lightColorScheme(primary = Color(0xFF496B5B), secondary = Color(0xFFB48154), background = Color(0xFFF7F1E8), surface = Color(0xFFFFFBF5))
    AppTheme.PASTEL_FINANCE -> lightColorScheme(primary = Color(0xFF5E74C9), secondary = Color(0xFF44A98A), background = Color(0xFFF1F9F7), surface = Color(0xFFFAFEFD))
    AppTheme.SOFT_FROST -> lightColorScheme(primary = Color(0xFF397BC4), secondary = Color(0xFF3D9B91), background = Color(0xFFEEF6FB), surface = Color(0xFFF8FCFF))
    AppTheme.SOFT_NEUMORPHIC -> lightColorScheme(primary = Color(0xFF4D76A8), secondary = Color(0xFF578D82), background = Color(0xFFE8EDF4), surface = Color(0xFFF0F4F9))
    AppTheme.PIXELATED -> lightColorScheme(primary = Color(0xFF255B42), secondary = Color(0xFF2D8060), background = Color(0xFFEEF8E8), surface = Color(0xFFF5FBEF), outline = Color(0xFF255B42))
    AppTheme.LIQUID_GLASS -> darkColorScheme(primary = Color(0xFF66DAFF), secondary = Color(0xFF62E6AC), background = Color(0xFF08111F), surface = Color(0xFF142038))
}

@Composable
fun badyetKoBackground(theme: AppTheme): Brush = if (theme == AppTheme.LIQUID_GLASS) {
    Brush.linearGradient(listOf(Color(0xFF172553), Color(0xFF08111F), Color(0xFF083B4D)))
} else {
    Brush.linearGradient(listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.background))
}
