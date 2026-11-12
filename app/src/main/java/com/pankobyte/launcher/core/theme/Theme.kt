package com.pankobyte.launcher.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.pankobyte.launcher.R

@OptIn(ExperimentalTextApi::class)
private fun fraunces(weight: Int) = Font(
    R.font.fraunces,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight),
        FontVariation.Setting("opsz", 72f),
        FontVariation.Setting("SOFT", 50f),
        FontVariation.Setting("WONK", 0f),
    ),
)

@OptIn(ExperimentalTextApi::class)
private fun karla(weight: Int) = Font(
    R.font.karla,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/** A playful serif stays on the child home; parent screens use the clearer sans family. */
val KidDisplayFont = FontFamily(fraunces(600), fraunces(700))
val DisplayFont = FontFamily(karla(600), karla(700))

/** Sans for everything else. It is the default on every screen. */
val BodyFont = FontFamily(karla(400), karla(500), karla(600), karla(700))

/** Every screen takes its colours from Tokens.kt. The look does not follow the phone's dark setting. */
@Composable
fun LauncherTheme(content: @Composable () -> Unit) {
    // MaterialTheme supplies ripples only. Colours never come from the Material scheme.
    MaterialTheme(colorScheme = lightColorScheme()) {
        ProvideTextStyle(TextStyle(fontFamily = BodyFont, letterSpacing = 0.sp), content)
    }
}
