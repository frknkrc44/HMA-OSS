package icu.nullptr.hidemyapplist.xposed

import android.os.Binder
import android.os.Build
import com.github.kyuubiran.ezxhelper.utils.findField
import icu.nullptr.hidemyapplist.common.Constants
import icu.nullptr.hidemyapplist.common.Utils

class Utils4Xposed {
    companion object {
        fun getPackageNameFromPackageSettings(packageSettings: Any): String? {
            return runCatching {
                findField(packageSettings::class.java, true) {
                    name == if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) "mName" else "name"
                }.get(packageSettings) as? String
            }.getOrNull()
        }

        fun getCallingApps(service: HMAService): Array<String> {
            return getCallingApps(service, Binder.getCallingUid())
        }

        fun getCallingApps(service: HMAService, callingUid: Int): Array<String> {
            if (callingUid == Constants.UID_SYSTEM) return arrayOf()
            return Utils.binderLocalScope {
                service.pms.getPackagesForUid(callingUid)
            } ?: arrayOf()
        }
    }
}
