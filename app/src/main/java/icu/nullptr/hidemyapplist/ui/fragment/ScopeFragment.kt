package top.secret.hma.v1.ui.fragment

import android.os.Bundle
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import top.secret.hma.v1.service.ConfigManager
import top.secret.hma.v1.ui.adapter.AppScopeAdapter
import top.secret.hma.v1.ui.util.navController

class ScopeFragment : AppSelectFragment() {

    private lateinit var checked: MutableSet<String>

    override val firstComparator: Comparator<String> = Comparator.comparing { !checked.contains(it) }

    private val requestKey by lazy {
        val args by navArgs<ScopeFragmentArgs>()
        if (args.isOpposite) "app_opposite_select" else "app_select"
    }

    override val adapter by lazy {
        val args by navArgs<ScopeFragmentArgs>()
        checked = args.checked.toMutableSet()
        if (!args.filterOnlyEnabled) AppScopeAdapter(checked, null)
        else AppScopeAdapter(checked) { ConfigManager.getAppConfig(it)?.useWhitelist == args.isWhiteList }
    }

    override fun onBack() {
        setFragmentResult(requestKey, Bundle().apply {
            putStringArrayList("checked", ArrayList(checked))
        })
        navController.navigateUp()
    }
}
