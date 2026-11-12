package com.pankobyte.launcher.core.apps

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KidHintTest {
    @Test
    fun gamesAreSuggested() {
        assertTrue(looksMadeForKids("Chess", "com.example.chess", isGame = true))
    }

    @Test
    fun kidsInNameOrPackageIsSuggested() {
        assertTrue(looksMadeForKids("YouTube Kids", "com.google.android.apps.youtube.kids", isGame = false))
        assertTrue(looksMadeForKids("Learn ABC", "com.example.toddler.abc", isGame = false))
    }

    @Test
    fun ordinaryAppsAreNotSuggested() {
        assertFalse(looksMadeForKids("Chrome", "com.android.chrome", isGame = false))
        assertFalse(looksMadeForKids("Bank", "com.example.bank", isGame = false))
    }
}
