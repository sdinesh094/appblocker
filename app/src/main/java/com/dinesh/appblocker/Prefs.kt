package com.dinesh.appblocker

import android.content.Context
import android.content.SharedPreferences

/**
 * All persisted state for the app: the master on/off flag and the set of
 * package names the user has chosen to block. Backed by SharedPreferences,
 * which is process-wide, so the accessibility service sees changes made in
 * the UI immediately.
 */
object Prefs {

    private const val FILE = "app_blocker_prefs"
    private const val KEY_ENABLED = "blocking_enabled"
    private const val KEY_BLOCKED = "blocked_packages"
    private const val KEY_SHOW_SYSTEM = "show_system_apps"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    /**
     * Returns a defensive copy. The Set handed back by getStringSet must never
     * be mutated in place, so every caller gets its own copy.
     */
    fun blockedPackages(context: Context): Set<String> =
        HashSet(prefs(context).getStringSet(KEY_BLOCKED, emptySet()) ?: emptySet())

    fun isBlocked(context: Context, packageName: String): Boolean =
        blockedPackages(context).contains(packageName)

    fun setBlocked(context: Context, packageName: String, blocked: Boolean) {
        val updated = blockedPackages(context).toMutableSet()
        if (blocked) updated.add(packageName) else updated.remove(packageName)
        prefs(context).edit().putStringSet(KEY_BLOCKED, updated).apply()
    }

    fun showSystemApps(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SHOW_SYSTEM, false)

    fun setShowSystemApps(context: Context, show: Boolean) {
        prefs(context).edit().putBoolean(KEY_SHOW_SYSTEM, show).apply()
    }
}
