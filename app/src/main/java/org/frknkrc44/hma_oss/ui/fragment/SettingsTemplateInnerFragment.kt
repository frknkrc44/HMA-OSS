package org.frknkrc44.hma_oss.ui.fragment

import android.view.MenuItem
import androidx.fragment.app.clearFragmentResultListener
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import icu.nullptr.hidemyapplist.ui.util.navigate
import org.frknkrc44.hma_oss.R
import org.frknkrc44.hma_oss.ui.adapter.SettingsTemplateListAdapter
import org.frknkrc44.hma_oss.ui.util.toEditSettingFragmentArgs
import org.frknkrc44.hma_oss.ui.util.toTargetSettingList

class SettingsTemplateInnerFragment : BaseSettingsPTFragment() {
    private val args by lazy { navArgs<SettingsTemplateInnerFragmentArgs>() }

    override val title by lazy { getString(R.string.edit_list) }

    override val adapter by lazy {
        SettingsTemplateListAdapter(args.value.items) { adapter, item ->
            MaterialAlertDialogBuilder(requireContext()).apply {
                setTitle(item.name)
                setItems(
                    R.array.settings_template_inner_action_texts,
                ) { dialog, which ->
                    when (which) {
                        0 -> launchEditSettingFragment(item.toEditSettingFragmentArgs())
                        1 -> {
                            val index = adapter.items.indexOf(item)
                            if (index >= 0) {
                                adapter.items.removeAt(index)
                                adapter.notifyItemRemoved(index)
                            }
                        }
                    }

                    dialog.dismiss()
                }
            }.show()
        }
    }

    override fun onBack() {
        setFragmentResult(
            "setting_select",
            adapter.targetSettingListToBundle()
        )

        super.onBack()
    }

    fun onMenuOptionSelected(item: MenuItem) {
        when (item.itemId) {
            R.id.menu_add -> {
                val args = EditSettingFragmentArgs(database = null, name = "", value = null)
                launchEditSettingFragment(args)
            }
        }
    }

    fun launchEditSettingFragment(args: EditSettingFragmentArgs) {
        setFragmentResultListener("edit_setting") { _, bundle ->
            fun deal() {
                val item = bundle.toTargetSettingList().firstOrNull()

                if (item != null) {
                    val index = adapter.items.indexOfFirst { it.name == item.name && it.database == item.database }

                    if (index >= 0) {
                        adapter.items[index] = item
                        adapter.notifyItemChanged(index)
                    } else {
                        adapter.items.add(item)
                        adapter.notifyItemInserted(adapter.items.size - 1)
                    }
                }
            }
            deal()
            clearFragmentResultListener("edit_setting")
        }

        navigate(R.id.nav_settings_templates_edit_setting, args.toBundle())
    }

    override val menu = Pair(
        R.menu.menu_settings_template,
        this::onMenuOptionSelected,
    )
}
