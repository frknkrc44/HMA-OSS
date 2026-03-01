package org.frknkrc44.hma_oss.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import icu.nullptr.hidemyapplist.common.JsonConfig
import kotlinx.coroutines.flow.MutableStateFlow

class BulkConfigWizardViewModel() : ViewModel() {

    class Factory() : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(BulkConfigWizardViewModel::class.java)) {
                val viewModel = BulkConfigWizardViewModel()
                @Suppress("UNCHECKED_CAST")
                return viewModel as T
            } else throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    val appliedAppList = MutableStateFlow<ArrayList<String>>(ArrayList())
    val appConfig = MutableStateFlow<JsonConfig.AppConfig?>(null)
}
