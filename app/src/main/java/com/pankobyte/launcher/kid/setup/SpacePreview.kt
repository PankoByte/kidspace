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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
    val green = Color(0xFF6FBF73)
    val cheek = Color(0xBFF4A6A0)
    Canvas(modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(Color(0xFFFBF4E4))) {
        // Same numbers as the icon, whose visible part is the middle 72 of a 108 grid.
        scale(size.width / 72f, pivot = Offset.Zero) {
            translate(-18f, -20.5f) {
                for (x in listOf(39.3f, 68.7f)) drawCircle(green, 11.025f, Offset(x, 42.05f))
                drawOval(green, Offset(25.65f, 41.525f), Size(56.7f, 40.95f))
                for (x in listOf(39.3f, 68.7f)) drawCircle(Color.White, 7.14f, Offset(x, 42.05f))
                for (x in listOf(39.825f, 68.175f)) {
                    drawCircle(ParentColors.ink, 3.78f, Offset(x, 42.575f))
                    drawCircle(Color.White, 1.36f, Offset(x - 1.134f, 41.25f))
                }
                for (x in listOf(33.525f, 74.475f)) drawCircle(cheek, 3.57f, Offset(x, 64.1f))
                drawArc(
                    ParentColors.ink, 15f, 150f, false, Offset(36.15f, 47.3f), Size(35.7f, 23.1f),
                    style = Stroke(2.31f, cap = StrokeCap.Round),
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
