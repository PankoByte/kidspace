package com.pankobyte.launcher.core.apps

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.os.UserManager
import android.util.LruCache
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** One launchable activity. [key] is stable across restarts and unique across user profiles. */
@Immutable
data class AppEntry(
    val component: ComponentName,
    val userSerial: Long,
    val label: String,
    /** Android's own app type, one of the ApplicationInfo.CATEGORY values. */
    val category: Int = -1,
) {
    val packageName: String get() = component.packageName
    val key: String = appKey(component.flattenToString(), userSerial)
}

fun appKey(flatComponent: String, userSerial: Long) = "$flatComponent#$userSerial"

/** Reads the installed apps through LauncherApps, keeps the list fresh, and caches icons. */
class AppRepository(private val context: Context, private val scope: CoroutineScope) {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)

    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps

    private val _loaded = MutableStateFlow(false)
    /** True once the first app list has been read. */
    val loaded: StateFlow<Boolean> = _loaded

    @Volatile private var infos: Map<String, LauncherActivityInfo> = emptyMap()
    private val icons = LruCache<String, ImageBitmap>(400)
    private val iconPx = (ICON_DP * context.resources.displayMetrics.density).toInt()

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) = changed(packageName)
        override fun onPackageAdded(packageName: String, user: UserHandle) = changed(packageName)
        override fun onPackageChanged(packageName: String, user: UserHandle) = changed(packageName)
        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = reload()
        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = reload()
    }

    init {
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        reload()
    }

    private fun changed(packageName: String) {
        // An updated app may ship a new icon, so drop that package's cached icons.
        icons.snapshot().keys.filter { it.startsWith("$packageName/") }.forEach { icons.remove(it) }
        reload()
    }

    fun reload() {
        scope.launch(Dispatchers.Default) {
            val map = HashMap<String, LauncherActivityInfo>()
            val list = ArrayList<AppEntry>()
            for (user in launcherApps.profiles) {
                val serial = userManager.getSerialNumberForUser(user)
                for (info in launcherApps.getActivityList(null, user)) {
                    if (info.componentName.packageName == context.packageName) continue
                    val entry = AppEntry(
                        component = info.componentName,
                        userSerial = serial,
                        label = info.label?.toString().orEmpty().ifBlank { info.componentName.packageName },
                        category = info.applicationInfo.category,
                    )
                    map[entry.key] = info
                    list += entry
                }
            }
            list.sortWith(compareBy({ it.label.lowercase() }, { it.key }))
            infos = map
            _apps.value = list
            _loaded.value = true
        }
    }

    fun cachedIcon(entry: AppEntry): ImageBitmap? = icons.get(entry.key)

    suspend fun icon(entry: AppEntry): ImageBitmap? {
        icons.get(entry.key)?.let { return it }
        val info = infos[entry.key] ?: return null
        return withContext(Dispatchers.IO) {
            runCatching { render(info.getIcon(0)).asImageBitmap() }.getOrNull()?.also { icons.put(entry.key, it) }
        }
    }

    /** Draws the icon as a full square so the screen can clip it to its own tile shape. */
    private fun render(drawable: Drawable): Bitmap {
        val bitmap = createBitmap(iconPx, iconPx)
        val canvas = Canvas(bitmap)
        if (drawable is AdaptiveIconDrawable) {
            // Adaptive layers are 108 dp with a 72 dp visible centre: draw them 1.5 times larger.
            val extra = iconPx / 4
            for (layer in listOf(drawable.background, drawable.foreground)) {
                layer ?: continue
                layer.setBounds(-extra, -extra, iconPx + extra, iconPx + extra)
                layer.draw(canvas)
            }
        } else {
            drawable.setBounds(0, 0, iconPx, iconPx)
            drawable.draw(canvas)
        }
        return bitmap
    }

    fun launch(entry: AppEntry): Boolean {
        val user = userManager.getUserForSerialNumber(entry.userSerial) ?: return false
        return runCatching { launcherApps.startMainActivity(entry.component, user, null, null) }.isSuccess
    }

    private companion object {
        const val ICON_DP = 56
    }
}
