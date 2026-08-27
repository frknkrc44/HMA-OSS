package org.frknkrc44.hma_oss.zygote.util

import android.content.Intent
import android.content.pm.IPackageManager
import android.content.pm.ResolveInfo
import icu.nullptr.hidemyapplist.common.Utils.conflictedModules
import org.frknkrc44.hma_oss.zygote.util.ContextUtils.packageManager
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.callMethodWithTypes
import rikka.hidden.compat.UserManagerApis

object PackageManagerUtils {
    fun IPackageManager.isConflictingModuleInstalled(): Boolean {
        // we shouldn't apply hooks when the HMA/HMAL detected
        return conflictedModules.any { isPackageAvailable(it, 0) }
    }

    // This part is a copy of Android code
    fun getLaunchIntentForPackageAsUser(packageName: String, userId: Int): Intent? {
        val intentToResolve = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_INFO)
            setPackage(packageName)
        }

        var resolveInfos = queryIntentActivitiesAsUser(intentToResolve, userId)
        if (resolveInfos.isNullOrEmpty()) {
            intentToResolve.apply {
                removeCategory(Intent.CATEGORY_INFO)
                addCategory(Intent.CATEGORY_LAUNCHER)
                setPackage(packageName)
            }

            resolveInfos = queryIntentActivitiesAsUser(intentToResolve, userId)
        }

        return if (resolveInfos.isNullOrEmpty()) {
            null
        } else {
            Intent(intentToResolve).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK

                resolveInfos.first().activityInfo.let {
                    setClassName(it.packageName, it.name)
                }
            }
        }
    }

    // I am lazy to call IPackageManager
    @Suppress("UNCHECKED_CAST")
    private fun queryIntentActivitiesAsUser(intent: Intent, userId: Int) = callMethodWithTypes(
        packageManager,
        "queryIntentActivitiesAsUser",
        arrayOf(
            Intent::class.java,
            Int::class.javaPrimitiveType!!,
            Int::class.javaPrimitiveType!!,
        ),
        arrayOf(intent, /* flags */ 0, userId)
    ) as List<ResolveInfo>?

    fun IPackageManager.findApp(packageName: String) =
        UserManagerApis.getUserIdsNoThrow().any { isPackageAvailable(packageName, it) }
}
