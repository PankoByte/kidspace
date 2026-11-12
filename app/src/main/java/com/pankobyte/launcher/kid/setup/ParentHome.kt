package com.pankobyte.launcher.kid.setup

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pankobyte.launcher.LauncherViewModel
import com.pankobyte.launcher.Onboard
import com.pankobyte.launcher.R
import com.pankobyte.launcher.Route
import com.pankobyte.launcher.core.data.LauncherSettings
import com.pankobyte.launcher.core.perms.GuardChecks
import com.pankobyte.launcher.core.perms.Perms
import com.pankobyte.launcher.core.theme.Dimens
import com.pankobyte.launcher.core.theme.DisplayFont
import com.pankobyte.launcher.core.theme.LineIcons
import com.pankobyte.launcher.core.theme.ParentColors
import com.pankobyte.launcher.core.ui.CapsLabel
import com.pankobyte.launcher.core.ui.FlowHeader
import com.pankobyte.launcher.core.ui.IconBadge
import com.pankobyte.launcher.core.ui.ParentBody
import com.pankobyte.launcher.core.ui.ParentCard
import com.pankobyte.launcher.core.ui.ParentDivider
import com.pankobyte.launcher.core.ui.ParentScreen
import com.pankobyte.launcher.core.ui.ParentSwitch
import com.pankobyte.launcher.core.ui.ParentTitle
import com.pankobyte.launcher.core.ui.PrimaryButton
import com.pankobyte.launcher.core.ui.StatusChip
import com.pankobyte.launcher.core.ui.rememberSettingsLauncher

/** Play times offered, in minutes. "No limit" is stored as -1. */
private val timeChoices = listOf(15, 30, 45, NO_LIMIT_MIN)
private const val NO_LIMIT_MIN = -1

/** Limits of the custom play time, and how far one tap on minus or plus moves it. */
private const val CUSTOM_MIN = 5
private const val CUSTOM_MAX = 240
private const val CUSTOM_STEP = 5

/** Short play time for the choice cards: "30 min", "1 hour", "1.5 hours". */
@Composable
fun timeLabel(minutes: Int): String = when {
    minutes < 0 -> stringResource(R.string.time_none)
    minutes < 60 -> stringResource(R.string.time_minutes, minutes)
    minutes % 60 == 0 -> pluralStringResource(R.plurals.time_hours, minutes / 60, minutes / 60)
    minutes == 90 -> stringResource(R.string.time_hour_and_half)
    else -> stringResource(R.string.time_hours_minutes, minutes / 60, minutes % 60)
}

/** The same with minutes spelled out, for the parent home: "30 minutes". */
@Composable
private fun timeLabelLong(minutes: Int): String =
    if (minutes in 0 until 60) pluralStringResource(R.plurals.time_minutes_long, minutes, minutes) else timeLabel(minutes)

/**
 * The parent's main screen once setup is done: the allowed apps, the play time, and Start.
 * Start asks Android to make us the home app first. Settings sits behind the top-right button.
 */
