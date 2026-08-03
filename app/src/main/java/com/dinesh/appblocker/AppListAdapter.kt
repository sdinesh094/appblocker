package com.dinesh.appblocker

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * One installed app as shown in the picker list.
 *
 * [hasLauncher] is true when the package has a home-screen icon. That — not
 * the system/user flag — is the right default filter, because YouTube and
 * Chrome are preinstalled system apps on Samsung and must still appear.
 */
data class AppEntry(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val hasLauncher: Boolean
)

class AppListAdapter(
    private var items: List<AppEntry>,
    private val isBlocked: (String) -> Boolean,
    private val onToggle: (String, Boolean) -> Unit
) : RecyclerView.Adapter<AppListAdapter.AppViewHolder>() {

    class AppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.appIcon)
        val name: TextView = view.findViewById(R.id.appName)
        val pkg: TextView = view.findViewById(R.id.appPackage)
        val check: CheckBox = view.findViewById(R.id.appCheck)
    }

    fun submit(newItems: List<AppEntry>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val item = items[position]
        holder.icon.setImageDrawable(item.icon)
        holder.name.text = item.label
        holder.pkg.text = item.packageName
        holder.check.isChecked = isBlocked(item.packageName)

        // The whole row is the touch target; the CheckBox is display-only.
        holder.itemView.setOnClickListener {
            val next = !holder.check.isChecked
            holder.check.isChecked = next
            onToggle(item.packageName, next)
        }
    }

    override fun getItemCount(): Int = items.size
}
