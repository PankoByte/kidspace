package com.pankobyte.launcher.core.apps

import android.content.pm.ApplicationInfo

private val KID_WORDS = listOf("kid", "child", "toddler", "junior", "baby", "preschool", "nursery")

/**
 * A rough guess that an app is meant for children, used only to sort the app picker.
 * Android gives installed apps no "made for kids" flag and Google Play has no public lookup
 * for one, so this goes by what the phone does know: the app says it is a game, or its name or
 * package mentions children. The parent still approves every app.
 */
fun looksMadeForKids(label: String, packageName: String, isGame: Boolean): Boolean {
    if (isGame) return true
    val name = label.lowercase()
    val pkg = packageName.lowercase()
    return KID_WORDS.any { it in name || it in pkg }
}

fun AppEntry.looksMadeForKids(): Boolean =
    looksMadeForKids(label, packageName, isGame = category == ApplicationInfo.CATEGORY_GAME)
