package com.dinesh.appblocker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView

/**
 * The full-screen "Blocked" panel. Deliberately extends the plain framework
 * Activity so it carries no AppCompat theme requirements and starts fast.
 */
class BlockActivity : Activity() {

    companion object {
        const val EXTRA_PACKAGE = "extra_package"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_block)

        val blockedPackage = intent?.getStringExtra(EXTRA_PACKAGE)
        findViewById<TextView>(R.id.blockedAppName).text =
            blockedPackage?.let { labelFor(it) } ?: getString(R.string.this_app)

        findViewById<Button>(R.id.closeButton).setOnClickListener { goHome() }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        val blockedPackage = intent?.getStringExtra(EXTRA_PACKAGE)
        findViewById<TextView>(R.id.blockedAppName).text =
            blockedPackage?.let { labelFor(it) } ?: getString(R.string.this_app)
    }

    private fun labelFor(packageName: String): String = try {
        val pm = packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    } catch (e: Exception) {
        packageName
    }

    private fun goHome() {
        val home = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            startActivity(home)
        } catch (e: Exception) {
            // Ignore: worst case we simply finish below.
        }
        finish()
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        // Back must not drop the user into the app that was just blocked.
        goHome()
    }
}
