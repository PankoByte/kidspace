package com.pankobyte.launcher.kid.setup

import android.animation.ValueAnimator
import android.graphics.Paint
import android.os.Build
import android.graphics.Typeface
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pankobyte.launcher.R
import com.pankobyte.launcher.core.theme.KidColors
import com.pankobyte.launcher.core.theme.LineIcons
import com.pankobyte.launcher.core.theme.ParentColors
import kotlin.math.PI
import kotlin.math.sin

/** A single paced loop. Android's disabled-animation setting gets a complete, still scene. */
@Composable
private fun illustrationProgress(loopMillis: Int = 9000): State<Float> {
    if (!ValueAnimator.areAnimatorsEnabled()) return rememberUpdatedState(0.7f)
    return rememberInfiniteTransition(label = "setupIllustration").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(loopMillis, easing = LinearEasing)),
        label = "illustrationProgress",
    )
}

private fun smooth(value: Float): Float = value.coerceIn(0f, 1f).let { it * it * (3 - 2 * it) }

/**
 * Two spaces on one phone. The parent's apps are gathered into a container, a lock closes on it,
 * and it slides off the left edge. A separate container named after the app slides in from the
 * right with the child's apps. The two are never on screen as one, which is the whole promise.
 */
@Composable
fun WelcomeIllustration(modifier: Modifier = Modifier) {
    val progress = illustrationProgress(loopMillis = 6000)
    val name = stringResource(R.string.app_name)
    val description = stringResource(R.string.welcome_animation_description)
    val parentApps = listOf(LineIcons.Camera, LineIcons.Phone, LineIcons.Clock, LineIcons.Music, LineIcons.Sliders,
        LineIcons.Grid, LineIcons.Home, LineIcons.Key, LineIcons.Search).map { rememberVectorPainter(it) }
    val kidApps = listOf(LineIcons.Gamepad, LineIcons.Brush, LineIcons.Book, LineIcons.Music).map { rememberVectorPainter(it) }
    val kidTints = listOf(Color(0xFFDCD5F5), Color(0xFFFFDFCA), Color(0xFFD5E2FF), Color(0xFFFCE7B2))
    val lock = rememberVectorPainter(LineIcons.Lock)
    val glass = remember {
        Path().apply { addRoundRect(RoundRect(Rect(SCREEN_X, SCREEN_Y, SCREEN_X + SCREEN_W, SCREEN_Y + SCREEN_H), CornerRadius(27f))) }
    }
    Canvas(modifier.clearAndSetSemantics { contentDescription = description }) {
        val t = progress.value
        // The parent's home screen shrinks into a card of its own, a lock closes on it, and the
        // card leaves to the left. At the end of the loop it is simply back, full size.
        val leaving = t < 0.9f
        val boxed = if (leaving) seg(t, 0.07f, 0.18f) else 0f
        val locked = if (leaving) seg(t, 0.19f, 0.27f) else 0f
        val parentOut = if (leaving) seg(t, 0.30f, 0.44f) else 0f
        // The child's space arrives as a second, separate card, then opens to fill the screen.
        // The two cards are never joined, which is the whole promise.
        val kidIn = seg(t, 0.38f, 0.52f)
        val opened = seg(t, 0.54f, 0.64f)
        val kidAlpha = 1 - seg(t, 0.93f, 1f)
        // The phone floats: one slow rise and fall per loop, with the ground shadow answering it.
        val lift = sin(t * 2 * PI).toFloat()
        // Drawn in its own units and fitted to whatever room the screen has left over.
        val unit = minOf(size.width / STAGE_W, size.height / STAGE_H)
        translate(size.width / 2 - 180f * unit, (size.height - STAGE_H * unit) / 2) {
        scale(unit, pivot = Offset.Zero) {
            drawCircle(ParentColors.blueSoft, 138f, Offset(180f, 207f))
            translate(180f, 421f) {
                scale(1f - 0.06f * lift, 0.09f, pivot = Offset.Zero) {
                    drawCircle(
                        Brush.radialGradient(
                            0f to ParentColors.ink.copy(alpha = 0.34f - 0.06f * lift),
                            0.55f to ParentColors.ink.copy(alpha = 0.12f),
                            1f to Color.Transparent,
                            center = Offset.Zero, radius = 106f,
                        ),
                        106f, Offset.Zero,
                    )
                }
            }
            translate(top = -2.5f * lift) {
                phone(SCREEN_X, SCREEN_Y, SCREEN_W, SCREEN_H, 27f, Backdrop)
                clipPath(glass) {
                    if (parentOut < 1f) card(glass, -CARD_TRAVEL * parentOut, 1f - CARD_SHRINK * boxed) {
                        drawRect(ParentWallpaper, Offset(SCREEN_X, SCREEN_Y), Size(SCREEN_W, SCREEN_H))
                        // A widget, a grid of apps and a dock, as on any phone.
                        tile(112f, 46f, 136f, 54f, 16f)
                        drawCircle(ParentColors.blueSoft, 15f, Offset(134f, 73f))
                        box(ParentColors.sage, 158f, 62f, 74f, 8f, 4f)
                        box(ParentColors.sageSoft, 158f, 77f, 50f, 8f, 4f)
                        for (index in parentApps.indices) {
                            val x = 112f + (index % 3) * 50f
                            val y = 120f + (index / 3) * 54f
                            tile(x, y, 36f, 36f, 11f)
                            glyph(parentApps[index], x + 9f, y + 9f, 18f, ParentColors.muted, unit)
                        }
                        box(Color.White, 106f, 334f, 148f, 50f, 19f, 0.7f)
                        for (index in 0..2) {
                            val x = 116f + index * 46f
                            box(ParentColors.sage, x, 341f, 36f, 36f, 11f)
                            glyph(parentApps[index + 1], x + 9f, 350f, 18f, ParentColors.ink, unit)
                        }
                        drawRect(ParentColors.ink, Offset(SCREEN_X, SCREEN_Y), Size(SCREEN_W, SCREEN_H), alpha = 0.32f * locked)
                        if (locked > 0f) {
                            // In the gap between the grid and the dock, so it covers no app.
                            val pop = locked * (1 + 0.2f * sin(locked * PI).toFloat())
                            drawCircle(Color.White, 20f * pop, Offset(180f, 300f), alpha = locked)
                            glyph(lock, 170f, 290f, 20f, ParentColors.ink, unit, locked)
                        }
                    }
                    if (kidIn > 0f && kidAlpha > 0f) card(glass, CARD_TRAVEL * (1 - kidIn), 1f - CARD_SHRINK * (1 - opened)) {
                        drawRect(KidWallpaper, Offset(SCREEN_X, SCREEN_Y), Size(SCREEN_W, SCREEN_H), alpha = kidAlpha)
                        drawCircle(KidColors.sun, 13f, Offset(238f, 58f), alpha = kidAlpha)
                        drawOval(KidColors.hillBack, Offset(60f, 352f), Size(200f, 120f), alpha = kidAlpha)
                        drawOval(KidColors.hillFront, Offset(150f, 366f), Size(220f, 120f), alpha = kidAlpha)
                        drawLogo(163f, 48f, 34f, kidAlpha)
                        label(name, 180f, 103f, 16f, KidColors.ink, bold = true, alpha = kidAlpha, center = true, maxWidth = 140f)
                        for (index in kidApps.indices) {
                            val shown = seg(t, 0.58f + 0.02f * index, 0.64f + 0.02f * index) * kidAlpha
                            val x = 110f + (index % 2) * 76f
                            val y = 124f + (index / 2) * 76f
                            tile(x, y, 64f, 64f, 20f, shown)
                            drawCircle(kidTints[index], 22f * shown, Offset(x + 32f, y + 32f), alpha = shown)
                            glyph(kidApps[index], x + 20f, y + 20f, 24f, ParentColors.ink, unit, shown)
                        }
                    }
                }
                // Drawn over the sliding screens, as the real ones sit over whatever is showing.
                statusBar(SCREEN_X, SCREEN_Y, SCREEN_W)
                island(180f, 18f)
                box(ParentColors.ink, 154f, 392f, 52f, 4f, 2f, 0.85f)
            }
        }
        }
    }
}

