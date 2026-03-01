package top.secret.hma.v1.xposed

import android.app.ActivityThread
import android.os.Binder
import android.os.Build
import com.github.kyuubiran.ezxhelper.utils.findField
import top.secret.hma.v1.common.Constants
import top.secret.hma.v1.common.Utils

object Utils4Xposed {
    fun getPackageNameFromPackageSettings(packageSettings: Any): String? {
        return runCatching {
            findField(packageSettings::class.java, true) {
                name == if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) "mName" else "name"
            }.get(packageSettings) as? String
        }.getOrNull()
    }

    fun getPackageManager() = ActivityThread.currentActivityThread().application.packageManager!!

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
