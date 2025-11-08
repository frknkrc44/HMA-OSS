package org.frknkrc44.hma_oss.ui.viewmodel

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import icu.nullptr.hidemyapplist.common.settings_presets.ReplacementItem
import icu.nullptr.hidemyapplist.service.ConfigManager
import kotlinx.coroutines.flow.MutableStateFlow
import org.frknkrc44.hma_oss.ui.fragment.SettingsTemplateConfFragmentArgs

class SettingsTemplateConfViewModel(
    @Suppress("unused")
    val originalName: String?,
    var name: String?
) : ViewModel() {

    class Factory(private val args: SettingsTemplateConfFragmentArgs) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SettingsTemplateConfViewModel::class.java)) {
                val viewModel = SettingsTemplateConfViewModel(args.name, args.name)
                args.name?.let {
                    viewModel.appliedAppList.value = ConfigManager.getSettingTemplateAppliedAppList(it)
                    viewModel.targetSettingList.value = ConfigManager.getSettingTemplateTargetSettingList(it)
                }
                @Suppress("UNCHECKED_CAST")
                return viewModel as T
            } else throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    val appliedAppList = MutableStateFlow<ArrayList<String>>(ArrayList())
    val targetSettingList = MutableStateFlow<ArrayList<ReplacementItem>>(ArrayList())

    fun targetSettingListToBundle() = Bundle().apply {
        for (item in targetSettingList.value) {
            putStringArrayList(item.name, arrayListOf(item.value, item.database))
        }
    }
}

fun Bundle.bundleToTargetSettingList() = keySet().map {
    val item = getStringArray(it)!!
    ReplacementItem(it, item[0], item[1])
}
