package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.fredporciuncula.flow.preferences.FlowSharedPreferences
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper

private val LightColorScheme =
    lightColorScheme(
        primary = md_theme_light_primary,
        onPrimary = md_theme_light_onPrimary,
        primaryContainer = md_theme_light_primaryContainer,
        onPrimaryContainer = md_theme_light_onPrimaryContainer,
        secondary = md_theme_light_secondary,
        onSecondary = md_theme_light_onSecondary,
        secondaryContainer = md_theme_light_secondaryContainer,
        onSecondaryContainer = md_theme_light_onSecondaryContainer,
        tertiary = md_theme_light_tertiary,
        onTertiary = md_theme_light_onTertiary,
        tertiaryContainer = md_theme_light_tertiaryContainer,
        onTertiaryContainer = md_theme_light_onTertiaryContainer,
        error = md_theme_light_error,
        errorContainer = md_theme_light_errorContainer,
        onError = md_theme_light_onError,
        onErrorContainer = md_theme_light_onErrorContainer,
        background = md_theme_light_background,
        onBackground = md_theme_light_onBackground,
        surface = md_theme_light_surface,
        onSurface = md_theme_light_onSurface,
        surfaceVariant = md_theme_light_surfaceVariant,
        onSurfaceVariant = md_theme_light_onSurfaceVariant,
        outline = md_theme_light_outline,
        inverseOnSurface = md_theme_light_inverseOnSurface,
        inverseSurface = md_theme_light_inverseSurface,
        inversePrimary = md_theme_light_inversePrimary,
        surfaceTint = md_theme_light_surfaceTint,
        outlineVariant = md_theme_light_outlineVariant,
        scrim = md_theme_light_scrim,
    )

private val DarkColorScheme =
    darkColorScheme(
        primary = md_theme_dark_primary,
        onPrimary = md_theme_dark_onPrimary,
        primaryContainer = md_theme_dark_primaryContainer,
        onPrimaryContainer = md_theme_dark_onPrimaryContainer,
        secondary = md_theme_dark_secondary,
        onSecondary = md_theme_dark_onSecondary,
        secondaryContainer = md_theme_dark_secondaryContainer,
        onSecondaryContainer = md_theme_dark_onSecondaryContainer,
        tertiary = md_theme_dark_tertiary,
        onTertiary = md_theme_dark_onTertiary,
        tertiaryContainer = md_theme_dark_tertiaryContainer,
        onTertiaryContainer = md_theme_dark_onTertiaryContainer,
        error = md_theme_dark_error,
        errorContainer = md_theme_dark_errorContainer,
        onError = md_theme_dark_onError,
        onErrorContainer = md_theme_dark_onErrorContainer,
        background = md_theme_dark_background,
        onBackground = md_theme_dark_onBackground,
        surface = md_theme_dark_surface,
        onSurface = md_theme_dark_onSurface,
        surfaceVariant = md_theme_dark_surfaceVariant,
        onSurfaceVariant = md_theme_dark_onSurfaceVariant,
        outline = md_theme_dark_outline,
        inverseOnSurface = md_theme_dark_inverseOnSurface,
        inverseSurface = md_theme_dark_inverseSurface,
        inversePrimary = md_theme_dark_inversePrimary,
        surfaceTint = md_theme_dark_surfaceTint,
        outlineVariant = md_theme_dark_outlineVariant,
        scrim = md_theme_dark_scrim,
    )

