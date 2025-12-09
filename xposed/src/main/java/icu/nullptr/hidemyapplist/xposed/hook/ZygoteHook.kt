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

        /**
         * GID that gives write access to app-private data directories on external
         * storage (used on devices without sdcardfs only).
         */
        const val EXT_DATA_RW_GID: Int = 1078

        /**
         * GID that gives write access to app-private OBB directories on external
         * storage (used on devices without sdcardfs only).
         */
        const val EXT_OBB_RW_GID: Int = 1079

        /**
         * Defines the gid shared by all applications running under the same profile.
         */
        const val SHARED_USER_GID: Int = 9997

        val TARGET_GID_LIST = intArrayOf(SHARED_USER_GID, EXT_DATA_RW_GID, EXT_OBB_RW_GID)
    }

    private val hooks = mutableListOf<XC_MethodHook.Unhook>()

    override fun load() {
        findMethodOrNull(ZYGOTE_PROCESS_CLASS) {
            name == "start"
        }?.hookBefore { param ->
            logD(TAG, "@startZygoteProcess: Starting ${param.args.contentToString()}")

            val caller = param.args.lastOrNull { it is String } as String? ?: return@hookBefore
            if (service.shouldRestrictZygotePermissions(caller)) {
                val gIDsIndex = param.args.indexOfFirst { it is IntArray }
                if (gIDsIndex < 0) return@hookBefore
                val gIDs = param.args[gIDsIndex] as IntArray

                logD(TAG, "@startZygoteProcess: GIDs are ${gIDs.contentToString()}, replacing now")
                param.args[gIDsIndex] = gIDs.filter { it !in TARGET_GID_LIST }.toIntArray()
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