// The welcome phone, in drawing units. Its screen is 9 by 21, as tall phones are.
private const val SCREEN_X = 96f
private const val SCREEN_Y = 11f
private const val SCREEN_W = 168f
private const val SCREEN_H = 392f
private const val STAGE_W = 290f
private const val STAGE_H = 434f
private val ParentWallpaper = Brush.verticalGradient(listOf(Color(0xFFEEF2FB), Color(0xFFDDE6F8)), startY = SCREEN_Y, endY = SCREEN_Y + SCREEN_H)
private val KidWallpaper = Brush.verticalGradient(listOf(KidColors.sky, Color(0xFFEAF4F1)), startY = SCREEN_Y, endY = SCREEN_Y + SCREEN_H)

private val Backdrop = Brush.verticalGradient(listOf(Color(0xFFD9E1F2), Color(0xFFC6D2EA)), startY = SCREEN_Y, endY = SCREEN_Y + SCREEN_H)

/** How much smaller a home screen is once it has become a card, and how far a card slides to leave. */
private const val CARD_SHRINK = 0.27f
private const val CARD_TRAVEL = 165f

/**
 * A whole home screen as a card: [amount] of full size about the middle of the phone's screen and
 * moved sideways by [shift]. Below full size it gets rounded corners from [glass] and a shadow.
 */
