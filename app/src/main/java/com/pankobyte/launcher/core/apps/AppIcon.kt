package com.pankobyte.launcher.core.apps

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.pankobyte.launcher.AppContainer

/** App icon as the system provides it. The caller sets size and clip shape through [modifier]. */
@Composable
fun AppIcon(entry: AppEntry, modifier: Modifier = Modifier) {
    val repo = AppContainer.apps
    val bitmap by produceState(repo.cachedIcon(entry), entry.key) {
        if (value == null) value = repo.icon(entry)
    }
    val image = bitmap
    if (image != null) {
        Image(bitmap = image, contentDescription = null, modifier = modifier, contentScale = ContentScale.Crop)
    } else {
        Box(modifier)
    }
}
