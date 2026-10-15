package com.xiaokuaile.xin.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Xin 的主色是一种偏青的绿（“信”青），和 Pin 的红色区分开。
private val XinTeal = Color(0xFF13A387)
private val XinTealDark = Color(0xFF4FD6B6)

private val LightColors = lightColorScheme(
    primary = XinTeal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDF3E8),
    onPrimaryContainer = Color(0xFF00382C),
    secondary = Color(0xFF52635D),
    background = Color(0xFFF7F8F7),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEEF1EF),
    onSurfaceVariant = Color(0xFF5E6663),
    outlineVariant = Color(0xFFDCE1DE),
    error = Color(0xFFD64545),
)

private val DarkColors = darkColorScheme(
    primary = XinTealDark,
    onPrimary = Color(0xFF00382C),
    primaryContainer = Color(0xFF005142),
    onPrimaryContainer = Color(0xFFCDF3E8),
    secondary = Color(0xFFB8CBC4),
    background = Color(0xFF0E1110),
    surface = Color(0xFF151918),
    surfaceVariant = Color(0xFF222826),
    onSurfaceVariant = Color(0xFFA9B2AE),
    outlineVariant = Color(0xFF2F3633),
    error = Color(0xFFFF8A80),
)

@Composable
fun XinTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
