package com.dinesh.appblocker

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent

/**
 * The engine of the app.
 *
 * Android fires TYPE_WINDOW_STATE_CHANGED whenever a new window comes to the
 * foreground, and tells us which package owns it. If that package is on the
 * block list and blocking is switched on, we send the user home and put the
 * block screen up.
 *
 * An accessibility service is the only way to do this without root: there is
 * no API that lets an ordinary app stop another app from launching.
 */
class BlockerService : AccessibilityService() {

    companion object {
        private const val TAG = "BlockerService"

        /** Ignore repeat events for the same package inside this window (ms). */
        private const val DEBOUNCE_MS = 700L
    }

    private var lastBlockedPackage: String? = null
    private var lastBlockedAt = 0L

    /** Launcher / Settings / system UI. Resolved once when the service starts. */
    private var neverBlockable: Set<String> = emptySet()

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val target = event.packageName?.toString() ?: return

        // Never block ourselves, or we would loop forever on our own block screen.
        if (target == packageName) return

        // Second line of defence: even if a protected package somehow ended up
        // on the block list, never act on it. Blocking the launcher or Settings
        // would leave no way to switch the service back off.
        if (neverBlockable.contains(target)) return

        if (!Prefs.isEnabled(this)) return
        if (!Prefs.isBlocked(this, target)) return

        // A single app switch can emit several window events. Without this the
        // block screen would be launched repeatedly.
        val now = SystemClock.elapsedRealtime()
        if (target == lastBlockedPackage && now - lastBlockedAt < DEBOUNCE_MS) return
        lastBlockedPackage = target
        lastBlockedAt = now

        Log.d(TAG, "Blocking $target")

        // Step 1: always works, no permission needed. Gets the blocked app off
        // screen even if the overlay permission was never granted.
        performGlobalAction(GLOBAL_ACTION_HOME)

        // Step 2: show why. On Android 10+ a background activity start needs
        // the "display over other apps" permission, so this is best-effort.
        try {
            val intent = Intent(this, BlockActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION
                )
                putExtra(BlockActivity.EXTRA_PACKAGE, target)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, "Could not show block screen: ${e.message}")
        }
    }

    override fun onInterrupt() {
        // Required override. Nothing to interrupt.
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        neverBlockable = SafetyList.neverBlockable(this)
        Log.d(TAG, "Accessibility service connected")
    }
}
