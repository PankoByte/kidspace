package com.pankobyte.launcher.core.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pankobyte.launcher.R
import com.pankobyte.launcher.Onboard
import com.pankobyte.launcher.core.theme.Dimens
import com.pankobyte.launcher.core.theme.DisplayFont
import com.pankobyte.launcher.core.theme.LineIcons
import com.pankobyte.launcher.core.theme.ParentColors

/** Opens a system settings page or dialog and ignores the result. State is re-read on resume. */
@Composable
fun rememberSettingsLauncher(): (Intent) -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {}
    return { intent -> runCatching { launcher.launch(intent) } }
}

/** [text] with the first occurrence of [part] in bold. */
fun boldPart(text: String, part: String): AnnotatedString = buildAnnotatedString {
    append(text)
    val start = text.indexOf(part)
    if (part.isNotEmpty() && start >= 0) addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, start + part.length)
}

/** Full-screen column in the parent look. [hills] draws the soft hills behind the bottom button. */
@Composable
fun ParentScreen(
    modifier: Modifier = Modifier,
    hills: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(Modifier.fillMaxSize().background(ParentColors.bg)) {
        if (hills) {
            Hills(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(96.dp),
                back = ParentColors.hillBack,
                front = ParentColors.hillFront,
            )
        }
        Column(
            // On tablets and unfolded phones the page stays a readable column in the middle.
            modifier = modifier
                .align(Alignment.TopCenter)
                .systemBarsPadding()
                .displayCutoutPadding()
                .imePadding()
                .widthIn(max = Dimens.contentMaxWidth)
                .fillMaxSize()
                .padding(horizontal = Dimens.parentMargin),
            content = content,
        )
    }
}

/**
 * Top row of a parent screen: a back arrow, then either the setup progress ([step] counts from 0
 * out of [totalSteps]) or a [title]. [serifTitle] is for a screen whose header is its only title.
 */
@Composable
fun FlowHeader(
    onBack: (() -> Unit)?,
    step: Int? = null,
    totalSteps: Int = 0,
    title: String? = null,
    serifTitle: Boolean = false,
) {
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp).height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            Box(
                // Pulled left so the arrow itself, not its touch area, lines up with the page margin.
                Modifier.offset(x = (-12).dp).size(44.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(LineIcons.Back, stringResource(R.string.action_back), tint = ParentColors.ink, modifier = Modifier.size(22.dp))
            }
        }
        if (step != null) {
            val position = Onboard.position(step).coerceAtLeast(1)
            val count = (totalSteps - 1).coerceAtLeast(1)
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.setup_step, position, count),
                    color = ParentColors.ink,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(count) { index ->
                        Box(
                            Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(if (index < position) ParentColors.accent else ParentColors.border),
                        )
                    }
                }
            }
        } else if (title != null) {
            Text(
                title,
                color = ParentColors.ink,
                fontSize = if (serifTitle) 24.sp else 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = if (serifTitle) DisplayFont else null,
            )
        }
    }
}

/** Parent screen title. [large] is used for the welcome headline. */
@Composable
fun ParentTitle(text: String, modifier: Modifier = Modifier, large: Boolean = false) {
    Text(
        text = text,
        modifier = modifier,
        color = ParentColors.ink,
        fontFamily = DisplayFont,
        fontWeight = FontWeight.Bold,
        fontSize = if (large) 38.sp else 30.sp,
        lineHeight = if (large) 44.sp else 36.sp,
        // Large type is tracked in a little, as on iOS large titles.
        letterSpacing = if (large) (-0.6).sp else (-0.4).sp,
    )
}

@Composable
fun ParentBody(text: String, modifier: Modifier = Modifier) {
    Text(text = text, modifier = modifier, color = ParentColors.muted, fontSize = 16.sp, lineHeight = 24.sp)
}

/** A quiet sentence-case label above a group or inside a card. */
@Composable
fun CapsLabel(text: String, modifier: Modifier = Modifier, color: Color = ParentColors.muted) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp,
    )
}

