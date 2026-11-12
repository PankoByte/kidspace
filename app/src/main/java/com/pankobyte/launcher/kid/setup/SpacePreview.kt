package com.pankobyte.launcher.kid.setup

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
import androidx.compose.ui.graphics.Color
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
import com.pankobyte.launcher.core.theme.LineIcons
import com.pankobyte.launcher.core.theme.ParentColors

/** A bounded group of apps: the same visual idea in the brand, introduction and review. */
@Composable
fun SpaceMark(modifier: Modifier = Modifier) {
    Box(modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(ParentColors.accent), contentAlignment = Alignment.Center) {
        Icon(LineIcons.Grid, null, tint = Color.White, modifier = Modifier.size(22.dp))
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