@Composable
fun HomeScreen(vm: LauncherViewModel) {
    val context = LocalContext.current
    val apps by vm.apps.collectAsStateWithLifecycle()
    val allowed by vm.allowed.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val openSettings = rememberSettingsLauncher()
    val checks = remember(vm.resumeTick) { GuardChecks.read(context) }
    val guardOn = checks.accessibility
    val allowedApps = remember(apps, allowed) {
        apps.filter { it.packageName in allowed.orEmpty() }.distinctBy { it.packageName }
    }
    val canStart = guardOn && allowedApps.isNotEmpty() && profile != null
    var explainHome by rememberSaveable { mutableStateOf(false) }
    var explainDnd by rememberSaveable { mutableStateOf(false) }
    val askHome = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        vm.finishStart(accepted = result.resultCode == Activity.RESULT_OK)
    }
    fun start() {
        val intent = vm.beginStart() ?: return
        runCatching { askHome.launch(intent) }.onFailure { vm.finishStart() }
    }

    ParentScreen {
        Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            SpaceMark()
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.app_name), color = ParentColors.ink, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            Box(
                Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button) { vm.route = Route.Settings },
                contentAlignment = Alignment.Center,
            ) {
                Icon(LineIcons.Sliders, stringResource(R.string.settings_title), tint = ParentColors.ink, modifier = Modifier.size(22.dp))
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(16.dp))
            if (!guardOn) {
                GuardOffBanner { vm.route = Route.Guard }
                Spacer(Modifier.height(16.dp))
            }
            if (allowedApps.isEmpty()) {
                NoAppsCard { vm.route = Route.Apps }
            } else {
                SpacePreview(allowedApps) { vm.route = Route.Apps }
            }
            Spacer(Modifier.height(12.dp))
            ParentCard(onClick = { vm.route = Route.Time }, padding = 18.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(LineIcons.Clock, size = 42.dp, background = ParentColors.blueSoft)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.home_time_label), color = ParentColors.muted, fontSize = 14.sp)
                        Spacer(Modifier.height(3.dp))
                        Text(profile?.let { timeLabelLong(it.timeLimitMin) }.orEmpty(), color = ParentColors.ink, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    }
                    Icon(LineIcons.Chevron, null, tint = ParentColors.muted, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            ParentCard(padding = 0.dp) {
                SettingRow(LineIcons.BellOff, stringResource(R.string.settings_dnd), stringResource(R.string.home_dnd_body)) {
                    ParentSwitch(
                        checked = settings?.useDnd == true && checks.dndAccess,
                        onCheckedChange = { on ->
                            vm.setUseDnd(on)
                            // Android answers with a list of every app, so show the way first.
                            if (on && !checks.dndAccess) explainDnd = true
                        },
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
        }
        if (vm.homeRefused && canStart) {
            ParentCard(background = ParentColors.warnBg, border = ParentColors.warnBorder, padding = 14.dp) {
                Text(stringResource(R.string.home_refused_title), color = ParentColors.ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
                ParentBody(stringResource(R.string.home_refused_body))
            }
            Spacer(Modifier.height(12.dp))
        }
        PrimaryButton(stringResource(R.string.home_start), {
            if (Perms.isDefaultHome(context)) start() else explainHome = true
        }, enabled = canStart, height = 58.dp) { enabled ->
            Icon(LineIcons.Play, null, tint = if (enabled) ParentColors.onInk else ParentColors.disabledText, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(16.dp))
    }
    if (explainDnd) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { explainDnd = false },
            containerColor = ParentColors.surface,
            title = {
                Text(stringResource(R.string.dnd_guide_title), color = ParentColors.ink, fontSize = 21.sp, lineHeight = 27.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                // Why first, then how. It scrolls, so the buttons stay reachable on a short screen.
                Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                    Text(stringResource(R.string.dnd_guide_why), color = ParentColors.ink, fontSize = 15.sp, lineHeight = 21.sp)
                    Spacer(Modifier.height(16.dp))
                    DndGuide()
                }
            },
            confirmButton = {
                PrimaryButton(stringResource(R.string.dnd_guide_open), { explainDnd = false; openSettings(Perms.dndIntent()) })
            },
            dismissButton = {
                com.pankobyte.launcher.core.ui.TextLink(stringResource(R.string.handoff_cancel), { explainDnd = false })
            },
        )
    }
    if (explainHome) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { explainHome = false },
            containerColor = ParentColors.surface,
            title = {
                Text(stringResource(R.string.handoff_title), color = ParentColors.ink, fontSize = 21.sp, lineHeight = 27.sp, fontWeight = FontWeight.Bold)
            },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) { HomeChoiceGuide() } },
            confirmButton = {
                PrimaryButton(stringResource(R.string.handoff_confirm), { explainHome = false; start() })
            },
            dismissButton = {
                com.pankobyte.launcher.core.ui.TextLink(stringResource(R.string.handoff_cancel), { explainHome = false })
            },
        )
    }
}

/** Yellow notice at the top of the home screen while the Kid mode guard is off. */
@Composable
private fun GuardOffBanner(onFix: () -> Unit) {
    ParentCard(background = ParentColors.warnBg, border = ParentColors.warnBorder, borderWidth = 1.5.dp, padding = 16.dp) {
        Row(verticalAlignment = Alignment.Top) {
            IconBadge(LineIcons.Warning, size = 40.dp, background = ParentColors.warnInk, tint = ParentColors.warnBg)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    stringResource(R.string.home_guard_off),
                    color = ParentColors.ink,
                    fontFamily = DisplayFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
                Spacer(Modifier.height(2.dp))
                Text(stringResource(R.string.home_guard_off_body), color = ParentColors.ink, fontSize = 14.sp, lineHeight = 19.sp)
            }
        }
        Spacer(Modifier.height(14.dp))
        PrimaryButton(stringResource(R.string.home_guard_fix), onFix, height = 48.dp)
    }
}

