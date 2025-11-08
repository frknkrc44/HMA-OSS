package org.frknkrc44.hma_oss.ui.fragment

import android.view.MenuItem
import androidx.navigation.fragment.navArgs
import org.frknkrc44.hma_oss.R
import org.frknkrc44.hma_oss.ui.adapter.SettingsTemplateListAdapter

class SettingsTemplateInnerFragment : BaseSettingsPTFragment() {
    private val args by lazy { navArgs<SettingsTemplateInnerFragmentArgs>() }

    override val title by lazy {
        if (args.value.name.isNullOrEmpty()) null else args.value.name
    }

    override val adapter by lazy {
        SettingsTemplateListAdapter(title) {
            // TODO: Add setting popup menu with options: Edit, Remove
        }
    }

    fun onMenuOptionSelected(item: MenuItem) {
        when (item.itemId) {
            R.id.menu_add -> {
                // TODO: Implement add setting screen
            }
        }
    }

    override val menu = Pair(
        R.menu.menu_settings_template,
        this::onMenuOptionSelected,
    )
}