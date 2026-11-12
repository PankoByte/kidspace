package com.pankobyte.launcher.kid.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pankobyte.launcher.KidRoute
import com.pankobyte.launcher.LauncherViewModel
import com.pankobyte.launcher.R
import com.pankobyte.launcher.core.apps.AppEntry
import com.pankobyte.launcher.core.apps.AppIcon
import com.pankobyte.launcher.core.theme.Dimens
import com.pankobyte.launcher.core.theme.KidDisplayFont as DisplayFont
import com.pankobyte.launcher.core.theme.KidColors
import com.pankobyte.launcher.core.theme.LineIcons
import com.pankobyte.launcher.core.theme.NightColors
import com.pankobyte.launcher.core.ui.Hills
import com.pankobyte.launcher.core.ui.NightSky
import com.pankobyte.launcher.core.ui.Sunny
import com.pankobyte.launcher.core.ui.SunnyMood
import com.pankobyte.launcher.kid.pin.KidPinScreen
import com.pankobyte.launcher.kid.session.KidSessionState
import com.pankobyte.launcher.kid.session.KidSnapshot
import com.pankobyte.launcher.kid.session.NO_LIMIT
import com.pankobyte.launcher.kid.session.WARNING_MS
import com.pankobyte.launcher.kid.session.minutesLeft

/** How far a raised card's solid shadow sticks out to the right and below. */
private val shadowX = 3.dp
private val shadowY = 4.dp

/** Height of the strip at the bottom of the kid home that holds the Grown-ups button. */
private val bottomStrip = 84.dp

/** Everything shown while Kid mode is on. No adult screen is reachable from here without the PIN. */
@Composable
fun KidRoot(vm: LauncherViewModel, snapshot: KidSnapshot) {
    val timesUp = snapshot.state == KidSessionState.TIMES_UP
    // Back never leaves Kid mode. It only steps back to the kid home.
    BackHandler { vm.kidRoute = KidRoute.Home }

    when (vm.kidRoute) {
        KidRoute.Pin -> KidPinScreen(
            vm = vm,
            onBack = { vm.kidRoute = KidRoute.Home },
            onSuccess = { if (timesUp) vm.kidRoute = KidRoute.ParentChoice else vm.leaveKidMode() },
        )
        KidRoute.ParentChoice -> if (timesUp) ParentChoiceScreen(vm) else KidHomeScreen(vm, snapshot)
        KidRoute.Home -> if (timesUp) TimesUpScreen(vm) else KidHomeScreen(vm, snapshot)
    }
}

/**
 * The kid home: a greeting, the play time left, and the allowed apps. In the last minutes of
 * play time the sky turns warm and the words change, as a gentle warning.
 */
@Composable
private fun KidHomeScreen(vm: LauncherViewModel, snapshot: KidSnapshot) {
    val apps by vm.apps.collectAsStateWithLifecycle()
    val shown = remember(apps, snapshot.allowedPackages) {
        apps.filter { it.packageName in snapshot.allowedPackages }.distinctBy { it.packageName }
    }
    // Two big tiles a row for a few apps, four small round ones once there are many.
    val compact = shown.size > 6
    val timed = snapshot.remainingMs != NO_LIMIT
    val low = timed && snapshot.remainingMs <= WARNING_MS
    val edge = if (low) KidColors.lowEdge else KidColors.ink

    Box(Modifier.fillMaxSize().background(if (low) KidColors.lowSky else KidColors.sky)) {
        // Sunny sits beside the greeting. Near the end it turns orange and sleepy.
        Sunny(
            Modifier.align(Alignment.TopEnd).systemBarsPadding().padding(top = 12.dp, end = 24.dp).size(80.dp),
            mood = if (low) SunnyMood.Sleepy else SunnyMood.Happy,
            color = if (low) KidColors.lowSun else KidColors.sun,
        )
        if (compact) {
            // A flat strip of grass, so a long list of apps scrolls behind a clean edge.
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(if (low) KidColors.lowHillBack else KidColors.hillBack)) {
                Spacer(Modifier.height(bottomStrip))
                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
        } else {
            Hills(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(170.dp),
                back = if (low) KidColors.lowHillBack else KidColors.hillBack,
                front = if (low) KidColors.lowHillFront else KidColors.hillFront,
            )
        }
        Column(Modifier.align(Alignment.TopCenter).systemBarsPadding().widthIn(max = Dimens.contentMaxWidth).fillMaxSize().padding(horizontal = Dimens.kidMargin)) {
            Spacer(Modifier.height(28.dp))
            Text(
                stringResource(if (low) R.string.kid_low_title else R.string.kid_greeting),
                color = KidColors.ink,
                fontFamily = DisplayFont,
                fontWeight = FontWeight.Bold,
                fontSize = 44.sp,
                lineHeight = 48.sp,
            )
            if (timed) {
                Spacer(Modifier.height(20.dp))
                PlayTimeCard(snapshot, low, edge)
            }
            Spacer(Modifier.height(16.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(if (compact) 4 else 2),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else Dimens.kidTileGap),
                verticalArrangement = Arrangement.spacedBy(Dimens.kidTileGap),
                contentPadding = PaddingValues(top = 2.dp, bottom = 12.dp),
            ) {
                items(shown, key = { it.key }) { entry ->
                    if (compact) KidAppSmall(entry, edge) { vm.launchKidApp(entry) }
                    else KidAppTile(entry, edge) { vm.launchKidApp(entry) }
                }
            }
            Box(Modifier.height(bottomStrip), contentAlignment = Alignment.CenterStart) {
                GrownUpsPill(night = false) { vm.kidRoute = KidRoute.Pin }
            }
        }
    }
}

