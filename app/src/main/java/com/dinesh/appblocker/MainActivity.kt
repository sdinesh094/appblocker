package com.dinesh.appblocker

import android.content.ComponentName
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var masterSwitch: SwitchCompat
    private lateinit var statusService: TextView
    private lateinit var statusOverlay: TextView
    private lateinit var accessibilityButton: Button
    private lateinit var overlayButton: Button
    private lateinit var searchField: EditText
    private lateinit var systemAppsCheck: CheckBox
    private lateinit var recyclerView: RecyclerView
    private lateinit var progress: ProgressBar
    private lateinit var adapter: AppListAdapter

    /** Every launchable app on the device, loaded once off the main thread. */
    private var allApps: List<AppEntry> = emptyList()

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        masterSwitch = findViewById(R.id.masterSwitch)
        statusService = findViewById(R.id.statusService)
        statusOverlay = findViewById(R.id.statusOverlay)
        accessibilityButton = findViewById(R.id.accessibilityButton)
        overlayButton = findViewById(R.id.overlayButton)
        searchField = findViewById(R.id.searchField)
        systemAppsCheck = findViewById(R.id.systemAppsCheck)
        recyclerView = findViewById(R.id.appList)
        progress = findViewById(R.id.progress)

        adapter = AppListAdapter(
            items = emptyList(),
            isBlocked = { pkg -> Prefs.isBlocked(this, pkg) },
            onToggle = { pkg, blocked -> Prefs.setBlocked(this, pkg, blocked) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        masterSwitch.isChecked = Prefs.isEnabled(this)
        masterSwitch.setOnCheckedChangeListener { _, checked ->
            Prefs.setEnabled(this, checked)
            if (checked && !isAccessibilityServiceEnabled()) {
                Toast.makeText(
                    this,
                    getString(R.string.status_service_off),
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        systemAppsCheck.isChecked = Prefs.showSystemApps(this)
        systemAppsCheck.setOnCheckedChangeListener { _, checked ->
            Prefs.setShowSystemApps(this, checked)
            applyFilter()
        }

        accessibilityButton.setOnClickListener {
            openSettings(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        overlayButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                openSettings(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
            } else {
                openSettings(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                })
            }
        }

        searchField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
            override fun afterTextChanged(s: Editable?) = applyFilter()
        })

        loadInstalledApps()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
        // The list reflects saved state, which may have changed elsewhere.
        adapter.notifyDataSetChanged()
    }

    // ---------------------------------------------------------------- status

    private fun refreshStatus() {
        val serviceOn = isAccessibilityServiceEnabled()
        statusService.text = getString(
            if (serviceOn) R.string.status_service_on else R.string.status_service_off
        )

        val overlayOn = hasOverlayPermission()
        statusOverlay.text = getString(
            if (overlayOn) R.string.status_overlay_on else R.string.status_overlay_off
        )
    }

    /**
     * Reads the system's list of enabled accessibility services and looks for
     * ours. Checks both the long and short component forms because different
     * OEM builds store it differently.
     */
    private fun isAccessibilityServiceEnabled(): Boolean {
        val component = ComponentName(this, BlockerService::class.java)
        val longForm = component.flattenToString()
        val shortForm = component.flattenToShortString()

        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        return enabled.split(':').any { entry ->
            entry.equals(longForm, ignoreCase = true) ||
                entry.equals(shortForm, ignoreCase = true)
        }
    }

    private fun hasOverlayPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }

    private fun openSettings(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, R.string.toast_no_settings, Toast.LENGTH_LONG).show()
        }
    }

    // ------------------------------------------------------------- app list

    private fun loadInstalledApps() {
        progress.visibility = View.VISIBLE

        Thread {
            val entries = try {
                buildAppList()
            } catch (e: Exception) {
                emptyList()
            }

            mainHandler.post {
                allApps = entries
                progress.visibility = View.GONE
                applyFilter()
            }
        }.start()
    }

    private fun buildAppList(): List<AppEntry> {
        val pm = packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        // Apps with a home-screen icon — what the user thinks of as "apps".
        val launchable = try {
            pm.queryIntentActivities(launcherIntent, 0)
                .mapNotNull { it.activityInfo?.packageName }
                .toSet()
        } catch (e: Exception) {
            emptySet()
        }

        val installed = try {
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
        } catch (e: Exception) {
            emptyList<ApplicationInfo>()
        }

        val neverBlockable = SafetyList.neverBlockable(this)

        return installed
            .filter { it.packageName != packageName }
            .filter { !neverBlockable.contains(it.packageName) }
            .map { info ->
                AppEntry(
                    packageName = info.packageName,
                    label = try {
                        pm.getApplicationLabel(info).toString()
                    } catch (e: Exception) {
                        info.packageName
                    },
                    icon = try {
                        pm.getApplicationIcon(info)
                    } catch (e: Exception) {
                        null
                    },
                    hasLauncher = launchable.contains(info.packageName)
                )
            }
            .sortedBy { it.label.lowercase(Locale.getDefault()) }
    }

    private fun applyFilter() {
        val query = searchField.text?.toString()?.trim()?.lowercase(Locale.getDefault()) ?: ""
        val includeHidden = systemAppsCheck.isChecked
        // Read the blocked set once rather than once per row.
        val blocked = Prefs.blockedPackages(this)

        val filtered = allApps
            .filter { includeHidden || it.hasLauncher || blocked.contains(it.packageName) }
            .filter {
                query.isEmpty() ||
                    it.label.lowercase(Locale.getDefault()).contains(query) ||
                    it.packageName.lowercase(Locale.getDefault()).contains(query)
            }

        adapter.submit(filtered)
    }
}
