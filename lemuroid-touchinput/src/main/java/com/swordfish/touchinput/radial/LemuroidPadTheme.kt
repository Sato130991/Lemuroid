package com.swordfish.touchinput.radial

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.DefaultShadowColor
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.swordfish.touchinput.radial.settings.TouchControllerID

enum class PadSkin(val id: String, val displayName: String) {
    AUTO("auto", "Automático (Consola)"),
    CLASSIC("classic", "Cristal Lemuroid"),
    NES("nes", "NES Original"),
    SNES("snes", "Super Nintendo"),
    GAMEBOY("gb", "Game Boy DMG"),
    GBA("gba", "Game Boy Advance"),
    MEGADRIVE("genesis", "Sega Mega Drive"),
    PS1("psx", "PlayStation 1"),
    N64("n64", "Nintendo 64"),
    ARCADE("arcade", "Arcade Neon"),
    DARK_OLED("oled", "Negro OLED"),
    ;

    companion object {
        fun fromId(id: String?): PadSkin = values().firstOrNull { it.id == id } ?: AUTO

        fun resolveForController(
            skin: PadSkin,
            controllerID: TouchControllerID?,
        ): PadSkin {
            if (skin != AUTO) return skin
            return when (controllerID) {
                TouchControllerID.NES,
                TouchControllerID.ATARI7800,
                TouchControllerID.PCE,
                -> NES

                TouchControllerID.SNES -> SNES

                TouchControllerID.GB,
                TouchControllerID.WS_LANDSCAPE,
                TouchControllerID.WS_PORTRAIT,
                -> GAMEBOY

                TouchControllerID.GBA,
                TouchControllerID.DESMUME,
                TouchControllerID.MELONDS,
                TouchControllerID.NINTENDO_3DS,
                -> GBA

                TouchControllerID.GENESIS_3,
                TouchControllerID.GENESIS_6,
                TouchControllerID.SMS,
                TouchControllerID.GG,
                -> MEGADRIVE

                TouchControllerID.PSX,
                TouchControllerID.PSX_DUALSHOCK,
                TouchControllerID.PSP,
                -> PS1

                TouchControllerID.N64 -> N64

                TouchControllerID.ARCADE_4,
                TouchControllerID.ARCADE_6,
                TouchControllerID.NGP,
                TouchControllerID.ATARI2600,
                TouchControllerID.LYNX,
                TouchControllerID.DOS,
                -> ARCADE

                null -> CLASSIC
            }
        }
    }
}

