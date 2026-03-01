package top.secret.hma.v1.ui.adapter

import android.view.ViewGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import top.secret.hma.v1.common.Constants
import top.secret.hma.v1.service.ConfigManager
import top.secret.hma.v1.service.PrefManager
import top.secret.hma.v1.ui.view.AppItemView
import top.secret.hma.v1.R

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