/** White card with a dark outline and a solid shadow. [press] from 0 to 1 pushes it down onto the shadow. */
@Composable
private fun RaisedCard(
    modifier: Modifier,
    radius: Dp,
    edge: Color,
    press: Float = 0f,
    fill: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    Box(
        modifier.drawBehind {
            drawRoundRect(
                edge,
                topLeft = Offset(shadowX.toPx(), shadowY.toPx()),
                size = Size(size.width - shadowX.toPx(), size.height - shadowY.toPx()),
                cornerRadius = CornerRadius(radius.toPx()),
            )
        },
    ) {
        Box(
            Modifier
                .padding(end = shadowX, bottom = shadowY)
                .then(if (fill) Modifier.fillMaxSize() else Modifier.fillMaxWidth())
                .graphicsLayer {
                    translationX = shadowX.toPx() * press
                    translationY = shadowY.toPx() * press
                }
                .clip(shape)
                .background(KidColors.card)
                .border(2.dp, edge, shape),
            content = content,
        )
    }
}

/** Minutes left, in big numbers, over a bar that empties as play time runs down. */
@Composable
private fun PlayTimeCard(snapshot: KidSnapshot, low: Boolean, edge: Color) {
    val minutes = minutesLeft(snapshot.remainingMs)
    val fraction = if (snapshot.totalMs > 0) (snapshot.remainingMs.toFloat() / snapshot.totalMs).coerceIn(0f, 1f) else 0f
    val barColor = if (low) KidColors.lowSun else KidColors.sun
    RaisedCard(Modifier.fillMaxWidth(), radius = 20.dp, edge = edge) {
        Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 14.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    stringResource(R.string.kid_minutes, minutes),
                    modifier = Modifier.alignByBaseline(),
                    color = KidColors.ink,
                    fontFamily = DisplayFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.kid_left),
                    modifier = Modifier.alignByBaseline(),
                    color = KidColors.ink,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Spacer(Modifier.height(6.dp))
            Canvas(Modifier.fillMaxWidth().height(24.dp)) {
                val bar = 12.dp.toPx()
                val knob = size.height / 2 - 1.dp.toPx()
                val y = size.height / 2
                // The knob stays fully inside the track at both ends.
                val x = size.height / 2 + (size.width - size.height) * fraction
                drawRoundRect(KidColors.barTrack, Offset(0f, y - bar / 2), Size(size.width, bar), CornerRadius(bar / 2))
                drawRoundRect(barColor, Offset(0f, y - bar / 2), Size(x, bar), CornerRadius(bar / 2))
                drawCircle(barColor, knob, Offset(x, y))
                drawCircle(edge, knob, Offset(x, y), style = Stroke(2.dp.toPx()))
            }
        }
    }
}

/** How far a tile is pressed in, from 0 to 1. The only decorative motion in Kid mode. */
@Composable
private fun pressAmount(source: MutableInteractionSource): Float {
    val pressed by source.collectIsPressedAsState()
    val amount by animateFloatAsState(if (pressed) 1f else 0f, tween(90), label = "kidPress")
    return amount
}

