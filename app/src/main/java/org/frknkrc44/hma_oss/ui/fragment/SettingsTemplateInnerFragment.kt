package org.frknkrc44.hma_oss.ui.fragment

import android.util.Log
import android.view.MenuItem
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import icu.nullptr.hidemyapplist.service.ServiceClient
import org.frknkrc44.hma_oss.R
import org.frknkrc44.hma_oss.ui.adapter.SettingsTemplateListAdapter

class SettingsTemplateInnerFragment : BaseSettingsPTFragment() {
    private val args by lazy { navArgs<SettingsTemplateInnerFragmentArgs>() }

    override val title by lazy { getString(R.string.edit_list) }

    override val adapter by lazy {
        SettingsTemplateListAdapter(args.value.name) { adapter, item ->
            MaterialAlertDialogBuilder(requireContext()).apply {
                setTitle(item.name)
                setItems(
                    R.array.settings_template_inner_action_texts,
                ) { dialog, which ->
                    when (which) {
                        0 -> {
                            // TODO: Implement add/edit screen
                        }
                        1 -> adapter.items.remove(item)
                    }

                    dialog.dismiss()
                }
            }.show()
        }
    }

    fun onMenuOptionSelected(item: MenuItem) {
        when (item.itemId) {
            R.id.menu_add -> {
                // TODO: Implement add/edit setting screen
                ServiceClient.log(
                    Log.INFO,
                    javaClass.simpleName,
                    ServiceClient.listAllSettings("global")?.sortedWith { o1, o2 -> o1.compareTo(o2, true) }.toString()
                )
            }
        }
    }

    override val menu = Pair(
        R.menu.menu_settings_template,
        this::onMenuOptionSelected,
    )
}