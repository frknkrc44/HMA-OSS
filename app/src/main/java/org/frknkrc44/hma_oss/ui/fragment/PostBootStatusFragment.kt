package org.frknkrc44.hma_oss.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.frknkrc44.hma_oss.R
import org.frknkrc44.hma_oss.databinding.FragmentPostBootStatusBinding
import icu.nullptr.hidemyapplist.ui.util.setupToolbar
import dev.androidbroadcast.vbpd.viewBinding
import java.io.File
import java.io.IOException

/**
 * PostBoot fork — live status widget.
 *
 * Reads /data/local/tmp/hma-oss-postboot.status (which the postboot-bootstrap
 * shell script mirrors from /dev/.hma_oss/status and makes world-readable)
 * and renders it inside the manager. No root privileges are required to
 * display; the underlying status file is populated by the module scripts.
 *
 * The user can press Refresh to reload; a full "activate" is intentionally
 * NOT triggered here — activation is a lifecycle event owned by KernelSU
 * Manager (Soft Reboot). This widget is read-only, matching the safety
 * contract described in docs/POSTBOOT_PORT.md.
 */
class PostBootStatusFragment : Fragment(R.layout.fragment_post_boot_status) {

    private val binding by viewBinding(FragmentPostBootStatusBinding::bind)

    companion object {
        private const val STATUS_PATH = "/data/local/tmp/hma-oss-postboot.status"
        private const val LOG_PATH    = "/data/local/tmp/hma-oss-postboot.log"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupToolbar(
            toolbar = binding.toolbar,
            title = getString(R.string.title_postboot_status),
        )
        binding.refresh.setOnClickListener { reload() }
        reload()
    }

    private fun reload() {
        binding.summary.text = getString(R.string.postboot_loading)
        binding.detail.text = ""
        lifecycleScope.launch {
            val status = withContext(Dispatchers.IO) { readSafely(STATUS_PATH) }
            val log    = withContext(Dispatchers.IO) { readSafely(LOG_PATH) }
            val parsed = parse(status)
            binding.summary.text = summariseFor(parsed)
            binding.detail.text = buildString {
                append("Status file: ")
                append(STATUS_PATH)
                append("\n\n")
                append(status.ifBlank { "(not present)" })
                if (log.isNotBlank()) {
                    append("\n\n---- Log ----\n")
                    append(log)
                }
            }
        }
    }

    private fun readSafely(path: String): String {
        return try {
            val f = File(path)
            if (!f.exists() || !f.canRead()) "" else f.readText()
        } catch (_: IOException) {
            ""
        } catch (_: SecurityException) {
            ""
        }
    }

    private fun parse(raw: String): Map<String, String> {
        if (raw.isBlank()) return emptyMap()
        val out = LinkedHashMap<String, String>()
        raw.lineSequence().forEach { line ->
            val idx = line.indexOf('=')
            if (idx > 0) out[line.substring(0, idx).trim()] = line.substring(idx + 1).trim()
        }
        return out
    }

    private fun summariseFor(map: Map<String, String>): String {
        val result = map["RESULT"] ?: return getString(R.string.postboot_state_unknown)
        val res = getString(when (result) {
            "HEALTHY_INJECTED"                 -> R.string.postboot_state_healthy
            "WAITING_FOR_KERNELSU_SOFT_REBOOT" -> R.string.postboot_state_waiting_softreboot
            "WAITING_FOR_ZYGISK_PROVIDER"      -> R.string.postboot_state_waiting_provider
            "NO_ZYGISK_PROVIDER"               -> R.string.postboot_state_no_provider
            "MODULE_DISABLED"                  -> R.string.postboot_state_disabled
            "MISSING_ZYGISK_LIB"               -> R.string.postboot_state_missing_lib
            "NOT_INSTALLED"                    -> R.string.postboot_state_not_installed
            "BUSY"                             -> R.string.postboot_state_busy
            "FAILED"                           -> R.string.postboot_state_failed
            else                               -> R.string.postboot_state_unknown
        })
        val extras = listOfNotNull(
            map["PROVIDER"]?.let { "Provider: $it" },
            map["ZYGOTE_PID"]?.let { "Zygote: $it" },
            map["SYSTEM_SERVER_PID"]?.let { "system_server: $it" },
            map["ZYGOTE_LIB_MAPPED"]?.let { "Zygote lib mapped: $it" },
            map["SYSTEM_SERVER_LIB_MAPPED"]?.let { "system_server lib mapped: $it" },
        )
        return if (extras.isEmpty()) res else res + "\n\n" + extras.joinToString("\n")
    }
}