/** Big square tile, two to a row. */
@Composable
private fun KidAppTile(entry: AppEntry, edge: Color, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    RaisedCard(Modifier.aspectRatio(1f), radius = Dimens.kidTileRadius, edge = edge, press = pressAmount(source), fill = true) {
        Column(
            Modifier
                .fillMaxSize()
                .clickable(interactionSource = source, indication = LocalIndication.current, role = Role.Button, onClick = onClick)
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            AppIcon(entry, Modifier.size(76.dp).clip(CircleShape))
            Spacer(Modifier.height(10.dp))
            Text(
                entry.label,
                color = KidColors.ink,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Small round icon with its name underneath, four to a row. */
@Composable
private fun KidAppSmall(entry: AppEntry, edge: Color, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val press = pressAmount(source)
    val disc = 62.dp
    Column(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(disc + shadowX, disc + shadowY)
                .drawBehind {
                    drawCircle(edge, disc.toPx() / 2, Offset(disc.toPx() / 2 + shadowX.toPx(), disc.toPx() / 2 + shadowY.toPx()))
                },
        ) {
            Box(
                Modifier
                    .size(disc)
                    .graphicsLayer {
                        translationX = shadowX.toPx() * press
                        translationY = shadowY.toPx() * press
                    }
                    .clip(CircleShape)
                    .background(KidColors.card)
                    .border(2.dp, edge, CircleShape)
                    .padding(6.dp),
            ) {
                AppIcon(entry, Modifier.fillMaxSize().clip(CircleShape))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            entry.label,
            color = KidColors.ink,
            fontWeight = FontWeight.Bold,
            fontSize = 12.5.sp,
            lineHeight = 15.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** The way to the PIN screen. Dark on the daytime home, outlined on the night screens. */
@Composable
private fun GrownUpsPill(night: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .height(Dimens.kidPillHeight)
            .clip(CircleShape)
            .background(if (night) NightColors.raised else KidColors.ink)
            .then(if (night) Modifier.border(1.dp, NightColors.text, CircleShape) else Modifier)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(LineIcons.Lock, null, tint = if (night) NightColors.text else KidColors.onInk, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            stringResource(R.string.kid_grown_ups),
            color = if (night) NightColors.text else KidColors.onInk,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
        )
    }
}

/** Shown when play time has run out: night has fallen. Only the Grown-ups button does anything. */
@Composable
private fun TimesUpScreen(vm: LauncherViewModel) {
    Box(Modifier.fillMaxSize()) {
        NightSky()
        Column(Modifier.fillMaxSize().systemBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.weight(0.54f))
            Text(
                stringResource(R.string.times_up_title),
                color = NightColors.text,
                fontFamily = DisplayFont,
                fontWeight = FontWeight.Bold,
                fontSize = 56.sp,
                lineHeight = 60.sp,
            )
            Text(stringResource(R.string.times_up_body), color = NightColors.muted, fontSize = 25.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.weight(0.46f))
            GrownUpsPill(night = true) { vm.kidRoute = KidRoute.Pin }
            Spacer(Modifier.height(28.dp))
        }
    }
}

/** After a correct PIN on the time's-up screen: 15 more minutes, or leave Kid mode. */
@Composable
private fun ParentChoiceScreen(vm: LauncherViewModel) {
    Box(Modifier.fillMaxSize().background(NightColors.bg)) {
        Column(
            Modifier.align(Alignment.TopCenter).systemBarsPadding().widthIn(max = Dimens.contentMaxWidth).fillMaxSize().padding(horizontal = Dimens.kidMargin),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            Text(
                stringResource(R.string.times_up_title),
                color = NightColors.text,
                fontFamily = DisplayFont,
                fontWeight = FontWeight.Bold,
                fontSize = 40.sp,
            )
            Spacer(Modifier.height(28.dp))
            ChoiceButton(stringResource(R.string.parent_more), filled = true) { vm.addKidTime() }
            Spacer(Modifier.height(12.dp))
            ChoiceButton(stringResource(R.string.parent_leave), filled = false) { vm.leaveKidMode() }
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun ChoiceButton(label: String, filled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(shape)
            .background(if (filled) NightColors.text else NightColors.raised)
            .then(if (filled) Modifier else Modifier.border(1.dp, NightColors.text, shape))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (filled) NightColors.bg else NightColors.text, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}
