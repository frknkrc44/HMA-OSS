package org.frknkrc44.hma_oss.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.Gravity
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import icu.nullptr.hidemyapplist.common.Utils.nonHmaModules
import icu.nullptr.hidemyapplist.service.ServiceClient
import icu.nullptr.hidemyapplist.ui.view.ListItemView
import org.frknkrc44.hma_oss.R

@SuppressLint("NotifyDataSetChanged")
@Suppress("DEPRECATION")
class FixIssueAdapter(
    val context: Context,
    val list: List<String>
) : RecyclerView.Adapter<FixIssueAdapter.ViewHolder>()  {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ListItemView(parent.context)
        view.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(list[position])

    override fun getItemCount() = list.size

    inner class ViewHolder(val view: ListItemView) : RecyclerView.ViewHolder(view) {
        fun bind(packageName: String) {
            with(context.packageManager) {
                val appInfo = ServiceClient.getPackageInfo(packageName, 0)?.applicationInfo
                view.text = getApplicationLabel(appInfo!!)
                view.binding.icon.setImageDrawable(getApplicationIcon(appInfo))
            }

            @SuppressLint("UseCompatLoadingForDrawables")
            with(view.binding.button as MaterialButton) {
                isVisible = true
                icon = resources.getDrawable(
                    R.drawable.outline_delete_24,
                    context.theme,
                )

                insetLeft = 0
                insetTop = 0
                insetRight = 0
                insetBottom = 0
                iconPadding = 0
                iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START

                setOnClickListener {
                    if (packageName in nonHmaModules) {
                        uninstallPackage(packageName)
                    } else {
                        MaterialAlertDialogBuilder(context)
                            .setTitle(R.string.home_migrate_data)
                            .setMessage(R.string.home_migrate_data_summary)
                            .setPositiveButton(R.string.yes) { _, _ ->
                                migrateOrUninstallOnly(true, packageName)
                            }
                            .setNegativeButton(android.R.string.cancel, null)
                            .setNeutralButton(R.string.home_migrate_uninstall_only) { _, _ ->
                                migrateOrUninstallOnly(false, packageName)
                            }
                            .show()
                    }
                }
            }
        }
    }

    private fun migrateOrUninstallOnly(migrateData: Boolean, packageName: String) {
        if (migrateData && !ServiceClient.migrateData(packageName)) {
            showMigrateStatusDialog(false)
            return
        }

        uninstallPackage(packageName)
        showMigrateStatusDialog(true)
    }

    private fun uninstallPackage(packageName: String) = context.startActivity(
        Intent(Intent.ACTION_UNINSTALL_PACKAGE).apply {
            data = "package:$packageName".toUri()
        }
    )

    private fun showMigrateStatusDialog(success: Boolean) {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.home_migrate_data)
            .setMessage(if (success) {
                R.string.home_migrate_data_completed
            } else {
                R.string.home_migrate_data_failed
            })
            .setNegativeButton(android.R.string.ok, null)
            .show()
    }
}
