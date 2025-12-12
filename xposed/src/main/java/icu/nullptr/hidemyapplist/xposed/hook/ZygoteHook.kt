package icu.nullptr.hidemyapplist.xposed.hook

import com.github.kyuubiran.ezxhelper.utils.findMethodOrNull
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import de.robv.android.xposed.XC_MethodHook
import icu.nullptr.hidemyapplist.xposed.HMAService
import icu.nullptr.hidemyapplist.xposed.logD

class ZygoteHook(private val service: HMAService): IFrameworkHook {
    companion object {
        private const val TAG = "ZygoteHook"

        private const val ZYGOTE_PROCESS_CLASS = "android.os.ZygoteProcess"
    }

    private val hooks = mutableListOf<XC_MethodHook.Unhook>()

    override fun load() {
        findMethodOrNull(ZYGOTE_PROCESS_CLASS) {
            name == "start"
        }?.hookBefore { param ->
            logD(TAG, "@startZygoteProcess: Starting ${param.args.contentToString()}")

            // ignore if the GIDs array is null
            val gIDsIndex = param.args.indexOfFirst { it is IntArray }
            if (gIDsIndex < 0) return@hookBefore

            val caller = param.args.lastOrNull { it is String } as String? ?: return@hookBefore
            val perms = service.getRestrictedZygotePermissions(caller)
            if (perms.isNotEmpty()) {
                val gIDs = param.args[gIDsIndex] as IntArray

                logD(TAG, "@startZygoteProcess: GIDs are ${gIDs.contentToString()}, removing $perms now")
                param.args[gIDsIndex] = gIDs.filter { it !in perms }.toIntArray()
                service.filterCount++
            }
        }?.let {
            logD(TAG, "Loaded ZygoteProcess start hook!")
            hooks += it
        }
    }

    override fun unload() {
        hooks.forEach(XC_MethodHook.Unhook::unhook)
        hooks.clear()
    }
}
