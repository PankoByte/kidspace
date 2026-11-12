package com.pankobyte.launcher.kid.setup

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pankobyte.launcher.R
import com.pankobyte.launcher.core.apps.AppEntry
import com.pankobyte.launcher.core.apps.AppIcon
import com.pankobyte.launcher.core.theme.ParentColors

/** The app's logo: the frog from the app icon (ic_launcher_foreground.xml), drawn in code. */
@Composable
fun SpaceMark(modifier: Modifier = Modifier) {
    Canvas(modifier.size(36.dp)) { drawLogo(0f, 0f, size.width) }
}

private val LogoCream = Color(0xFFFBF4E4)
private val LogoGreen = Color(0xFF6FBF73)
private val LogoCheek = Color(0xBFF4A6A0)

/**
 * The logo as a rounded tile of [side] with its top left at [x], [y]: the same frog as the app
 * icon, for drawings that show the app, such as the settings guides in setup.
 */
fun DrawScope.drawLogo(x: Float, y: Float, side: Float, alpha: Float = 1f) {
    drawRoundRect(LogoCream, Offset(x, y), Size(side, side), CornerRadius(side * 0.3f), alpha = alpha)
    translate(x, y) {
        // Same numbers as the icon, whose visible part is the middle 72 of a 108 grid.
        scale(side / 72f, pivot = Offset.Zero) {
            translate(-18f, -20.5f) {
                for (cx in listOf(39.3f, 68.7f)) drawCircle(LogoGreen, 11.025f, Offset(cx, 42.05f), alpha = alpha)
                drawOval(LogoGreen, Offset(25.65f, 41.525f), Size(56.7f, 40.95f), alpha = alpha)
                for (cx in listOf(39.3f, 68.7f)) drawCircle(Color.White, 7.14f, Offset(cx, 42.05f), alpha = alpha)
                for (cx in listOf(39.825f, 68.175f)) {
                    drawCircle(ParentColors.ink, 3.78f, Offset(cx, 42.575f), alpha = alpha)
                    drawCircle(Color.White, 1.36f, Offset(cx - 1.134f, 41.25f), alpha = alpha)
                }
                for (cx in listOf(33.525f, 74.475f)) drawCircle(LogoCheek, 3.57f, Offset(cx, 64.1f), alpha = alpha)
                drawArc(
                    ParentColors.ink, 15f, 150f, false, Offset(36.15f, 47.3f), Size(35.7f, 23.1f),
                    alpha = alpha, style = Stroke(2.31f, cap = StrokeCap.Round),
                )
            }
        }
    }
}

/**
 * The apps the child will get, as icons only. The panel keeps one height however many apps are
 * chosen: more apps make the icons smaller, never the card taller.
 */
@Composable
fun SpacePreview(apps: List<AppEntry>, onEdit: () -> Unit) {
    val names = apps.joinToString { it.label }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(ParentColors.surface).border(1.dp, ParentColors.border, RoundedCornerShape(24.dp))) {
        Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 8.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.preview_title), Modifier.weight(1f), color = ParentColors.muted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Box(Modifier.clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClick = onEdit).padding(12.dp)) {
                Text(stringResource(R.string.home_edit_apps), color = ParentColors.accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
        BoxWithConstraints(
            Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, bottom = 10.dp).height(PREVIEW_HEIGHT)
                .clip(RoundedCornerShape(18.dp)).background(ParentColors.preview).padding(12.dp)
                .clearAndSetSemantics { contentDescription = names },
            contentAlignment = Alignment.Center,
        ) {
            // Try every column count and keep the one that gives the largest square cell.
            var columns = 1
            var cell = 0.dp
            for (candidate in 1..apps.size) {
                val rows = (apps.size + candidate - 1) / candidate
                val fits = minOf(maxWidth / candidate, maxHeight / rows)
                if (fits > cell) { cell = fits; columns = candidate }
            }
            cell = minOf(cell, 72.dp)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                for (row in apps.chunked(columns)) {
                    Row {
                        for (entry in row) {
                            Box(Modifier.size(cell), contentAlignment = Alignment.Center) {
                                Box(Modifier.size(cell * 0.84f).clip(RoundedCornerShape(cell * 0.24f)).background(Color.White), contentAlignment = Alignment.Center) {
                                    AppIcon(entry, Modifier.size(cell * 0.58f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private val PREVIEW_HEIGHT = 132.dp
