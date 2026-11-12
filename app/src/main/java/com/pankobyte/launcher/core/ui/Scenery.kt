package com.pankobyte.launcher.core.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.pankobyte.launcher.core.theme.KidColors
import com.pankobyte.launcher.core.theme.NightColors

/** Two overlapping hills filling the bottom of their box: a taller one on the left, a nearer one on the right. */
@Composable
fun Hills(modifier: Modifier = Modifier, back: Color, front: Color) {
    Canvas(modifier.clipToBounds()) {
        val w = size.width
        val h = size.height
        drawOval(back, topLeft = Offset(-0.45f * w, 0f), size = Size(1.2f * w, 2.2f * h))
        drawOval(front, topLeft = Offset(0.25f * w, 0.24f * h), size = Size(1.3f * w, 2.2f * h))
    }
}

enum class SunnyMood {
    /** Open eyes that blink now and then. */
    Happy,
    /** Heavy eyelids, for the last minutes of play time. */
    Sleepy,
}

/**
 * Sunny, the app's character: the sun with a face. It is the logo on the parent screens and
 * the sun in the child's sky, and it gets sleepy as play time runs out. Drawn in code, so it
 * adds no image files.
 */
@Composable
fun Sunny(
    modifier: Modifier = Modifier,
    mood: SunnyMood = SunnyMood.Happy,
    color: Color = KidColors.sun,
    face: Color = KidColors.ink,
) {
    // One quick blink at the end of every loop. The system's "remove animations" setting stops it.
    val open by rememberInfiniteTransition(label = "sunny").animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 4200
                1f at 0
                1f at 3900
                0.1f at 4030
                1f at 4160
            },
        ),
        label = "blink",
    )
    Canvas(modifier) {
        val r = size.minDimension / 2
        drawCircle(color, r, center)
        drawFace(center, r, face, if (mood == SunnyMood.Sleepy) null else open)
    }
}

/**
 * Eyes, cheeks and a smile on a disc of radius [r] around [c]. [open] is how far the eyes are
 * open, from 0 to 1. Null draws them as closed lids.
 */
private fun DrawScope.drawFace(c: Offset, r: Float, ink: Color, open: Float?) {
    val line = Stroke(r * 0.075f, cap = StrokeCap.Round)
    for (side in listOf(-1f, 1f)) {
        val eye = c + Offset(side * r * 0.3f, -r * 0.1f)
        if (open == null) {
            val lid = r * 0.13f
            drawArc(ink, 20f, 140f, false, eye - Offset(lid, lid * 1.2f), Size(lid * 2, lid * 2), style = line)
        } else {
            val eyeR = r * 0.085f
            drawOval(ink, eye - Offset(eyeR, eyeR * open), Size(eyeR * 2, eyeR * 2 * open))
        }
        drawCircle(KidColors.lowSun.copy(alpha = 0.28f), r * 0.13f, c + Offset(side * r * 0.52f, r * 0.2f))
    }
    val smile = r * 0.34f
    drawArc(ink, 25f, 130f, false, c + Offset(-smile, r * 0.12f - smile), Size(smile * 2, smile * 2), style = line)
}

/** The picture at the top of the welcome screen: Sunny over the hills and a small house. */
@Composable
fun WelcomeScene(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(28.dp)).background(KidColors.sky)) {
        Sunny(Modifier.align(Alignment.TopEnd).padding(top = 24.dp, end = 44.dp).size(72.dp))
        Hills(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(0.42f),
            back = KidColors.hillBack,
            front = KidColors.ink,
        )
        Canvas(Modifier.align(Alignment.BottomStart).offset(x = 58.dp, y = (-46).dp).size(30.dp, 40.dp)) {
            val roof = size.height * 0.36f
            drawPath(
                Path().apply {
                    moveTo(size.width / 2, 0f)
                    lineTo(size.width + 3.dp.toPx(), roof)
                    lineTo(-3.dp.toPx(), roof)
                    close()
                },
                KidColors.roof,
            )
            drawRect(Color.White, topLeft = Offset(0f, roof), size = Size(size.width, size.height - roof))
        }
    }
}

/** Night sky for the Time's up screen: twinkling stars, a sleeping moon, dark hills. */
@Composable
fun NightSky(modifier: Modifier = Modifier) {
    val loop = rememberInfiniteTransition(label = "night")
    val twinkle by loop.animateFloat(0.35f, 1f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "twinkle")
    // Three "z" marks drift up from the moon one after another.
    val drift by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(3600, easing = LinearEasing)), label = "zzz")
    Box(modifier.fillMaxSize().background(NightColors.bg)) {
        Canvas(Modifier.fillMaxSize()) {
            val stars = listOf(0.13f to 0.10f, 0.64f to 0.125f, 0.83f to 0.18f, 0.21f to 0.26f, 0.88f to 0.39f, 0.09f to 0.43f)
            stars.forEachIndexed { index, (x, y) ->
                val even = index % 2 == 0
                drawCircle(
                    NightColors.moon.copy(alpha = if (even) twinkle else 1.35f - twinkle),
                    radius = (if (even) 2.5f else 1.8f).dp.toPx(),
                    center = Offset(size.width * x, size.height * y),
                )
            }
            val r = 70.dp.toPx()
            val centre = Offset(size.width * 0.5f, size.height * 0.27f)
            drawCircle(NightColors.moon, r, centre)
            drawFace(centre, r, NightColors.bg, open = null)
            val z = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round)
            repeat(3) { index ->
                val t = (drift + index / 3f) % 1f
                val s = (6 + 5 * t).dp.toPx()
                val at = centre + Offset(r * 0.95f + r * 0.45f * t, -r * 0.6f - r * 0.75f * t)
                // Fades in, then out, over its climb.
                val alpha = (if (t < 0.25f) t / 0.25f else (1f - t) / 0.75f).coerceIn(0f, 1f)
                drawPath(
                    Path().apply {
                        moveTo(at.x, at.y)
                        lineTo(at.x + s, at.y)
                        lineTo(at.x, at.y + s)
                        lineTo(at.x + s, at.y + s)
                    },
                    NightColors.moon.copy(alpha = alpha),
                    style = z,
                )
            }
        }
        Hills(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(150.dp),
            back = NightColors.hillBack,
            front = NightColors.hillFront,
        )
    }
}
