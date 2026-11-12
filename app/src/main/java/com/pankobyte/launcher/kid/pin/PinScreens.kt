package com.pankobyte.launcher.kid.pin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pankobyte.launcher.LauncherViewModel
import com.pankobyte.launcher.Onboard
import com.pankobyte.launcher.PinResult
import com.pankobyte.launcher.R
import com.pankobyte.launcher.core.theme.Dimens
import com.pankobyte.launcher.core.theme.DisplayFont
import com.pankobyte.launcher.core.theme.LineIcons
import com.pankobyte.launcher.core.theme.NightColors
import com.pankobyte.launcher.core.theme.ParentColors
import com.pankobyte.launcher.core.ui.FlowHeader
import com.pankobyte.launcher.core.ui.MessagePill
import com.pankobyte.launcher.core.ui.ParentBody
import com.pankobyte.launcher.core.ui.ParentScreen
import com.pankobyte.launcher.core.ui.ParentTitle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The lower part of every PIN screen: [status] (the dots, or the lock card) centred in the free
 * space, with the number pad at the bottom.
 */
@Composable
private fun ColumnScope.PinArea(
    state: PinEntryState,
    night: Boolean,
    enabled: Boolean,
    onComplete: (String) -> Unit,
    status: @Composable () -> Unit,
) {
    Spacer(Modifier.weight(1f))
    Box(Modifier.fillMaxWidth().heightIn(min = 96.dp), contentAlignment = Alignment.Center) { status() }
    Spacer(Modifier.weight(1f))
    PinPad(state, night = night, enabled = enabled, onComplete = onComplete)
}

/** The dots, with room under them for an [error] on a pill. */
@Composable
private fun DotsWithNote(state: PinEntryState, night: Boolean, error: String?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        PinDots(state, night)
        Spacer(Modifier.height(14.dp))
        Box(Modifier.heightIn(min = 36.dp), contentAlignment = Alignment.Center) {
            if (error != null) {
                MessagePill(
                    error,
                    background = if (night) NightColors.errorBg else ParentColors.errorBg,
                    color = if (night) NightColors.error else ParentColors.error,
                )
            }
        }
    }
}

/**
 * Choosing a PIN in the parent look: enter four digits, then confirm them. With [step] it is the
 * last step of first-run setup. With [change] it follows the current-PIN check from Settings.
 */
@Composable
fun PinCreateScreen(vm: LauncherViewModel, change: Boolean, onCancel: () -> Unit, step: Int? = null) {
    val state = remember { PinEntryState() }
    val scope = rememberCoroutineScope()
    var first by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val confirming = first != null

    ParentScreen {
        FlowHeader(
            onBack = onCancel,
            step = step,
            totalSteps = Onboard.COUNT,
            title = if (change) stringResource(R.string.pin_change_header) else null,
        )
        ParentTitle(
            stringResource(
                when {
                    confirming -> R.string.pin_confirm_title
                    change -> R.string.pin_new_title
                    else -> R.string.pin_create_title
                },
            ),
        )
        // Only the very first PIN needs a reason. After that the title says it all.
        if (!change && !confirming) {
            Spacer(Modifier.height(6.dp))
            ParentBody(stringResource(R.string.pin_create_body))
        }
        PinArea(
            state = state,
            night = false,
            enabled = !saving,
            onComplete = { pin ->
                val earlier = first
                when {
                    earlier == null -> {
                        first = pin
                        state.clear()
                    }
                    earlier == pin -> {
                        saving = true
                        vm.createPin(pin)
                    }
                    else -> {
                        first = null
                        scope.launch { state.wrong() }
                    }
                }
            },
        ) {
            DotsWithNote(
                state,
                night = false,
                error = if (state.error) stringResource(R.string.pin_mismatch) else null,
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** Checking a typed PIN against the saved one, shared by both PIN-check screens. */
private class PinCheck(val state: PinEntryState, val lockedMs: Long, val busy: Boolean, val submit: (String) -> Unit) {
    val locked get() = lockedMs > 0
}

@Composable
private fun rememberPinCheck(vm: LauncherViewModel, onSuccess: () -> Unit): PinCheck {
    val state = remember { PinEntryState() }
    val scope = rememberCoroutineScope()
    val lock by vm.pinLock.collectAsStateWithLifecycle()
    var checking by remember { mutableStateOf(false) }
    val now by produceState(System.currentTimeMillis(), lock.lockedUntil) {
        while (System.currentTimeMillis() < lock.lockedUntil) {
            value = System.currentTimeMillis()
            delay(250)
        }
        value = System.currentTimeMillis()
    }
    return PinCheck(state, lockedMs = lock.lockedUntil - now, busy = checking) { pin ->
        checking = true
        scope.launch {
            when (vm.checkPin(pin)) {
                PinResult.Ok -> onSuccess()
                PinResult.Wrong -> state.wrong()
                PinResult.Locked -> state.clear()
            }
            checking = false
        }
    }
}

/** "Enter your current PIN", in the parent look. The first half of Change PIN in Settings. */
@Composable
fun CurrentPinScreen(vm: LauncherViewModel, onBack: () -> Unit, onSuccess: () -> Unit) {
    val check = rememberPinCheck(vm, onSuccess)
    ParentScreen {
        FlowHeader(onBack = onBack, title = stringResource(R.string.pin_change_header))
        ParentTitle(stringResource(R.string.pin_current_title))
        PinArea(check.state, night = false, enabled = !check.locked && !check.busy, onComplete = check.submit) {
            if (check.locked) {
                PinLockCard(check.lockedMs, night = false)
            } else {
                DotsWithNote(
                    check.state,
                    night = false,
                    error = if (check.state.error) stringResource(R.string.pin_wrong) else null,
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** "Grown-ups only", in the night look. The only way out of Kid mode, with an Emergency button. */
@Composable
fun KidPinScreen(vm: LauncherViewModel, onBack: () -> Unit, onSuccess: () -> Unit) {
    val check = rememberPinCheck(vm, onSuccess)
    Box(Modifier.fillMaxSize().background(NightColors.bg)) {
        Column(
            Modifier.align(Alignment.TopCenter).systemBarsPadding().widthIn(max = Dimens.contentMaxWidth).fillMaxSize().padding(horizontal = Dimens.kidMargin),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Gives way first on a short screen, so the number pad and the way out always fit.
            Spacer(Modifier.weight(0.6f, fill = false).height(36.dp))
            Box(Modifier.size(52.dp).clip(CircleShape).background(NightColors.raised), contentAlignment = Alignment.Center) {
                Icon(LineIcons.Lock, null, tint = NightColors.text, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.pin_title),
                color = NightColors.text,
                fontFamily = DisplayFont,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
            )
            PinArea(check.state, night = true, enabled = !check.locked && !check.busy, onComplete = check.submit) {
                if (check.locked) {
                    PinLockCard(check.lockedMs, night = true)
                } else {
                    DotsWithNote(
                        check.state,
                        night = true,
                        error = if (check.state.error) stringResource(R.string.pin_wrong) else null,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier
                        .height(Dimens.kidPillHeight)
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClick = onBack)
                        .padding(end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(LineIcons.Back, null, tint = NightColors.text, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.pin_back), color = NightColors.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.weight(1f))
                Row(
                    Modifier
                        .height(Dimens.kidPillHeight)
                        .clip(CircleShape)
                        .border(1.5.dp, NightColors.error, CircleShape)
                        .clickable(role = Role.Button) { vm.emergencyCall() }
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(LineIcons.Phone, null, tint = NightColors.error, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.pin_emergency), color = NightColors.error, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
