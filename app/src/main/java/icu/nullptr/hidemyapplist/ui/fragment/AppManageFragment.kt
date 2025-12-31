package icu.nullptr.hidemyapplist.ui.fragment

import icu.nullptr.hidemyapplist.service.ConfigManager
import icu.nullptr.hidemyapplist.ui.adapter.AppManageAdapter
import icu.nullptr.hidemyapplist.ui.util.navigate
import icu.nullptr.hidemyapplist.util.PackageHelper
import org.frknkrc44.hma_oss.R
import org.frknkrc44.hma_oss.ui.fragment.AppSettingsV2FragmentArgs

class AppManageFragment : AppSelectFragment() {

    override val firstComparator: Comparator<String> = Comparator.comparing(ConfigManager::isHideEnabled).reversed()

    override val adapter = AppManageAdapter {
        if (PackageHelper.exists(it)) {
            val args = AppSettingsV2FragmentArgs(it)
            navigate(R.id.nav_app_settings, args.toBundle())
        }
    }
}