/** Full-width filled button. [leading] is drawn before the label and told whether the button is enabled. */
@Composable
fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = Dimens.parentButtonHeight,
    leading: (@Composable (enabled: Boolean) -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = height)
            .clip(RoundedCornerShape(Dimens.parentButtonRadius))
            .background(if (enabled) ParentColors.accent else ParentColors.sage)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (leading != null) {
            leading(enabled)
            Spacer(Modifier.width(10.dp))
        }
        Text(
            label,
            color = if (enabled) ParentColors.onInk else ParentColors.disabledText,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

/** Text-only button for a secondary action such as Not now. */
@Composable
fun TextLink(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 52.dp)
            .clip(RoundedCornerShape(Dimens.parentButtonRadius))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = ParentColors.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

/** Small outlined button, as on the empty search result. */
@Composable
fun OutlineButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(shape)
            .background(ParentColors.surface)
            .border(1.5.dp, ParentColors.ink, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = ParentColors.ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

/** Rounded panel with a hairline border. Colours change for warnings and errors. */
@Composable
fun ParentCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    background: Color = ParentColors.surface,
    border: Color = ParentColors.border,
    borderWidth: Dp = 1.dp,
    padding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(Dimens.parentCardRadius)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .border(borderWidth, border, shape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

@Composable
fun ParentDivider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(ParentColors.sageSoft))
}

/** A line icon in a circle. */
@Composable
fun IconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    background: Color = ParentColors.sageSoft,
    tint: Color = ParentColors.ink,
) {
    Box(modifier.size(size).clip(CircleShape).background(background), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

/** Round tick mark: a ring when off, filled when on. */
@Composable
fun CheckCircle(checked: Boolean, modifier: Modifier = Modifier, size: Dp = 26.dp) {
    // The filled disc springs in over the ring, so a tap visibly lands.
    val fill by animateFloatAsState(if (checked) 1f else 0f, spring(dampingRatio = 0.55f, stiffness = 700f), label = "check")
    Box(modifier.size(size).border(1.5.dp, ParentColors.muted, CircleShape), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(size)
                .graphicsLayer {
                    scaleX = fill
                    scaleY = fill
                    alpha = fill.coerceIn(0f, 1f)
                }
                .clip(CircleShape)
                .background(ParentColors.ink),
            contentAlignment = Alignment.Center,
        ) {
            Icon(LineIcons.Check, null, tint = ParentColors.onInk, modifier = Modifier.size(size * 0.6f))
        }
    }
}

/** On/off switch with a white knob. */
@Composable
fun ParentSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val knobX by animateDpAsState(if (checked) 22.dp else 2.dp, label = "switchKnob")
    Box(
        // Taller than the track so the touch area is a full 48 dp.
        Modifier
            .size(52.dp, 48.dp)
            .toggleable(value = checked, interactionSource = null, indication = null, role = Role.Switch, onValueChange = onCheckedChange),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(52.dp, 32.dp).clip(CircleShape).background(if (checked) ParentColors.ink else ParentColors.switchOff),
        ) {
            Box(Modifier.offset { IntOffset(knobX.roundToPx(), 2.dp.roundToPx()) }.size(28.dp).clip(CircleShape).background(Color.White))
        }
    }
}

/** Short message on a tinted pill, used for PIN errors. */
@Composable
fun MessagePill(text: String, background: Color, color: Color, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.clip(RoundedCornerShape(10.dp)).background(background).padding(horizontal = 14.dp, vertical = 8.dp),
        color = color,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
    )
}

/** A dot and a short status, as on the Kid mode guard row in Settings. */
@Composable
fun StatusChip(text: String, background: Color, dot: Color, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(CircleShape).background(background).padding(horizontal = 9.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(dot))
        Spacer(Modifier.width(6.dp))
        Text(text, color = ParentColors.ink, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
    }
}
