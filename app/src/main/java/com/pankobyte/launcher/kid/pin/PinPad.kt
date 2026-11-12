package com.pankobyte.launcher.kid.pin

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pankobyte.launcher.R
import com.pankobyte.launcher.core.theme.Dimens
import com.pankobyte.launcher.core.theme.DisplayFont
import com.pankobyte.launcher.core.theme.LineIcons
import com.pankobyte.launcher.core.theme.NightColors
import com.pankobyte.launcher.core.theme.ParentColors

/** Digits typed so far, plus the shake and red rings shown after a wrong PIN. */
@Stable
class PinEntryState {
    var digits by mutableStateOf("")
        private set

    /** True from a wrong PIN until the next digit is typed. Turns the dots into red rings. */
    var error by mutableStateOf(false)
        private set
    val shake = Animatable(0f)

    /** Adds a digit and returns the full PIN once [PIN_LENGTH] digits are in. */
    fun add(digit: Char): String? {
        if (digits.length >= PIN_LENGTH) return null
        error = false
        digits += digit
        return digits.takeIf { it.length == PIN_LENGTH }
    }

    fun backspace() {
        digits = digits.dropLast(1)
    }

    fun clear() {
        digits = ""
    }

    /** Shakes the dots once, then clears them and marks the error. */
    suspend fun wrong() {
        shake.animateTo(
            0f,
            keyframes {
                durationMillis = 240
                -18f at 40
                18f at 100
                -10f at 160
                10f at 200
            },
        )
        clear()
        error = true
    }
}

/** Four ring dots that fill as digits are typed. [night] is the dark "Grown-ups only" look. */
@Composable
fun PinDots(state: PinEntryState, night: Boolean, modifier: Modifier = Modifier) {
    val filled = if (night) NightColors.text else ParentColors.ink
    val ring = when {
        state.error -> if (night) NightColors.error else ParentColors.errorRing
        night -> NightColors.muted
        else -> ParentColors.muted
    }
    val spoken = stringResource(R.string.pin_progress, state.digits.length, PIN_LENGTH)
    Row(
        modifier
            .graphicsLayer { translationX = state.shake.value * density }
            .clearAndSetSemantics { contentDescription = spoken },
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        repeat(PIN_LENGTH) { index ->
            val fill by animateFloatAsState(
                if (index < state.digits.length) 1f else 0f,
                spring(dampingRatio = 0.5f, stiffness = 800f),
                label = "pinDot",
            )
            Box(Modifier.size(Dimens.pinDot).border(1.5.dp, ring, CircleShape)) {
                Box(
                    Modifier
                        .size(Dimens.pinDot)
                        .graphicsLayer {
                            scaleX = fill
                            scaleY = fill
                            alpha = fill.coerceIn(0f, 1f)
                        }
                        .clip(CircleShape)
                        .background(filled),
                )
            }
        }
    }
}

/** Number pad: 1 to 9, then 0 and backspace. Calls [onComplete] with the PIN when four digits are in. */
@Composable
fun PinPad(
    state: PinEntryState,
    night: Boolean,
    enabled: Boolean,
    onComplete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val content = if (night) NightColors.text else ParentColors.ink
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (row in listOf("123", "456", "789")) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (digit in row) Digit(digit, night, enabled) { state.add(digit)?.let(onComplete) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Spacer(Modifier.weight(1f))
            Digit('0', night, enabled) { state.add('0')?.let(onComplete) }
            Box(
                Modifier
                    .weight(1f)
                    .height(Dimens.pinKeyHeight)
                    .clip(RoundedCornerShape(Dimens.pinKeyRadius))
                    .clickable(enabled = enabled, role = Role.Button) { state.backspace() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    LineIcons.Backspace,
                    stringResource(R.string.cd_backspace),
                    tint = content,
                    modifier = Modifier.alpha(if (enabled) 1f else 0.4f),
                )
            }
        }
    }
}

@Composable
private fun RowScope.Digit(digit: Char, night: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(Dimens.pinKeyRadius)
    val look = when {
        night -> Modifier.background(NightColors.raised)
        enabled -> Modifier.background(ParentColors.surface).border(1.dp, ParentColors.border, shape)
        else -> Modifier.background(ParentColors.sageSoft)
    }
    Box(
        Modifier
            .weight(1f)
            .height(Dimens.pinKeyHeight)
            .alpha(if (night && !enabled) 0.45f else 1f)
            .clip(shape)
            .then(look)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            digit.toString(),
            color = when {
                night -> NightColors.text
                enabled -> ParentColors.ink
                else -> ParentColors.disabledText
            },
            fontFamily = DisplayFont,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
        )
    }
}

/** Shown in place of the dots while the PIN is locked after too many wrong tries. */
@Composable
fun PinLockCard(lockedMs: Long, night: Boolean, modifier: Modifier = Modifier) {
    val seconds = ((lockedMs + 999) / 1000).toInt()
    val shape = RoundedCornerShape(Dimens.parentCardRadius)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .then(
                if (night) Modifier.background(NightColors.raised)
                else Modifier.background(ParentColors.surface).border(1.dp, ParentColors.border, shape),
            )
            .padding(vertical = 20.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.pin_locked_title),
            color = if (night) NightColors.error else ParentColors.error,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            stringResource(R.string.pin_locked_body),
            color = if (night) NightColors.muted else ParentColors.muted,
            fontSize = 13.sp,
        )
        Text(
            "%02d:%02d".format(seconds / 60, seconds % 60),
            color = if (night) NightColors.text else ParentColors.ink,
            fontFamily = DisplayFont,
            fontWeight = FontWeight.Bold,
            fontSize = 42.sp,
            lineHeight = 50.sp,
        )
        Text(
            stringResource(R.string.pin_locked_spoken, seconds / 60, seconds % 60),
            color = if (night) NightColors.muted else ParentColors.muted,
            fontSize = 12.sp,
        )
    }
}