private inline fun DrawScope.card(glass: Path, shift: Float, amount: Float, content: DrawScope.() -> Unit) {
    translate(left = shift) {
        scale(amount, pivot = Offset(180f, SCREEN_Y + SCREEN_H / 2)) {
            val lifted = ((1f - amount) / CARD_SHRINK).coerceIn(0f, 1f)
            for (step in 1..5) {
                val grow = step * 2.2f
                box(ParentColors.ink, SCREEN_X - grow, SCREEN_Y - grow + 7f, SCREEN_W + 2 * grow, SCREEN_H + 2 * grow, 27f + grow, 0.04f * lifted)
            }
            clipPath(glass) { content() }
        }
    }
}

/** A white tile sitting just above the wallpaper: no outline, only a faint shadow under it. */
private fun DrawScope.tile(x: Float, y: Float, w: Float, h: Float, r: Float, alpha: Float = 1f) {
    box(ParentColors.ink, x - 0.5f, y + 2.5f, w + 1f, h, r, 0.05f * alpha)
    box(ParentColors.ink, x, y + 1f, w, h, r, 0.05f * alpha)
    box(Color.White, x, y, w, h, r, alpha)
}

private fun DrawScope.glyph(painter: Painter, x: Float, y: Float, side: Float, tint: Color, unit: Float, alpha: Float = 1f) {
    // Rasterize the vector at the final pixel size, avoiding a blurry upscaled cache.
    translate(x, y) {
        scale(1f / unit, pivot = Offset.Zero) {
            with(painter) { draw(Size(side * unit, side * unit), alpha = alpha, colorFilter = ColorFilter.tint(tint)) }
        }
    }
}

private val FrameLight = Color(0xFF6F82AB)
private val FrameMid = Color(0xFF2A3D63)
private val FrameDark = Color(0xFF111F3A)
private val Bezel = Color(0xFF0A1426)

/**
 * A phone around a screen of [w] by [h] at [x], [y]: soft drop shadow, side buttons, a metal
 * frame that catches light at its top left corner, a dark bezel, then the screen itself.
 */
private fun DrawScope.phone(x: Float, y: Float, w: Float, h: Float, r: Float, screen: Brush = SolidColor(Color.White)) {
    val bezel = 4.6f
    val edge = bezel + 2.4f
    // The shadow is a stack of faint, growing copies of the body, which reads as one soft blur.
    for (step in 1..8) {
        val grow = step * 1.5f
        box(ParentColors.ink, x - edge - grow, y - edge - grow + 8f, w + 2 * (edge + grow), h + 2 * (edge + grow), r + edge + grow, 0.02f)
    }
    box(FrameDark, x + w + edge - 1.2f, y + h * 0.27f, 2.8f, h * 0.11f, 1.4f)
    box(FrameDark, x - edge - 1.6f, y + h * 0.21f, 2.8f, h * 0.06f, 1.4f)
    box(FrameDark, x - edge - 1.6f, y + h * 0.29f, 2.8f, h * 0.06f, 1.4f)
    val outer = Offset(x - edge, y - edge)
    val outerSize = Size(w + 2 * edge, h + 2 * edge)
    drawRoundRect(
        Brush.linearGradient(
            0f to FrameLight, 0.18f to FrameMid, 0.62f to FrameDark, 1f to FrameMid,
            start = outer, end = Offset(outer.x + outerSize.width, outer.y + outerSize.height),
        ),
        outer, outerSize, CornerRadius(r + edge),
    )
    box(Bezel, x - bezel, y - bezel, w + 2 * bezel, h + 2 * bezel, r + bezel)
    drawRoundRect(screen, Offset(x, y), Size(w, h), CornerRadius(r))
}