class LemuroidPadTheme(
    skin: PadSkin = PadSkin.CLASSIC,
    controllerID: TouchControllerID? = null,
    opacity: Float = 1.0f,
) {
    private val alphaMultiplier = opacity.coerceIn(0.15f, 1.0f)
    val resolvedSkin: PadSkin = PadSkin.resolveForController(skin, controllerID)

    private fun colorWithOpacity(
        red: Int,
        green: Int,
        blue: Int,
        baseAlpha: Float,
    ): Color {
        return Color(
            red = red / 255f,
            green = green / 255f,
            blue = blue / 255f,
            alpha = (baseAlpha * alphaMultiplier).coerceIn(0f, 1f),
        )
    }

    val foregroundPadding: Dp = 8.dp
    val padding: Dp = 4.dp

    private val icons: Color
    private val iconsPressed: Color
    private val level3Fill: Color
    private val level3FillPressed: Color
    private val level2Fill: Color
    private val level2FillPressed: Color
    val level1Fill: Color
    val level0Fill: Color

    init {
        when (resolvedSkin) {
            PadSkin.NES -> {
                // Classic NES controller: Crimson red buttons, dark charcoal well, light grey shell
                icons = colorWithOpacity(255, 245, 245, 0.90f)
                iconsPressed = colorWithOpacity(255, 255, 255, 1.0f)
                level3Fill = colorWithOpacity(211, 47, 47, 0.72f)
                level3FillPressed = colorWithOpacity(183, 28, 28, 0.92f)
                level2Fill = colorWithOpacity(66, 66, 66, 0.35f)
                level2FillPressed = colorWithOpacity(211, 47, 47, 0.50f)
                level1Fill = colorWithOpacity(33, 33, 33, 0.38f)
                level0Fill = colorWithOpacity(189, 189, 189, 0.14f)
            }

            PadSkin.SNES -> {
                // Super Nintendo: Lavender/Purple accents with classic light grey shell
                icons = colorWithOpacity(250, 245, 255, 0.90f)
                iconsPressed = colorWithOpacity(255, 255, 255, 1.0f)
                level3Fill = colorWithOpacity(103, 58, 183, 0.68f)
                level3FillPressed = colorWithOpacity(74, 20, 140, 0.90f)
                level2Fill = colorWithOpacity(149, 117, 205, 0.25f)
                level2FillPressed = colorWithOpacity(103, 58, 183, 0.45f)
                level1Fill = colorWithOpacity(120, 120, 135, 0.25f)
                level0Fill = colorWithOpacity(180, 180, 195, 0.12f)
            }

            PadSkin.GAMEBOY -> {
                // Original Game Boy DMG-01: Burgundy/Magenta A/B buttons, olive-grey housing
                icons = colorWithOpacity(255, 240, 245, 0.90f)
                iconsPressed = colorWithOpacity(255, 255, 255, 1.0f)
                level3Fill = colorWithOpacity(156, 20, 88, 0.72f)
                level3FillPressed = colorWithOpacity(106, 13, 60, 0.92f)
                level2Fill = colorWithOpacity(48, 56, 65, 0.30f)
                level2FillPressed = colorWithOpacity(156, 20, 88, 0.45f)
                level1Fill = colorWithOpacity(115, 118, 98, 0.30f)
                level0Fill = colorWithOpacity(155, 165, 130, 0.15f)
            }

            PadSkin.GBA -> {
                // Game Boy Advance Indigo shell with light grey tactile buttons
                icons = colorWithOpacity(240, 242, 255, 0.90f)
                iconsPressed = colorWithOpacity(255, 255, 255, 1.0f)
                level3Fill = colorWithOpacity(73, 89, 184, 0.68f)
                level3FillPressed = colorWithOpacity(40, 53, 147, 0.90f)
                level2Fill = colorWithOpacity(121, 134, 203, 0.25f)
                level2FillPressed = colorWithOpacity(73, 89, 184, 0.45f)
                level1Fill = colorWithOpacity(57, 73, 171, 0.25f)
                level0Fill = colorWithOpacity(63, 81, 181, 0.12f)
            }

            PadSkin.MEGADRIVE -> {
                // Sega Mega Drive / Genesis: Deep matte black buttons with crimson/blue highlights
                icons = colorWithOpacity(245, 245, 245, 0.92f)
                iconsPressed = colorWithOpacity(255, 82, 82, 1.0f)
                level3Fill = colorWithOpacity(38, 50, 56, 0.78f)
                level3FillPressed = colorWithOpacity(198, 40, 40, 0.88f)
                level2Fill = colorWithOpacity(55, 71, 79, 0.35f)
                level2FillPressed = colorWithOpacity(198, 40, 40, 0.45f)
                level1Fill = colorWithOpacity(20, 25, 30, 0.42f)
                level0Fill = colorWithOpacity(30, 35, 40, 0.18f)
            }

            PadSkin.PS1 -> {
                // PlayStation 1 Original Grey pad
                icons = colorWithOpacity(236, 239, 241, 0.92f)
                iconsPressed = colorWithOpacity(128, 216, 255, 1.0f)
                level3Fill = colorWithOpacity(84, 110, 122, 0.70f)
                level3FillPressed = colorWithOpacity(38, 50, 56, 0.90f)
                level2Fill = colorWithOpacity(120, 144, 156, 0.28f)
                level2FillPressed = colorWithOpacity(84, 110, 122, 0.50f)
                level1Fill = colorWithOpacity(96, 125, 139, 0.28f)
                level0Fill = colorWithOpacity(144, 164, 174, 0.14f)
            }

            PadSkin.N64 -> {
                // Nintendo 64: Royal blue/green/red retro 64 palette
                icons = colorWithOpacity(255, 253, 231, 0.92f)
                iconsPressed = colorWithOpacity(255, 235, 59, 1.0f)
                level3Fill = colorWithOpacity(30, 136, 229, 0.68f)
                level3FillPressed = colorWithOpacity(46, 125, 50, 0.90f)
                level2Fill = colorWithOpacity(253, 216, 53, 0.25f)
                level2FillPressed = colorWithOpacity(229, 57, 53, 0.50f)
                level1Fill = colorWithOpacity(97, 97, 97, 0.30f)
                level0Fill = colorWithOpacity(158, 158, 158, 0.12f)
            }

            PadSkin.ARCADE -> {
                // Arcade cabinet neon cyan & magenta
                icons = colorWithOpacity(255, 255, 255, 0.92f)
                iconsPressed = colorWithOpacity(255, 235, 59, 1.0f)
                level3Fill = colorWithOpacity(0, 172, 193, 0.68f)
                level3FillPressed = colorWithOpacity(216, 27, 96, 0.90f)
                level2Fill = colorWithOpacity(0, 131, 143, 0.28f)
                level2FillPressed = colorWithOpacity(216, 27, 96, 0.45f)
                level1Fill = colorWithOpacity(26, 35, 126, 0.30f)
                level0Fill = colorWithOpacity(13, 19, 51, 0.16f)
            }

            PadSkin.DARK_OLED -> {
                // Minimalist Dark OLED
                icons = colorWithOpacity(200, 200, 200, 0.75f)
                iconsPressed = colorWithOpacity(62, 227, 102, 0.95f)
                level3Fill = colorWithOpacity(28, 28, 30, 0.65f)
                level3FillPressed = colorWithOpacity(48, 48, 52, 0.85f)
                level2Fill = colorWithOpacity(38, 38, 42, 0.25f)
                level2FillPressed = colorWithOpacity(62, 227, 102, 0.30f)
                level1Fill = colorWithOpacity(15, 15, 15, 0.35f)
                level0Fill = colorWithOpacity(0, 0, 0, 0.25f)
            }

            PadSkin.CLASSIC, PadSkin.AUTO -> {
                icons = colorWithOpacity(0, 0, 0, 0.50f)
                iconsPressed = colorWithOpacity(255, 255, 255, 0.50f)
                level3Fill = colorWithOpacity(255, 255, 255, 0.50f)
                level3FillPressed = colorWithOpacity(0, 0, 0, 0.50f)
                level2Fill = colorWithOpacity(255, 255, 255, 0.125f)
                level2FillPressed = colorWithOpacity(0, 0, 0, 0.125f)
                level1Fill = colorWithOpacity(255, 255, 255, 0.10f)
                level0Fill = colorWithOpacity(255, 255, 255, 0.05f)
            }
        }
    }

    val level3Shadow = DefaultShadowColor.copy(alpha = 0.05f * alphaMultiplier)
    val level3ShadowWidth = 4.dp

    val level2Shadow = DefaultShadowColor.copy(alpha = 0.05f * alphaMultiplier)
    val level2ShadowWidth = 4.dp

    val level1Shadow = DefaultShadowColor.copy(alpha = 0.10f * alphaMultiplier)
    val level1ShadowWidth = 4.dp

    val level0CornerRadius = 0.dp
    val level0Shadow = DefaultShadowColor.copy(alpha = 0.10f * alphaMultiplier)
    val level0ShadowWidth = 2.dp

    fun compositeFill(pressed: Boolean): Color {
        return if (pressed) {
            level2FillPressed
        } else {
            level2Fill
        }
    }

    fun foregroundFill(pressed: Boolean): Color {
        return if (pressed) {
            level3FillPressed
        } else {
            level3Fill
        }
    }

    fun icons(pressed: Boolean): Color {
        return if (pressed) {
            iconsPressed
        } else {
            icons
        }
    }
}

val LocalLemuroidPadTheme =
    compositionLocalOf<LemuroidPadTheme> {
        error("LemuroidPadTheme is missing")
    }
