package com.pankobyte.launcher.kid.setup

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pankobyte.launcher.LauncherViewModel
import com.pankobyte.launcher.Onboard
import com.pankobyte.launcher.R
import com.pankobyte.launcher.core.perms.GuardChecks
import com.pankobyte.launcher.core.perms.Perms
import com.pankobyte.launcher.core.theme.DisplayFont
import com.pankobyte.launcher.core.theme.LineIcons
import com.pankobyte.launcher.core.theme.ParentColors
import com.pankobyte.launcher.core.ui.FlowHeader
import com.pankobyte.launcher.core.ui.IconBadge
import com.pankobyte.launcher.core.ui.ParentBody
import com.pankobyte.launcher.core.ui.ParentCard
import com.pankobyte.launcher.core.ui.ParentScreen
import com.pankobyte.launcher.core.ui.ParentTitle
import com.pankobyte.launcher.core.ui.PrimaryButton
import com.pankobyte.launcher.core.ui.rememberSettingsLauncher
import com.pankobyte.launcher.kid.pin.PinCreateScreen

/**
 * First-run setup: demonstrate the space, choose apps and time, enable protection, then set the PIN.
 * Creating the PIN ends it. Back steps to the screen before, and closes the app on the first one.
 * Steps slide in from the side they come from, so forward and back feel different.
 */
@Composable
fun OnboardingFlow(vm: LauncherViewModel, step: Int, onFinish: () -> Unit) {
    val context = LocalContext.current
    // Once app protection is on, its step has nothing left to do and is passed over both ways.
    fun guardOn() = GuardChecks.read(context).accessibility
    val back: () -> Unit = {
        when {
            step == Onboard.WELCOME -> onFinish()
            step == Onboard.PIN && guardOn() -> vm.setOnboardingStep(Onboard.TIME)
            else -> vm.setOnboardingStep(Onboard.previous(step))
        }
    }
    BackHandler(onBack = back)
    AnimatedContent(
        targetState = step,
        transitionSpec = {
            val direction = if (Onboard.position(targetState) > Onboard.position(initialState)) 1 else -1
            (slideInHorizontally(tween(260)) { direction * it / 5 } + fadeIn(tween(260))) togetherWith
                (slideOutHorizontally(tween(160)) { -direction * it / 5 } + fadeOut(tween(160)))
        },
        label = "setupStep",
    ) { shown ->
        when (shown) {
            Onboard.WELCOME -> WelcomeScreen(onNext = { vm.setOnboardingStep(Onboard.APPS) })
            Onboard.GUARD -> GuardScreen(vm, step = shown, onBack = back, onDone = { vm.setOnboardingStep(Onboard.PIN) })
            Onboard.APPS -> AppPickerScreen(vm, step = shown, onBack = back, onDone = { vm.setOnboardingStep(Onboard.TIME) })
            Onboard.TIME -> TimeScreen(vm, step = shown, onBack = back, onDone = { vm.setOnboardingStep(if (guardOn()) Onboard.PIN else Onboard.GUARD) })
            else -> PinCreateScreen(vm, change = false, onCancel = back, step = Onboard.PIN)
        }
    }
}

/** One short promise and a looping illustration of the app boundary. */
@Composable
private fun WelcomeScreen(onNext: () -> Unit) {
    ParentScreen {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                SpaceMark()
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.app_name), color = ParentColors.ink, fontWeight = FontWeight.Bold, fontSize = 21.sp)
            }
            Spacer(Modifier.height(30.dp))
            ParentTitle(stringResource(R.string.welcome_title), large = true)
            Spacer(Modifier.height(26.dp))
            WelcomeIllustration()
            Spacer(Modifier.height(30.dp))
        }
        PrimaryButton(stringResource(R.string.welcome_start), onNext)
        Spacer(Modifier.height(16.dp))
    }
}

/**
 * The one permission Kid mode cannot run without: the Accessibility service that keeps a child
 * inside the approved apps. A visual guide and compact disclosure precede the
 * parent's agreement before Android's settings open. With [step] set it is part of first-run
 * setup; without, it is opened from the home screen or Settings.
 */
@Composable
fun GuardScreen(vm: LauncherViewModel, step: Int?, onBack: () -> Unit, onDone: () -> Unit) {
    val context = LocalContext.current
    val openSettings = rememberSettingsLauncher()
    val on = remember(vm.resumeTick) { GuardChecks.read(context) }.accessibility
    // Switched on in Android's list but not running: it needs switching off and on again.
    val stuck = !on && remember(vm.resumeTick) { Perms.isAccessibilityOn(context) }

    // During setup, coming back from Android with protection on goes straight to the next step.
    val moveOn by rememberUpdatedState(onDone)
    LaunchedEffect(on) { if (on && step != null) moveOn() }

    ParentScreen {
        FlowHeader(onBack = onBack, step = step, totalSteps = Onboard.COUNT)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            ParentTitle(stringResource(R.string.guard_title))
            Spacer(Modifier.height(6.dp))
            ParentBody(stringResource(R.string.guard_body))
            Spacer(Modifier.height(18.dp))
            if (on) {
                ParentCard(background = ParentColors.greenBg, border = ParentColors.green, padding = 16.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(LineIcons.Check, background = ParentColors.green, tint = ParentColors.onInk)
                        Spacer(Modifier.width(14.dp))
                        Text(
                            stringResource(R.string.guard_on_title),
                            color = ParentColors.ink,
                            fontFamily = DisplayFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            if (!on) {
                ProtectionGuide(enabled = false)
            }
            if (stuck) {
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.guard_stuck),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(ParentColors.warnBg)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    color = ParentColors.ink,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
            }
            Spacer(Modifier.height(16.dp))
        }
        if (on) {
            PrimaryButton(stringResource(if (step != null) R.string.guard_continue else R.string.action_done), onDone)
            Spacer(Modifier.height(16.dp))
        } else {
            PrimaryButton(stringResource(R.string.guard_agree), {
                vm.agreeToDisclosure()
                openSettings(Perms.accessibilityIntent())
            })
            Spacer(Modifier.height(16.dp))
        }
    }
}

