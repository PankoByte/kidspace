package com.pankobyte.launcher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.pankobyte.launcher.core.data.LauncherSettings
import com.pankobyte.launcher.core.theme.KidColors
import com.pankobyte.launcher.core.theme.LauncherTheme
import com.pankobyte.launcher.guard.KidSessionService
import com.pankobyte.launcher.kid.home.KidRoot
import com.pankobyte.launcher.kid.pin.CurrentPinScreen
import com.pankobyte.launcher.kid.pin.PinCreateScreen
import com.pankobyte.launcher.kid.session.KidSessionState
import com.pankobyte.launcher.kid.setup.AppPickerScreen
import com.pankobyte.launcher.kid.setup.GuardScreen
import com.pankobyte.launcher.kid.setup.HomeScreen
import com.pankobyte.launcher.kid.setup.OnboardingFlow
import com.pankobyte.launcher.kid.setup.SettingsScreen
import com.pankobyte.launcher.kid.setup.TimeScreen
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * The only activity. Opened from the app icon it shows first-run setup or the parent home. While Kid mode runs it
 * is also the phone's home app (through the `KidHome` alias), so Home always lands on the kid home.
 */
class MainActivity : ComponentActivity() {
    private val vm: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // A phone on its side is too short for the number pad, the one way out of Kid mode, so
        // phones stay upright. Tablets and unfolded phones have the room and turn freely.
        if (resources.configuration.smallestScreenWidthDp < 600) {
            requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        setContent { LauncherRoot(vm, onFinish = ::finish) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Home pressed. In Kid mode this lands on the kid home, never on a parent screen.
        vm.onHomePressed()
    }

    override fun onResume() {
        super.onResume()
        vm.onResume()
        // After a restart or process death, bring the session service back.
        lifecycleScope.launch {
            val snapshot = AppContainer.kid.snapshot.filterNotNull().first()
            if (snapshot.state != KidSessionState.OFF && !KidSessionService.running) KidSessionService.start(this@MainActivity)
        }
    }
}

@Composable
private fun LauncherRoot(vm: LauncherViewModel, onFinish: () -> Unit) {
    val kid by vm.kid.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val snapshot = kid
    val current = settings
    // Only the night screens are dark: Time's up, and the PIN screen inside Kid mode.
    val night = snapshot != null && snapshot.state != KidSessionState.OFF &&
        (snapshot.state == KidSessionState.TIMES_UP || vm.kidRoute == KidRoute.Pin)
    SystemBarIcons(darkIcons = !night)

    LauncherTheme {
        when {
            // Until the saved Kid mode state is read, show a blank kid-coloured frame, never a parent screen.
            snapshot == null || current == null -> Box(Modifier.fillMaxSize().background(KidColors.sky))
            snapshot.state != KidSessionState.OFF -> {
                // Back on the kid home does nothing. Kid screens register their own handlers.
                BackHandler {}
                KidRoot(vm, snapshot)
            }
            else -> ParentRoot(vm, current, onFinish)
        }
    }
}

@Composable
private fun ParentRoot(vm: LauncherViewModel, settings: LauncherSettings, onFinish: () -> Unit) {
    // First run: the setup steps, ending with the PIN. Once a PIN exists, setup is finished.
    if (!settings.hasPin) {
        OnboardingFlow(vm, step = vm.onboardingStep ?: settings.onboardingStep, onFinish = onFinish)
        return
    }
    val home = { vm.route = Route.Home }
    // A quick cross-fade, so moving between screens does not feel like a jump.
    AnimatedContent(
        targetState = vm.route,
        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) },
        label = "parentRoute",
    ) { route ->
        when (route) {
            Route.Home -> HomeScreen(vm)
            Route.Apps -> AppPickerScreen(vm, step = null, onBack = home, onDone = home)
            Route.Time -> TimeScreen(vm, step = null, onBack = home, onDone = home)
            Route.Settings -> SettingsScreen(vm, settings)
            Route.Guard -> {
                BackHandler(onBack = home)
                GuardScreen(vm, step = null, onBack = home, onDone = home)
            }
            Route.PinGate -> {
                val back = { vm.route = Route.Settings }
                BackHandler(onBack = back)
                CurrentPinScreen(vm, onBack = back, onSuccess = { vm.route = Route.PinChange })
            }
            Route.PinChange -> {
                val back = { vm.route = Route.Settings }
                BackHandler(onBack = back)
                PinCreateScreen(vm, change = true, onCancel = back)
            }
        }
    }
}

/** Dark status and navigation bar icons on light screens, light icons on dark ones. */
@Composable
private fun SystemBarIcons(darkIcons: Boolean) {
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? ComponentActivity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = darkIcons
            isAppearanceLightNavigationBars = darkIcons
        }
    }
}
