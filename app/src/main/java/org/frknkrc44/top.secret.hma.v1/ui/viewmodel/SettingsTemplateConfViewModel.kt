package top.secret.hma.v1.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import top.secret.hma.v1.common.settings_presets.ReplacementItem
import top.secret.hma.v1.service.ConfigManager
import kotlinx.coroutines.flow.MutableStateFlow
import top.secret.hma.v1.ui.fragment.SettingsTemplateConfFragmentArgs
import top.secret.hma.v1.ui.util.targetSettingListToBundle

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

    fun targetSettingListToBundle() = targetSettingList.value.targetSettingListToBundle()
}