/** The camera cut-out at the top of the screen, centred on [cx]. */
private fun DrawScope.island(cx: Float, y: Float) {
    box(Bezel, cx - 19f, y, 38f, 11f, 5.5f)
    drawCircle(Color(0xFF24365A), 2.4f, Offset(cx + 11f, y + 5.5f))
    drawCircle(Color(0xFF5E78B0), 0.8f, Offset(cx + 10.3f, y + 4.8f))
}

/** Clock, signal and battery along the top of a screen that starts at [x], [y] and is [w] wide. */
private fun DrawScope.statusBar(x: Float, y: Float, w: Float) {
    label("9:41", x + 18f, y + 16f, 9f, ParentColors.ink, bold = true)
    val right = x + w - 18f
    for (bar in 0..3) {
        val height = 2.5f + bar * 1.5f
        box(ParentColors.ink, right - 36f + bar * 3.4f, y + 16f - height, 2.2f, height, 0.8f)
    }
    drawRoundRect(ParentColors.ink, Offset(right - 17f, y + 8.5f), Size(15f, 7.5f), CornerRadius(2.4f), alpha = 0.45f, style = Stroke(0.9f))
    box(ParentColors.ink, right - 15.6f, y + 9.9f, 9.5f, 4.7f, 1.3f)
    box(ParentColors.ink, right - 1.2f, y + 10.8f, 1.2f, 3f, 0.6f, 0.45f)
}

private fun seg(t: Float, from: Float, to: Float): Float = smooth((t - from) / (to - from))

/** 0 to 1 and back: a short press centred just after [at]. */
private fun press(t: Float, at: Float): Float = seg(t, at - 0.025f, at) * (1 - seg(t, at + 0.01f, at + 0.05f))

private fun mix(a: Offset, b: Offset, f: Float): Offset = a + (b - a) * f

private val demoText = Paint(Paint.ANTI_ALIAS_FLAG)
private val demoBold: Typeface = Typeface.create("sans-serif", Typeface.BOLD)
private val demoRegular: Typeface = Typeface.create("sans-serif", Typeface.NORMAL)

/** Text inside a demo, in the demo's own small units. Shrinks to fit [maxWidth]. */
private fun DrawScope.label(
    text: String, x: Float, y: Float, textSize: Float, color: Color,
    bold: Boolean = false, alpha: Float = 1f, center: Boolean = false, maxWidth: Float = Float.MAX_VALUE,
) {
    if (alpha <= 0f) return
    demoText.typeface = if (bold) demoBold else demoRegular
    demoText.textAlign = if (center) Paint.Align.CENTER else Paint.Align.LEFT
    demoText.textSize = textSize
    val width = demoText.measureText(text)
    if (width > maxWidth) demoText.textSize = textSize * maxWidth / width
    demoText.color = color.copy(alpha = color.alpha * alpha.coerceIn(0f, 1f)).toArgb()
    drawContext.canvas.nativeCanvas.drawText(text, x, y, demoText)
}

private fun DrawScope.box(color: Color, x: Float, y: Float, w: Float, h: Float, r: Float, alpha: Float = 1f) {
    if (alpha > 0f) drawRoundRect(color, Offset(x, y), Size(w, h), CornerRadius(r), alpha = alpha.coerceIn(0f, 1f))
}

/** The fingertip that does the tapping in every demo. It is the cue that this is a recording. */
private fun DrawScope.finger(at: Offset, press: Float, alpha: Float) {
    if (alpha <= 0f) return
    val a = alpha.coerceIn(0f, 1f)
    drawCircle(ParentColors.accent, 15f + 13f * press, at, alpha = 0.22f * press * a)
    val r = 13f - 2.5f * press
    drawCircle(Color.Black, r, at + Offset(0f, 3f - 2f * press), alpha = 0.14f * a)
    drawCircle(Color.White, r, at, alpha = 0.96f * a)
    drawCircle(ParentColors.ink, r, at, alpha = a, style = Stroke(2.5f))
}

