package com.hd.hdmobilepos.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val HyundaiGreen = Color(0xFF005645)
private val HyundaiBrown = Color(0xFFC1A57A)
private val HyundaiBg = Color(0xFFF8F5EE)

private val LightColors = lightColorScheme(
    primary = HyundaiGreen,
    secondary = HyundaiBrown,
    tertiary = Color(0xFF7E6A43),
    background = HyundaiBg,
    surface = Color(0xFFFFFCF6),
    onPrimary = Color.White,
    onSecondary = Color(0xFF1F1A12),
    onBackground = Color(0xFF1C1C1C),
    onSurface = Color(0xFF1C1C1C)
)

private val DarkColors = darkColorScheme(
    primary = HyundaiGreen,
    secondary = HyundaiBrown,
    tertiary = Color(0xFFD9C29E)
)

@Composable
fun PPOSTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
