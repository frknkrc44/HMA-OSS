package top.secret.hma.v1.ui.fragment

import top.secret.hma.v1.service.ConfigManager
import top.secret.hma.v1.ui.adapter.AppManageAdapter
import top.secret.hma.v1.ui.util.navigate
import top.secret.hma.v1.util.PackageHelper
import top.secret.hma.v1.R
import top.secret.hma.v1.ui.fragment.AppSettingsV2FragmentArgs

class AppManageFragment : AppSelectFragment() {

    override val firstComparator: Comparator<String> = Comparator.comparing(ConfigManager::isHideEnabled).reversed()

    override val adapter = AppManageAdapter {
        if (PackageHelper.exists(it)) {
            val args = AppSettingsV2FragmentArgs(it)
            navigate(R.id.nav_app_settings, args.toBundle())
        }
    }
}