/** The app's icon, small. */
private fun DrawScope.mark(x: Float, y: Float, side: Float, alpha: Float = 1f) = drawLogo(x, y, side, alpha)

/** A thin playback bar along the bottom, as on a video. */
private fun DrawScope.playbackBar(t: Float, y: Float) {
    box(Color(0xFFC3CCDD), 18f, y, 324f, 3f, 1.5f)
    box(ParentColors.accent, 18f, y, 324f * t, 3f, 1.5f)
}

private fun DrawScope.radio(at: Offset, selected: Boolean) {
    drawCircle(if (selected) ParentColors.accent else ParentColors.switchOff, 8f, at, style = Stroke(2f))
    if (selected) drawCircle(ParentColors.accent, 4.2f, at)
}

/** The words shown inside a [SettingsDemo]: the ones the parent has to find in Android. */
private class DemoWords(
    val listTitle: String,
    /** Small heading above our row, when Android groups the list. */
    val section: String?,
    val row: String,
    val pageTitle: String,
    val toggle: String,
    val allow: String,
)

/** Which written step the clip is showing: 0 to 2, 3 once it is done, -1 when it is a still. */
@Composable
private fun demoStep(progress: State<Float>, still: Boolean): State<Int> = remember(progress, still) {
    derivedStateOf {
        val t = progress.value
        if (still) -1 else if (t < 0.34f) 0 else if (t < 0.60f) 1 else if (t < 0.80f) 2 else 3
    }
}

/**
 * A short looping recording of two Android settings screens: a fingertip opens our row in a list,
 * switches it on, then taps Allow. It is drawn small, inside a phone on a tinted stage with a
 * playback bar, so it reads as a clip to watch and not as controls to press. With [still] it is
 * one finished frame.
 */
