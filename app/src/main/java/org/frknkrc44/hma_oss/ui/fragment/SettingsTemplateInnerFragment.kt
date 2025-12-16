package org.frknkrc44.hma_oss.ui.fragment

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.clearFragmentResultListener
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import icu.nullptr.hidemyapplist.common.Constants
import icu.nullptr.hidemyapplist.common.Utils.generateRandomHex
import icu.nullptr.hidemyapplist.common.settings_presets.ReplacementItem
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
                MaterialAlertDialogBuilder(requireContext()).apply {
                    setTitle(R.string.add)
                    setNegativeButton(android.R.string.cancel, null)
                    setItems(R.array.settings_template_inner_add_texts) { dialog, which ->
                        when (which) {
                            0 -> {
                                val args = EditSettingFragmentArgs(database = null, name = "", value = null)
                                launchEditSettingFragment(args)
                            }
                            1 -> {
                                processIncomingSetting(
                                    ReplacementItem(
                                        name = Settings.Secure.ANDROID_ID,
                                        value = generateRandomHex(16),
                                        database = Constants.SETTINGS_SECURE,
                                    )
                                )
                            }
                            2 -> {
                                launchInputMethodSelectorDialog { componentName ->
                                    processIncomingSetting(
                                        ReplacementItem(
                                            name = Settings.Secure.DEFAULT_INPUT_METHOD,
                                            value = componentName.flattenToString(),
                                            database = Constants.SETTINGS_SECURE,
                                        )
                                    )
                                }
                            }
                        }

                        dialog.dismiss()
                    }
                }.show()
            }
        }
    }

    fun launchEditSettingFragment(args: EditSettingFragmentArgs) {
        setFragmentResultListener("edit_setting") { _, bundle ->
            fun deal() {
                val item = bundle.toTargetSettingList().firstOrNull() ?: return
                processIncomingSetting(item)
            }
            deal()
            clearFragmentResultListener("edit_setting")
        }

        navigate(R.id.nav_settings_templates_edit_setting, args.toBundle())
    }

    private fun processIncomingSetting(item: ReplacementItem) {
        val index = adapter.items.indexOfFirst { it.name == item.name && it.database == item.database }

        if (index >= 0) {
            adapter.items[index] = item
            adapter.notifyItemChanged(index)
        } else {
            adapter.items.add(item)
            adapter.notifyItemInserted(adapter.items.size - 1)
        }
    }

    private fun launchInputMethodSelectorDialog(onSelected: (ComponentName) -> Unit) {
        val pkgMgr = requireContext().packageManager
        val immService = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val immMap = immService.inputMethodList.associate { it.loadLabel(pkgMgr) to it.component }

        MaterialAlertDialogBuilder(requireContext()).apply {
            setTitle(R.string.settings_preset_input_method)
            setNegativeButton(android.R.string.cancel, null)

            val mapKeys = immMap.keys.toTypedArray()
            setItems(mapKeys) { _, which -> onSelected(immMap[mapKeys[which]]!!) }
        }.show()
    }

    override val menu = Pair(
        R.menu.menu_settings_template,
        this::onMenuOptionSelected,
    )
}