@Composable
private fun CardTopLine(label: String, action: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), color = ParentColors.muted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Text(action, color = ParentColors.ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(4.dp))
        Icon(LineIcons.Chevron, null, tint = ParentColors.ink, modifier = Modifier.size(14.dp))
    }
}

/** The Allowed apps card when nothing is chosen: a dashed red outline asking for a choice. */
@Composable
private fun NoAppsCard(onClick: () -> Unit) {
    val shape = RoundedCornerShape(Dimens.parentCardRadius)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(ParentColors.emptyBg)
            .drawBehind {
                val stroke = 1.5.dp.toPx()
                drawRoundRect(
                    ParentColors.errorRing,
                    topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2),
                    size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(Dimens.parentCardRadius.toPx()),
                    style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
                )
            }
            .clickable(role = Role.Button, onClick = onClick)
            .padding(18.dp),
    ) {
        CardTopLine(stringResource(R.string.home_apps_label), stringResource(R.string.home_choose))
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.home_apps_none),
            color = ParentColors.ink,
            fontFamily = DisplayFont,
            fontWeight = FontWeight.Bold,
            fontSize = 27.sp,
            lineHeight = 30.sp,
        )
        Spacer(Modifier.height(2.dp))
        Text(stringResource(R.string.home_apps_none_body), color = ParentColors.error, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

/** The parent picks how long one Kid mode session lasts. */
@Composable
fun TimeScreen(vm: LauncherViewModel, step: Int?, onBack: () -> Unit, onDone: () -> Unit) {
    val profile by vm.profile.collectAsStateWithLifecycle()
    val current = profile?.timeLimitMin
    // Custom stays picked even when minus and plus land on one of the preset times.
    // Null until the parent taps something: until then it follows the saved time.
    var customPicked by rememberSaveable { mutableStateOf<Boolean?>(null) }
    val custom = customPicked ?: (current != null && current != NO_LIMIT_MIN && current !in timeChoices)
    BackHandler(onBack = onBack)

    ParentScreen {
        FlowHeader(onBack = onBack, step = step, totalSteps = Onboard.COUNT)
        ParentTitle(stringResource(R.string.time_title))
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).selectableGroup().padding(top = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            for (pair in timeChoices.chunked(2)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (minutes in pair) {
                        TimeOption(timeLabel(minutes), !custom && current == minutes, Modifier.weight(1f), onClick = {
                            customPicked = false
                            vm.setTimeLimit(minutes)
                        })
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            val customMinutes = current?.takeIf { it > 0 }?.coerceIn(CUSTOM_MIN, CUSTOM_MAX) ?: 30
            ParentCard(
                border = if (custom) ParentColors.accent else ParentColors.border,
                background = if (custom) ParentColors.blueSoft else ParentColors.surface,
                padding = 20.dp,
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.time_custom), Modifier.weight(1f), color = ParentColors.ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    if (custom) Icon(LineIcons.Check, null, tint = ParentColors.accent, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StepButton(LineIcons.Minus, stringResource(R.string.time_less), current != null && customMinutes > CUSTOM_MIN) {
                        customPicked = true
                        vm.setTimeLimit((customMinutes - CUSTOM_STEP).coerceAtLeast(CUSTOM_MIN))
                    }
                    Box(
                        Modifier.weight(1f).heightIn(min = 56.dp).clip(RoundedCornerShape(12.dp))
                            .selectable(selected = custom, role = Role.RadioButton, onClick = {
                                customPicked = true
                                vm.setTimeLimit(customMinutes)
                            }),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(stringResource(R.string.time_minutes, customMinutes), color = ParentColors.ink,
                            fontWeight = FontWeight.Bold, fontSize = 30.sp, textAlign = TextAlign.Center)
                    }
                    StepButton(LineIcons.Plus, stringResource(R.string.time_more), current != null && customMinutes < CUSTOM_MAX) {
                        customPicked = true
                        vm.setTimeLimit((customMinutes + CUSTOM_STEP).coerceAtMost(CUSTOM_MAX))
                    }
                }
            }
        }
        PrimaryButton(stringResource(if (step != null) R.string.time_continue else R.string.action_done), onDone, enabled = current != null)
        Spacer(Modifier.height(16.dp))
    }
}

/** Equal-sized presets, including No limit. */
@Composable
private fun TimeOption(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    val content by animateColorAsState(if (selected) ParentColors.onInk else ParentColors.ink, tween(160), label = "timeText")
    val fill by animateColorAsState(if (selected) ParentColors.ink else ParentColors.surface, tween(160), label = "timeFill")
    Column(
        modifier
            .heightIn(min = 86.dp)
            .clip(shape)
            .background(fill)
            .then(if (selected) Modifier else Modifier.border(1.dp, ParentColors.border, shape))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, color = content, fontFamily = DisplayFont, fontWeight = FontWeight.Bold, fontSize = 21.sp)
            }
            if (selected) Icon(LineIcons.Check, null, tint = content, modifier = Modifier.size(20.dp))
        }
    }
}

