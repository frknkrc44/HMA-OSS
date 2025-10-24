package icu.nullptr.hidemyapplist.ui.adapter

import android.view.ViewGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import icu.nullptr.hidemyapplist.common.Constants
import icu.nullptr.hidemyapplist.service.ConfigManager
import icu.nullptr.hidemyapplist.service.PrefManager
import icu.nullptr.hidemyapplist.ui.view.AppItemView
import org.frknkrc44.hma_oss.R

class AppManageAdapter(
    private val onItemClickListener: (String) -> Unit
) : AppSelectAdapter() {

    inner class ViewHolder(view: AppItemView) : AppSelectAdapter.ViewHolder(view) {
        init {
            view.setOnClickListener {
                if (!PrefManager.bypassRiskyPackageWarning && Constants.riskyPackages.contains(view.binding.packageName.text)) {
                    MaterialAlertDialogBuilder(view.context)
                        .setTitle(R.string.app_warning_risky_package_title)
                        .setMessage(R.string.app_warning_risky_package_desc)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            onItemClickListener.invoke(filteredList[absoluteAdapterPosition])
                        }
                        .show()

                    return@setOnClickListener
                }

                onItemClickListener.invoke(filteredList[absoluteAdapterPosition])
            }
        }

        override fun bind(packageName: String) {
            (itemView as AppItemView).let {
                it.load(packageName)
                it.showEnabled = ConfigManager.isHideEnabled(packageName)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = AppItemView(parent.context, false)
        view.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return ViewHolder(view)
    }
}
