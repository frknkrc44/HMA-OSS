package org.frknkrc44.hma_oss.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import icu.nullptr.hidemyapplist.common.AppPresets
import icu.nullptr.hidemyapplist.common.SettingsPresets
import icu.nullptr.hidemyapplist.service.ConfigManager
import icu.nullptr.hidemyapplist.ui.view.ListItemView
import org.frknkrc44.hma_oss.BuildConfig
import org.frknkrc44.hma_oss.R

class AppPresetListAdapter(
    context: Context,
    private val onClickListener: ((ConfigManager.PresetInfo) -> Unit)?
) : RecyclerView.Adapter<AppPresetListAdapter.ViewHolder>() {

    private var list = mutableListOf<ConfigManager.PresetInfo>()

    init {
        updateList(context)
    }

    inner class ViewHolder(view: ListItemView) : RecyclerView.ViewHolder(view) {
        fun bind(item: ConfigManager.PresetInfo) {
            with(itemView as ListItemView) {
                if (item.type == null) {
                    showAsHeader()
                } else {
                    setIcon(
                        when (item.type) {
                            ConfigManager.PTType.APP -> R.drawable.baseline_assignment_24
                            ConfigManager.PTType.SETTINGS -> R.drawable.baseline_settings_24
                        }
                    )

                    itemView.setOnClickListener {
                        onClickListener?.invoke(item)
                    }
                }

                text = item.translation
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ListItemView(parent.context)
        view.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return ViewHolder(view)
    }

    override fun getItemCount() = list.size

    override fun getItemId(position: Int) = list[position].name.hashCode().toLong()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(list[position])

    @SuppressLint("NotifyDataSetChanged", "DiscouragedApi")
    private fun updateList(context: Context) {
        list.clear()

        val appPresetNames = AppPresets.instance.getAllPresetNames()
        val appPresetTranslations = appPresetNames.map { name ->
            try {
                val id = context.resources.getIdentifier(
                    "preset_${name}",
                    "string",
                    BuildConfig.APPLICATION_ID
                )

                return@map if (id != 0) { context.resources.getString(id) } else { name }
            } catch (_: Throwable) {}

            name
        }

        list += ConfigManager.PresetInfo(
            "preset",
            null,
            context.getString(R.string.title_preset),
        )

        list += appPresetNames
            .map { ConfigManager.PresetInfo(
                it,
                ConfigManager.PTType.APP,
                appPresetTranslations[appPresetNames.indexOf(it)]
            ) }
            .sortedWith { a, b -> a.translation.lowercase().compareTo(b.translation.lowercase()) }

        list += ConfigManager.PresetInfo(
            "settings_preset",
            null,
            context.getString(R.string.title_settings_preset),
        )

        val settingsPresetNames = SettingsPresets.instance.getAllPresetNames()
        val settingsPresetTranslations = settingsPresetNames.map { name ->
            try {
                val id = context.resources.getIdentifier(
                    "settings_preset_${name}",
                    "string",
                    BuildConfig.APPLICATION_ID
                )

                return@map if (id != 0) { context.resources.getString(id) } else { name }
            } catch (_: Throwable) {}

            name
        }

        list += settingsPresetNames
            .map { ConfigManager.PresetInfo(
                it,
                ConfigManager.PTType.SETTINGS,
                settingsPresetTranslations[settingsPresetNames.indexOf(it)]
            ) }
            .sortedWith { a, b -> a.translation.lowercase().compareTo(b.translation.lowercase()) }

        notifyDataSetChanged()
    }
}