/** Round minus or plus button on the selected Custom card. */
@Composable
private fun StepButton(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .alpha(if (enabled) 1f else 0.35f)
            .clip(CircleShape)
            .background(ParentColors.sageSoft)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, description, tint = ParentColors.ink, modifier = Modifier.size(22.dp))
    }
}

/**
 * Settings: the optional backup guard with its switch, the state of app protection, and
 * Change PIN. Silencing notifications, the other extra, sits on the home screen. An extra
 * counts as on only when its switch is on and Android has granted its
 * permission, so switching one on without the permission opens the matching settings page.
 */
@Composable
fun SettingsScreen(vm: LauncherViewModel, settings: LauncherSettings) {
    val context = LocalContext.current
    val openSettings = rememberSettingsLauncher()
    val checks = remember(vm.resumeTick) { GuardChecks.read(context) }
    val back = { vm.route = Route.Home }
    BackHandler(onBack = back)

    ParentScreen {
        FlowHeader(onBack = back, title = stringResource(R.string.settings_title), serifTitle = true)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            SectionLabel(stringResource(R.string.settings_extras), first = true)
            ParentCard(padding = 0.dp) {
                SettingRow(LineIcons.Refresh, stringResource(R.string.settings_backup), stringResource(R.string.settings_backup_body)) {
                    ParentSwitch(
                        checked = settings.useBackupGuard && checks.usageAccess,
                        onCheckedChange = { on ->
                            vm.setUseBackupGuard(on)
                            if (on && !checks.usageAccess) openSettings(Perms.usageAccessIntent())
                        },
                    )
                }
            }

            SectionLabel(stringResource(R.string.settings_protection))
            val guardOn = checks.accessibility
            ParentCard(
                onClick = { vm.route = Route.Guard },
                background = if (guardOn) ParentColors.surface else ParentColors.warnBgSoft,
                border = if (guardOn) ParentColors.border else ParentColors.warnBorder,
                borderWidth = if (guardOn) 1.dp else 1.5.dp,
                padding = 0.dp,
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(
                        if (guardOn) LineIcons.Shield else LineIcons.ShieldAlert,
                        size = 36.dp,
                        background = if (guardOn) ParentColors.sageSoft else ParentColors.warnBg,
                        tint = if (guardOn) ParentColors.ink else ParentColors.warnInk,
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_guard), color = ParentColors.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        StatusChip(
                            stringResource(if (guardOn) R.string.settings_guard_on else R.string.settings_guard_off),
                            background = if (guardOn) ParentColors.greenBg else ParentColors.warnBg,
                            dot = if (guardOn) ParentColors.green else ParentColors.warnInk,
                        )
                    }
                    Icon(LineIcons.Chevron, null, tint = ParentColors.ink, modifier = Modifier.size(16.dp))
                }
            }

            SectionLabel(stringResource(R.string.settings_pin))
            ParentCard(onClick = { vm.route = Route.PinGate }, padding = 0.dp) {
                SettingRow(LineIcons.Key, stringResource(R.string.settings_change_pin), null) {
                    Icon(LineIcons.Chevron, null, tint = ParentColors.ink, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            val privacyUrl = stringResource(R.string.privacy_url)
            ParentCard(onClick = { openSettings(Intent(Intent.ACTION_VIEW, privacyUrl.toUri())) }, padding = 0.dp) {
                SettingRow(LineIcons.Eye, stringResource(R.string.settings_privacy), null) {
                    Icon(LineIcons.Chevron, null, tint = ParentColors.ink, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String, first: Boolean = false) {
    CapsLabel(text, Modifier.padding(top = if (first) 8.dp else 26.dp, bottom = 8.dp))
}

/** One line in a settings card: an icon, a title, an optional sentence under it, and a control. */
@Composable
private fun SettingRow(icon: ImageVector, title: String, body: String?, trailing: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        IconBadge(icon, size = 36.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = ParentColors.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            if (body != null) {
                Spacer(Modifier.height(2.dp))
                Text(body, color = ParentColors.muted, fontSize = 13.5.sp, lineHeight = 19.sp)
            }
        }
        Spacer(Modifier.width(12.dp))
        trailing()
    }
}