private fun customAccentScheme(
    accent: String,
    isDark: Boolean,
): ColorScheme {
    val base = if (isDark) DarkColorScheme else LightColorScheme
    return when (accent) {
        "blue" ->
            if (isDark) {
                base.copy(
                    primary = Color(0xFF82B1FF),
                    onPrimary = Color(0xFF002E6B),
                    primaryContainer = Color(0xFF004494),
                    onPrimaryContainer = Color(0xFFD8E2FF),
                    secondary = Color(0xFFBAC6EA),
                    secondaryContainer = Color(0xFF3B4664),
                )
            } else {
                base.copy(
                    primary = Color(0xFF1565C0),
                    onPrimary = Color.White,
                    primaryContainer = Color(0xFFD8E2FF),
                    onPrimaryContainer = Color(0xFF001A41),
                    secondary = Color(0xFF525E7D),
                    secondaryContainer = Color(0xFFDAE2FF),
                )
            }

        "purple" ->
            if (isDark) {
                base.copy(
                    primary = Color(0xFFD0BCFF),
                    onPrimary = Color(0xFF381E72),
                    primaryContainer = Color(0xFF4F378B),
                    onPrimaryContainer = Color(0xFFEADDFF),
                    secondary = Color(0xFFCCC2DC),
                    secondaryContainer = Color(0xFF4A4458),
                )
            } else {
                base.copy(
                    primary = Color(0xFF6750A4),
                    onPrimary = Color.White,
                    primaryContainer = Color(0xFFEADDFF),
                    onPrimaryContainer = Color(0xFF21005D),
                    secondary = Color(0xFF625B71),
                    secondaryContainer = Color(0xFFE8DEF8),
                )
            }

        "red" ->
            if (isDark) {
                base.copy(
                    primary = Color(0xFFFF8A80),
                    onPrimary = Color(0xFF680008),
                    primaryContainer = Color(0xFF930010),
                    onPrimaryContainer = Color(0xFFFFDAD6),
                    secondary = Color(0xFFE7BDB8),
                    secondaryContainer = Color(0xFF5D3F3C),
                )
            } else {
                base.copy(
                    primary = Color(0xFFC62828),
                    onPrimary = Color.White,
                    primaryContainer = Color(0xFFFFDAD6),
                    onPrimaryContainer = Color(0xFF410002),
                    secondary = Color(0xFF775652),
                    secondaryContainer = Color(0xFFFFDAD6),
                )
            }

        "orange" ->
            if (isDark) {
                base.copy(
                    primary = Color(0xFFFFB74D),
                    onPrimary = Color(0xFF462A00),
                    primaryContainer = Color(0xFF653E00),
                    onPrimaryContainer = Color(0xFFFFDDB2),
                    secondary = Color(0xFFDDC2A1),
                    secondaryContainer = Color(0xFF56442A),
                )
            } else {
                base.copy(
                    primary = Color(0xFFE65100),
                    onPrimary = Color.White,
                    primaryContainer = Color(0xFFFFDDB2),
                    onPrimaryContainer = Color(0xFF291800),
                    secondary = Color(0xFF6F5B40),
                    secondaryContainer = Color(0xFFFBDEBC),
                )
            }

        else -> base
    }
}

@Composable
fun AppTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val flowPrefs =
        remember(context) {
            FlowSharedPreferences(SharedPreferencesHelper.getSharedPreferences(context))
        }

    val themeMode =
        flowPrefs.getString("theme_mode", "dark")
            .asFlow()
            .collectAsState(initial = "dark")
            .value

    val themeAccent =
        flowPrefs.getString("theme_accent", "dynamic")
            .asFlow()
            .collectAsState(initial = "dynamic")
            .value

    val systemDark = isSystemInDarkTheme()
    val useDarkTheme =
        when (themeMode) {
            "light" -> false
            "system" -> systemDark
            "amoled", "dark" -> true
            else -> darkTheme
        }

    val dynamicColorAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val baseColors =
        when {
            themeAccent == "dynamic" && dynamicColorAvailable && useDarkTheme ->
                dynamicDarkColorScheme(context)
            themeAccent == "dynamic" && dynamicColorAvailable && !useDarkTheme ->
                dynamicLightColorScheme(context)
            else -> customAccentScheme(themeAccent, useDarkTheme)
        }

    val finalColors =
        if (themeMode == "amoled" && useDarkTheme) {
            baseColors.copy(
                background = Color(0xFF000000),
                surface = Color(0xFF080808),
                surfaceVariant = Color(0xFF161816),
            )
        } else {
            baseColors
        }

    MaterialTheme(colorScheme = finalColors) {
        content()
    }
}