@Composable
private fun SettingsDemo(words: DemoWords, progress: State<Float>, still: Boolean) {
    Canvas(Modifier.fillMaxWidth().aspectRatio(360f / 250f).clip(RoundedCornerShape(24.dp)).background(ParentColors.blueSoft)) {
        val t = if (still) 0.9f else progress.value
        val page = seg(t, 0.28f, 0.36f) * (1 - seg(t, 0.96f, 1f))
        val switched = seg(t, 0.53f, 0.58f)
        val dialog = seg(t, 0.60f, 0.66f) * (1 - seg(t, 0.78f, 0.82f))
        val done = seg(t, 0.82f, 0.88f)
        scale(size.width / 360f, pivot = Offset.Zero) {
            // The phone runs off the bottom of the stage: a crop of a screen, not a card.
            phone(71f, 28f, 218f, 300f, 24f)
            island(180f, 34f)
            clipRect(71f, 28f, 289f, 250f) {
                // Screen one: a list in Android settings, with our row in it.
                val a = 1 - page
                translate(left = -30f * page) {
                    label(words.listTitle, 85f, 68f, 15f, ParentColors.ink, bold = true, alpha = a, maxWidth = 190f)
                    for (y in listOf(84f, 108f)) {
                        drawCircle(ParentColors.sage, 8f, Offset(93f, y + 8f), alpha = a)
                        box(ParentColors.sage, 110f, y + 3f, if (y == 84f) 96f else 70f, 9f, 4.5f, a)
                    }
                    if (words.section != null) label(words.section, 85f, 147f, 10f, ParentColors.muted, alpha = a, maxWidth = 190f)
                    box(ParentColors.blueSoft, 79f, 156f, 202f, 40f, 12f, a)
                    mark(87f, 164f, 24f, a)
                    label(words.row, 119f, 180f, 11.5f, ParentColors.ink, bold = true, alpha = a, maxWidth = 140f)
                    drawPath(Path().apply { moveTo(266f, 171f); lineTo(271f, 176f); lineTo(266f, 181f) },
                        ParentColors.accent, alpha = a, style = Stroke(2f, cap = StrokeCap.Round))
                    drawCircle(ParentColors.sage, 8f, Offset(93f, 218f), alpha = a)
                    box(ParentColors.sage, 110f, 213f, 84f, 9f, 4.5f, a)
                }
                // Screen two: our entry, its switch, then Android's confirmation.
                translate(left = 30f * (1 - page)) {
                    drawPath(Path().apply { moveTo(90f, 58f); lineTo(85f, 63f); lineTo(90f, 68f) },
                        ParentColors.ink, alpha = page, style = Stroke(2f, cap = StrokeCap.Round))
                    label(words.pageTitle, 99f, 67.5f, 12.5f, ParentColors.ink, bold = true, alpha = page, maxWidth = 176f)
                    box(ParentColors.bg, 79f, 82f, 202f, 44f, 12f, page)
                    label(words.toggle, 89f, 108f, 10.5f, ParentColors.ink, bold = true, alpha = page, maxWidth = 146f)
                    box(ParentColors.switchOff, 243f, 95f, 30f, 18f, 9f, page * (1 - switched))
                    box(ParentColors.accent, 243f, 95f, 30f, 18f, 9f, page * switched)
                    drawCircle(Color.White, 6.5f, Offset(252f + 12f * switched, 104f), alpha = page)
                    box(ParentColors.sage, 85f, 142f, 150f, 8f, 4f, page)
                    box(ParentColors.sage, 85f, 158f, 110f, 8f, 4f, page)
                }
                if (dialog > 0f) {
                    drawRect(ParentColors.ink, Offset(71f, 28f), Size(218f, 222f), alpha = 0.28f * dialog)
                    translate(top = 16f * (1 - dialog)) {
                        box(Color.White, 83f, 146f, 194f, 88f, 16f, dialog)
                        box(ParentColors.sage, 97f, 162f, 120f, 8f, 4f, dialog)
                        box(ParentColors.sage, 97f, 177f, 150f, 8f, 4f, dialog)
                        box(ParentColors.sage, 137f, 196f, 60f, 26f, 13f, dialog)
                        box(ParentColors.accent, 205f, 196f, 62f, 26f, 13f, dialog)
                        label(words.allow, 236f, 213f, 11f, Color.White, bold = true, alpha = dialog, center = true, maxWidth = 50f)
                    }
                }
                if (done > 0f) {
                    val pop = done * (1 + 0.18f * sin(done * PI).toFloat())
                    drawCircle(ParentColors.green, 22f * pop, Offset(180f, 192f), alpha = done)
                    drawPath(Path().apply { moveTo(171f, 192f); lineTo(177f, 198f); lineTo(190f, 185f) },
                        Color.White, alpha = done, style = Stroke(3.5f, cap = StrokeCap.Round))
                }
            }
            if (!still) {
                var at = mix(Offset(318f, 262f), Offset(246f, 182f), seg(t, 0.06f, 0.20f))
                at = mix(at, Offset(262f, 108f), seg(t, 0.38f, 0.48f))
                at = mix(at, Offset(240f, 214f), seg(t, 0.64f, 0.72f))
                val down = press(t, 0.24f) + press(t, 0.52f) + press(t, 0.76f)
                finger(at, down, seg(t, 0.04f, 0.08f) * (1 - seg(t, 0.80f, 0.86f)))
                playbackBar(t, 240f)
            }
        }
    }
}

/** The clip and written steps for switching on app protection under Android's Accessibility settings. */
@Composable
fun ProtectionGuide(enabled: Boolean) {
    val progress = illustrationProgress()
    val still = enabled || !ValueAnimator.areAnimatorsEnabled()
    val description = stringResource(R.string.guard_guide_description)
    val service = stringResource(R.string.guard_service_label)
    val words = DemoWords(
        listTitle = stringResource(R.string.guide_accessibility),
        section = stringResource(R.string.guide_downloaded),
        row = service,
        pageTitle = service,
        toggle = stringResource(R.string.guide_use_protection),
        allow = stringResource(R.string.guide_allow),
    )
    val current by demoStep(progress, still)
    Column(Modifier.clearAndSetSemantics { contentDescription = description }) {
        SettingsDemo(words, progress, still)
        Spacer(Modifier.height(14.dp))
        GuideStep(1, stringResource(R.string.guide_step_open), current)
        GuideStep(2, stringResource(R.string.guide_step_switch), current)
        GuideStep(3, stringResource(R.string.guide_step_allow), current)
    }
}

/**
 * The clip and written steps for letting Kid Space silence notifications: Android shows a list of
 * every app, the parent opens Kid Space in it, switches it on, then confirms.
 */
