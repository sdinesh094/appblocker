package com.dinesh.appblocker

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/**
 * Packages that must never be blockable.
 *
 * Blocking the home launcher would make the phone unusable: every return to
 * the home screen would re-trigger the block, and reaching Settings to switch
 * the service off would be painful. Settings and the system UI are excluded
 * for the same reason — you need a way out.
 */
object SafetyList {

    private val ALWAYS_EXCLUDED = setOf(
        "android",
        "com.android.settings",
        "com.android.systemui",
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller",
        // Samsung / One UI equivalents
        "com.sec.android.app.launcher",
        "com.samsung.android.app.settings",
        "com.samsung.android.settings",
        "com.samsung.android.lool"
    )

    /**
     * The fixed exclusions plus whichever app is currently the default home
     * launcher on this device.
     */
    fun neverBlockable(context: Context): Set<String> {
        val result = HashSet(ALWAYS_EXCLUDED)
        currentLauncher(context)?.let { result.add(it) }
        return result
    }

    private fun currentLauncher(context: Context): String? = try {
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        context.packageManager
            .resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo
            ?.packageName
    } catch (e: Exception) {
        null
    }
}
