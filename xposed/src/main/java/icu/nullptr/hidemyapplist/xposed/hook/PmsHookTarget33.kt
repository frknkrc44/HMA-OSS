package icu.nullptr.hidemyapplist.xposed.hook

import android.content.pm.PackageInstaller
import android.os.Build
import androidx.annotation.RequiresApi
import com.github.kyuubiran.ezxhelper.utils.findConstructor
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.github.kyuubiran.ezxhelper.utils.paramCount
import icu.nullptr.hidemyapplist.common.Constants.VENDING_PACKAGE_NAME
import icu.nullptr.hidemyapplist.common.Utils
import icu.nullptr.hidemyapplist.xposed.HMAService
import icu.nullptr.hidemyapplist.xposed.Logcat.logI
import icu.nullptr.hidemyapplist.xposed.Utils4Xposed.getPackageNameFromPackageSettings
import icu.nullptr.hidemyapplist.xposed.XposedConstants.APPS_FILTER_IMPL_CLASS

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class PmsHookTarget33(service: HMAService) : PmsHookTargetBase(service) {

    override val TAG = "PmsHookTarget33"

    private val getPackagesForUidMethod by lazy {
        findMethod("com.android.server.pm.Computer") {
            name == "getPackagesForUid"
        }
    }

    override val fakeSystemPackageInstallSourceInfo: Any by lazy {
        findConstructor(
            "android.content.pm.InstallSourceInfo"
        ) {
            paramCount == 5
        }.newInstance(
            null,
            null,
            null,
            null,
            PackageInstaller.PACKAGE_SOURCE_UNSPECIFIED,
        )
    }

    override val fakeUserPackageInstallSourceInfo: Any by lazy {
        findConstructor(
            "android.content.pm.InstallSourceInfo"
        ) {
            paramCount == 5
        }.newInstance(
            VENDING_PACKAGE_NAME,
            psPackageInfo?.signingInfo,
            VENDING_PACKAGE_NAME,
            VENDING_PACKAGE_NAME,
            PackageInstaller.PACKAGE_SOURCE_STORE,
        )
    }

    @Suppress("UNCHECKED_CAST")
    override fun load() {
        logI(TAG) { "Load hook" }

        hooks += findMethod(APPS_FILTER_IMPL_CLASS, findSuper = true) {
            name == "shouldFilterApplication"
        }.hookBefore { param ->
            applyPackageHiding(
                param.method.name,
                { param.args[1] as Int? },
                { getPackageNameFromPackageSettings(param.args[3]) },
                {
                    Utils.binderLocalScope {
                        getPackagesForUidMethod.invoke(param.args[0], it) as Array<String>?
                    }
                },
                { param.result = true },
            )
        }

        super.load()
    }
}