@Composable
fun DndGuide() {
    val progress = illustrationProgress()
    val still = !ValueAnimator.areAnimatorsEnabled()
    val name = stringResource(R.string.app_name)
    // Android 15 renamed this page; the clip uses the name the parent will actually see.
    val modes = Build.VERSION.SDK_INT >= 35
    val screen = stringResource(if (modes) R.string.guide_modes_screen else R.string.guide_dnd_screen)
    val toggle = stringResource(if (modes) R.string.guide_modes_toggle else R.string.guide_dnd_toggle)
    val description = stringResource(R.string.dnd_guide_description, screen, toggle)
    val words = DemoWords(
        listTitle = screen,
        section = null,
        row = name,
        pageTitle = name,
        toggle = toggle,
        allow = stringResource(R.string.guide_allow),
    )
    val current by demoStep(progress, still)
    Column(Modifier.clearAndSetSemantics { contentDescription = description }) {
        SettingsDemo(words, progress, still)
        Spacer(Modifier.height(12.dp))
        GuideStep(1, stringResource(R.string.guide_step_tap_app), current)
        GuideStep(2, stringResource(R.string.guide_step_switch), current)
        GuideStep(3, stringResource(R.string.guide_step_allow), current)
    }
}

/** One line of the written version of a demo. The line being shown in the clip is darker. */
@Composable
private fun GuideStep(number: Int, text: String, current: Int) {
    val now = current == number - 1
    val quiet = current >= 0 && !now
    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("$number", Modifier.width(22.dp), color = if (now) ParentColors.accent else ParentColors.muted, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(text, color = if (quiet) ParentColors.muted else ParentColors.ink, fontSize = 15.sp, lineHeight = 21.sp,
            fontWeight = if (now) FontWeight.Bold else FontWeight.Normal)
    }
}

/** A short looping recording of Android's home-app prompt: pick Kid Space, then Set as default. */
@Composable
fun HomeChoiceGuide() {
    val progress = illustrationProgress()
    val moving = ValueAnimator.areAnimatorsEnabled()
    val description = stringResource(R.string.handoff_choice)
    val name = stringResource(R.string.app_name)
    val setDefault = stringResource(R.string.guide_default)
    Column(Modifier.clearAndSetSemantics { contentDescription = description }) {
        Text(description, color = ParentColors.muted, fontSize = 15.sp, lineHeight = 21.sp)
        Spacer(Modifier.height(16.dp))
        Canvas(Modifier.fillMaxWidth().aspectRatio(360f / 214f).clip(RoundedCornerShape(20.dp)).background(ParentColors.blueSoft)) {
            val t = progress.value
            val selected = t > 0.28f && t < 0.97f
            val ready = seg(t, 0.28f, 0.33f) * (1 - seg(t, 0.96f, 1f))
            scale(size.width / 360f, pivot = Offset.Zero) {
                // The lower half of a phone, with Android's prompt sliding up from its edge.
                phone(71f, -84f, 218f, 270f, 24f, SolidColor(Color(0xFFC3CCDD)))
                box(Color.White, 71f, 34f, 218f, 152f, 20f)
                box(ParentColors.sage, 87f, 52f, 112f, 9f, 4.5f)
                mark(87f, 76f, 24f)
                label(name, 121f, 92.5f, 12.5f, ParentColors.ink, bold = true, maxWidth = 120f)
                radio(Offset(267f, 88f), selected)
                drawCircle(ParentColors.sage, 12f, Offset(99f, 124f))
                box(ParentColors.sage, 121f, 119.5f, 78f, 9f, 4.5f)
                radio(Offset(267f, 124f), false)
                box(ParentColors.sage, 171f, 146f, 110f, 28f, 14f)
                box(ParentColors.accent, 171f, 146f, 110f, 28f, 14f, ready)
                label(setDefault, 226f, 164f, 10.5f, ParentColors.muted, bold = true, alpha = 1 - ready, center = true, maxWidth = 94f)
                label(setDefault, 226f, 164f, 10.5f, Color.White, bold = true, alpha = ready, center = true, maxWidth = 94f)
                if (moving) {
                    var at = mix(Offset(322f, 226f), Offset(267f, 92f), seg(t, 0.08f, 0.22f))
                    at = mix(at, Offset(230f, 164f), seg(t, 0.38f, 0.50f))
                    finger(at, press(t, 0.27f) + press(t, 0.56f), seg(t, 0.05f, 0.09f) * (1 - seg(t, 0.64f, 0.72f)))
                    playbackBar(t, 204f)
                }
            }
        }
    }
}
