package icu.nullptr.hidemyapplist.ui.fragment

import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.WindowInsets
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import by.kirich1409.viewbindingdelegate.viewBinding
import icu.nullptr.hidemyapplist.service.PrefManager
import icu.nullptr.hidemyapplist.service.ServiceClient
import icu.nullptr.hidemyapplist.ui.adapter.LogAdapter
import icu.nullptr.hidemyapplist.ui.util.contentResolver
import icu.nullptr.hidemyapplist.ui.util.navController
import icu.nullptr.hidemyapplist.ui.util.setupToolbar
import icu.nullptr.hidemyapplist.ui.util.showToast
import kotlinx.coroutines.launch
import org.frknkrc44.hma_oss.R
import org.frknkrc44.hma_oss.databinding.FragmentLogsBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class LogsFragment : Fragment(R.layout.fragment_logs) {

    private val binding by viewBinding<FragmentLogsBinding>()
    private val adapter by lazy { LogAdapter(requireContext()) }
    private var logCache: String? = null

    private val saveSAFLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/x-log")) save@{ uri ->
            if (uri == null) return@save
            if (logCache.isNullOrEmpty()) {
                showToast(R.string.logs_empty)
                return@save
            }
            contentResolver.openOutputStream(uri).use { output ->
                if (output == null) showToast(R.string.home_export_failed)
                else output.write(logCache!!.toByteArray())
            }
            showToast(R.string.logs_saved)
        }

    private fun updateLogs() {
        lifecycleScope.launch {
            logCache = try {
                ServiceClient.logs
            } catch (_: Throwable) {
                "[ERROR] 01-01 01:01:01 (${getString(R.string.app_name)}) Cannot read logs due to Binder issues, try reading ${ServiceClient.logFileLocation} manually"
            }
            val raw = logCache?.split("\n")
            if (raw == null) {
                binding.serviceOff.visibility = View.VISIBLE
            } else {
                binding.serviceOff.visibility = View.GONE
                adapter.logs = buildList {
                    val cur = StringBuilder()
                    for (line in raw) {
                        if (line.startsWith('[')) {
                            if (cur.isNotEmpty()) {
                                val log = LogAdapter.parseLog(cur.toString())
                                if (log != null) add(log)
                            }
                            cur.clear()
                        }
                        cur.append(line)
                    }
                    if (cur.isNotEmpty()) {
                        val log = LogAdapter.parseLog(cur.toString())
                        if (log != null) add(log)
                    }
                    if (!PrefManager.logFilter_reverseOrder) reverse()
                }
            }
        }
    }

    private fun onMenuOptionSelected(item: MenuItem) {
        when (item.itemId) {
            R.id.menu_refresh -> updateLogs()
            R.id.menu_save -> {
                val date = SimpleDateFormat("yyyy-MM-dd_HH.mm.ss", Locale.getDefault()).format(Date())
                saveSAFLauncher.launch("hma_logs_$date.log")
            }
            R.id.menu_delete -> {
                ServiceClient.clearLogs()
                updateLogs()
            }
            R.id.menu_filter_debug -> {
                item.isChecked = true
                PrefManager.logFilter_level = 0
                updateLogs()
            }
            R.id.menu_filter_info -> {
                item.isChecked = true
                PrefManager.logFilter_level = 1
                updateLogs()
            }
            R.id.menu_filter_warn -> {
                item.isChecked = true
                PrefManager.logFilter_level = 2
                updateLogs()
            }
            R.id.menu_filter_error -> {
                item.isChecked = true
                PrefManager.logFilter_level = 3
                updateLogs()
            }
            R.id.menu_reverse_order -> {
                item.isChecked = !item.isChecked
                PrefManager.logFilter_reverseOrder = item.isChecked
                updateLogs()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        with(binding.toolbar) {
            setupToolbar(
                toolbar = this,
                title = getString(R.string.title_logs),
                menuRes = R.menu.menu_logs,
                onMenuOptionSelected = this@LogsFragment::onMenuOptionSelected
            )
            setNavigationIcon(R.drawable.baseline_arrow_back_24)
            setNavigationOnClickListener { navController.popBackStack() }
            // isTitleCentered = true
        }

        with(binding.toolbar.menu) {
            when (PrefManager.logFilter_level) {
                0 -> findItem(R.id.menu_filter_debug).isChecked = true
                1 -> findItem(R.id.menu_filter_info).isChecked = true
                2 -> findItem(R.id.menu_filter_warn).isChecked = true
                3 -> findItem(R.id.menu_filter_error).isChecked = true
            }
            findItem(R.id.menu_reverse_order).isChecked = PrefManager.logFilter_reverseOrder
        }

        binding.list.layoutManager = LinearLayoutManager(context)
        binding.list.adapter = adapter
        binding.list.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        updateLogs()

        binding.root.setOnApplyWindowInsetsListener { v, insets ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val barInsets = insets.getInsets(WindowInsets.Type.systemBars())
                v.setPadding(
                    barInsets.left,
                    barInsets.top,
                    barInsets.right,
                    barInsets.bottom,
                )
            } else {
                @Suppress("deprecation")
                v.setPadding(
                    insets.systemWindowInsetLeft,
                    insets.systemWindowInsetTop,
                    insets.systemWindowInsetRight,
                    insets.systemWindowInsetBottom,
                )
            }

            insets
        }
    }
}
