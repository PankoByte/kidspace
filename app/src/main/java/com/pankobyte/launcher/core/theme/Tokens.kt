package com.pankobyte.launcher.core.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Colours for every parent-side screen: setup, the parent home, settings. One light look, as designed. */
object ParentColors {
    val bg = Color(0xFFF5F7FC)
    /** Cards, input fields and keypad keys, one step above [bg]. */
    val surface = Color(0xFFFFFFFF)
    /** Text, filled buttons and selected controls. */
    val ink = Color(0xFF192C4D)
    val onInk = Color(0xFFFFFFFF)
    val muted = Color(0xFF58677F)
    val border = Color(0xFFDCE3EF)
    /** Icon badges, disabled buttons, the unlit part of the step bar. */
    val sage = Color(0xFFE0E8F7)
    /** Quiet note boxes and disabled keypad keys. */
    val sageSoft = Color(0xFFEBF0FA)
    val disabledText = Color(0xFF64738B)
    val switchOff = Color(0xFF9AA8BE)

    /** Step bar and the welcome numerals. */
    val accent = Color(0xFF365DD9)
    val blueSoft = Color(0xFFE8EEFF)
    val preview = Color(0xFFDCEFEA)
    val error = Color(0xFF9C2F1E)
    val errorBg = Color(0xFFF9DFDA)
    val errorRing = Color(0xFFC4452D)
    val refusedBg = Color(0xFFF5DCCD)
    val refusedBorder = Color(0xFFBA553E)
    val refusedText = Color(0xFF6B2414)
    val emptyBg = Color(0xFFFBF4EE)

    val yellow = Color(0xFFE3B23C)
    val warnBg = Color(0xFFFCEFCF)
    val warnBgSoft = Color(0xFFFDF6E3)
    val warnBorder = Color(0xFFCB8F0F)
    val warnInk = Color(0xFF895A00)

    val green = Color(0xFF246A56)
    val greenBg = Color(0xFFE0F1E9)

    /** Soft hills behind the Start button. */
    val hillBack = Color(0xFFDBE6D5)
    val hillFront = Color(0xFFCFDDC6)
}

/** Daytime kid home, and the warmer look for the last minutes of play time. */
object KidColors {
    val sky = Color(0xFFCFE4E7)
    val sun = Color(0xFFE3B23C)
    val hillBack = Color(0xFF5F9B77)
    val hillFront = Color(0xFF407D5C)
    val ink = Color(0xFF1D3B2F)
    val onInk = Color(0xFFFBFCF7)
    val card = Color(0xFFFBFCF7)
    val barTrack = Color(0xFFDCE4D5)
    val roof = Color(0xFFB94A30)

    val lowSky = Color(0xFFF4B184)
    val lowSun = Color(0xFFDA541F)
    val lowHillBack = Color(0xFF3F6C4F)
    val lowHillFront = Color(0xFF2C4A3E)
    val lowEdge = Color(0xFF8C3219)
}

/** Night look: the Time's up screen and the "Grown-ups only" PIN screen. */
object NightColors {
    val bg = Color(0xFF1D2B45)
    val raised = Color(0xFF2B3B5D)
    val hillBack = Color(0xFF13213B)
    val hillFront = Color(0xFF0F1A30)
    val moon = Color(0xFFF5E3A1)
    val text = Color(0xFFFBF6E3)
    val muted = Color(0xFFC3CDE0)
    val errorBg = Color(0xFF4B2A37)
    val error = Color(0xFFF4B5A6)
}

object Dimens {
    val parentMargin = 24.dp
    val parentButtonHeight = 56.dp
    val parentButtonRadius = 16.dp
    val parentCardRadius = 22.dp

    val kidMargin = 20.dp
    val kidTileGap = 14.dp
    val kidTileRadius = 22.dp
    val kidPillHeight = 48.dp

    val pinKeyHeight = 58.dp
    val pinKeyRadius = 18.dp
    val pinDot = 16.dp
}
