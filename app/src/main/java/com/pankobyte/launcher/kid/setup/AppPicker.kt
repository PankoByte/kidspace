package com.pankobyte.launcher.kid.setup

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pankobyte.launcher.LauncherViewModel
import com.pankobyte.launcher.Onboard
import com.pankobyte.launcher.R
import com.pankobyte.launcher.core.apps.AppEntry
import com.pankobyte.launcher.core.apps.AppIcon
import com.pankobyte.launcher.core.apps.looksMadeForKids
import com.pankobyte.launcher.core.theme.BodyFont
import com.pankobyte.launcher.core.theme.DisplayFont
import com.pankobyte.launcher.core.theme.LineIcons
import com.pankobyte.launcher.core.theme.ParentColors
import com.pankobyte.launcher.core.ui.CapsLabel
import com.pankobyte.launcher.core.ui.CheckCircle
import com.pankobyte.launcher.core.ui.FlowHeader
import com.pankobyte.launcher.core.ui.IconBadge
import com.pankobyte.launcher.core.ui.OutlineButton
import com.pankobyte.launcher.core.ui.ParentDivider
import com.pankobyte.launcher.core.ui.ParentScreen
import com.pankobyte.launcher.core.ui.ParentTitle
import com.pankobyte.launcher.core.ui.PrimaryButton

/**
 * The parent ticks the apps a child may open. Apps that look made for children are listed first
 * under "Suggested", the rest under "Other apps", and a search box narrows both. With [step] set
 * it is part of first-run setup and needs at least one app before it moves on.
 */
@Composable
fun AppPickerScreen(vm: LauncherViewModel, step: Int?, onBack: () -> Unit, onDone: () -> Unit) {
    val saved by vm.allowed.collectAsStateWithLifecycle()
    val initial = saved
    if (initial == null) {
        // The saved list is still loading. It takes a moment at most.
        ParentScreen { FlowHeader(onBack = onBack, step = step, totalSteps = Onboard.COUNT) }
        return
    }
    val apps by vm.apps.collectAsStateWithLifecycle()
    var selected by rememberSaveable { mutableStateOf(initial.toList()) }
    var query by rememberSaveable { mutableStateOf("") }

    val choices = remember(apps) { apps.distinctBy { it.packageName } }
    val matches = remember(choices, query) {
        val q = query.trim()
        if (q.isEmpty()) choices else choices.filter { it.label.contains(q, ignoreCase = true) }
    }
    val suggested = remember(matches) { matches.filter { it.looksMadeForKids() } }
    val others = remember(matches) { matches.filterNot { it.looksMadeForKids() } }
    val count = remember(selected, choices) { choices.count { it.packageName in selected } }

    fun toggle(entry: AppEntry) {
        selected = if (entry.packageName in selected) selected - entry.packageName else selected + entry.packageName
    }
    fun leave(then: () -> Unit) {
        vm.setAllowedApps(selected.toSet())
        then()
    }
    BackHandler { leave(onBack) }

    ParentScreen {
        FlowHeader(onBack = { leave(onBack) }, step = step, totalSteps = Onboard.COUNT)
        ParentTitle(stringResource(R.string.apps_title))
        Spacer(Modifier.height(20.dp))
        SearchField(query, onChange = { query = it })
        Spacer(Modifier.height(4.dp))
        if (matches.isEmpty()) {
            NoMatches(query.trim(), Modifier.weight(1f)) { query = "" }
        } else {
            LazyColumn(Modifier.weight(1f)) {
                if (suggested.isNotEmpty()) {
                    item(key = "#suggested") {
                        SectionHeader(stringResource(R.string.apps_suggested), null)
                    }
                    items(suggested, key = { it.packageName }) { entry ->
                        AppRow(entry, checked = entry.packageName in selected) { toggle(entry) }
                    }
                }
                if (others.isNotEmpty()) {
                    item(key = "#others") {
                        SectionHeader(stringResource(if (suggested.isEmpty()) R.string.apps_all else R.string.apps_others), null)
                    }
                    items(others, key = { it.packageName }) { entry ->
                        AppRow(entry, checked = entry.packageName in selected) { toggle(entry) }
                    }
                }
            }
        }
        ParentDivider()
        Row(Modifier.heightIn(min = 46.dp).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (count == 0) {
                Text(
                    stringResource(R.string.apps_none_selected),
                    color = ParentColors.muted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                Box(
                    // At least as wide as it is tall, so a one-digit count sits in a circle.
                    Modifier.height(26.dp).defaultMinSize(minWidth = 26.dp).clip(CircleShape).background(ParentColors.ink).padding(horizontal = 7.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(count.toString(), color = ParentColors.onInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    pluralStringResource(R.plurals.apps_chosen, count),
                    color = ParentColors.ink,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        PrimaryButton(
            stringResource(if (step != null) R.string.apps_continue else R.string.action_done),
            onClick = { leave(onDone) },
            enabled = step == null || count > 0,
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit) {
    val searchLabel = stringResource(R.string.apps_search)
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .background(ParentColors.surface)
            .border(if (focused) 2.dp else 1.dp, if (focused) ParentColors.ink else ParentColors.border, shape)
            .padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(LineIcons.Search, null, tint = ParentColors.ink, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) Text(stringResource(R.string.apps_search), color = ParentColors.muted, fontSize = 16.sp)
            BasicTextField(
                value = value,
                onValueChange = onChange,
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = searchLabel },
                singleLine = true,
                textStyle = TextStyle(color = ParentColors.ink, fontSize = 16.sp, fontFamily = BodyFont),
                cursorBrush = SolidColor(ParentColors.ink),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                interactionSource = source,
            )
        }
        if (value.isNotEmpty()) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).clickable(role = Role.Button) { onChange("") },
                contentAlignment = Alignment.Center,
            ) {
                Icon(LineIcons.Close, stringResource(R.string.apps_search_clear), tint = ParentColors.ink, modifier = Modifier.size(16.dp))
            }
        }
    }
}

/** Shown in place of the list when the search finds nothing. */
@Composable
private fun NoMatches(query: String, modifier: Modifier = Modifier, onClear: () -> Unit) {
    Column(modifier.fillMaxWidth().padding(top = 44.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        IconBadge(LineIcons.Search, size = 60.dp, background = ParentColors.sage, tint = ParentColors.muted)
        Spacer(Modifier.height(20.dp))
        Text(
            stringResource(R.string.apps_no_match, query),
            color = ParentColors.ink,
            fontFamily = DisplayFont,
            fontWeight = FontWeight.Bold,
            fontSize = 21.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(18.dp))
        OutlineButton(stringResource(R.string.apps_search_clear), onClear)
    }
}

@Composable
private fun SectionHeader(title: String, note: String?) {
    Column(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 6.dp)) {
        CapsLabel(title)
        if (note != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                note,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ParentColors.warnBg)
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                color = ParentColors.warnInk,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
        }
    }
}

@Composable
private fun AppRow(entry: AppEntry, checked: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (checked) ParentColors.blueSoft else androidx.compose.ui.graphics.Color.Transparent)
            .toggleable(checked, role = Role.Checkbox, onValueChange = { onToggle() })
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(entry, Modifier.size(40.dp).clip(CircleShape))
        Spacer(Modifier.width(14.dp))
        Text(
            entry.label,
            modifier = Modifier.weight(1f),
            color = ParentColors.ink,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(12.dp))
        CheckCircle(checked)
        Spacer(Modifier.width(2.dp))
    }
}
