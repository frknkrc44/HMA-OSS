package icu.nullptr.hidemyapplist.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toDrawable
import icu.nullptr.hidemyapplist.common.Constants
import icu.nullptr.hidemyapplist.hmaApp
import icu.nullptr.hidemyapplist.service.ConfigManager
import icu.nullptr.hidemyapplist.service.PrefManager
import icu.nullptr.hidemyapplist.service.ServiceClient
import icu.nullptr.hidemyapplist.ui.util.ThemeUtils.asDrawable
import icu.nullptr.hidemyapplist.ui.util.asComponentName
import icu.nullptr.hidemyapplist.ui.util.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.frknkrc44.hma_oss.BuildConfig
import org.frknkrc44.hma_oss.R
import java.text.Collator
import java.util.Locale

object PackageHelper {
    const val TAG = "PackageHelper"

    class PackageCache(
        val info: PackageInfo,
        val label: String,
        val icon: Drawable,
        val userId: Int,
    )

    object Comparators {
        val byLabel = Comparator<String> { o1, o2 ->
            try {
                val n1 = loadAppLabel(o1).lowercase(Locale.getDefault())
                val n2 = loadAppLabel(o2).lowercase(Locale.getDefault())
                Collator.getInstance(Locale.getDefault()).compare(n1, n2)
            } catch (_: Throwable) {
                byPackageName.compare(o1, o2)
            }
        }
        val byPackageName = Comparator<String> { o1, o2 ->
            val n1 = o1.lowercase(Locale.getDefault())
            val n2 = o2.lowercase(Locale.getDefault())
            Collator.getInstance(Locale.getDefault()).compare(n1, n2)
        }
        val byInstallTime = Comparator<String> { o1, o2 ->
            try {
                val n1 = loadPackageInfo(o1).firstInstallTime
                val n2 = loadPackageInfo(o2).firstInstallTime
                n2.compareTo(n1)
            } catch (_: Throwable) {
                byPackageName.compare(o1, o2)
            }
        }
        val byUpdateTime = Comparator<String> { o1, o2 ->
            try {
                val n1 = loadPackageInfo(o1).lastUpdateTime
                val n2 = loadPackageInfo(o2).lastUpdateTime
                n2.compareTo(n1)
            } catch (_: Throwable) {
                byPackageName.compare(o1, o2)
            }
        }
    }

    private val packageCache = MutableSharedFlow<Map<String, PackageCache>>(replay = 1)
    val appList = MutableSharedFlow<List<String>>(replay = 1)

    val isRefreshing = MutableSharedFlow<Boolean>(replay = 1)

    val refreshing get() = isRefreshing.replayCache.isEmpty() || isRefreshing.get()

    init {
        invalidateCache()
    }

    fun invalidateCache(
        onFinished: ((Throwable?) -> Unit)? = null
    ) {
        hmaApp.globalScope.launch {
            isRefreshing.emit(true)
            val cache = withContext(Dispatchers.IO) {
                val pm = hmaApp.packageManager
                val um = hmaApp.getSystemService(Context.USER_SERVICE) as UserManager
                val profiles = um.userProfiles

                if (ConfigManager.packageQueryWorkaround) {
                    mutableMapOf<String, PackageCache>().also { cacheMap ->
                        for (userProfile: UserHandle in profiles) {
                            val packages = ServiceClient.getPackageNames(userProfile.hashCode()) ?: arrayOf<String>()
                            for (packageName in packages) {
                                val packageInfo = ServiceClient.getPackageInfo(packageName, userProfile.hashCode())!!
                                if (packageInfo.packageName in Constants.packagesShouldNotHide) continue
                                packageInfo.applicationInfo?.let { appInfo ->
                                    val label = pm.getApplicationLabel(appInfo).toString()
                                    val icon = loadAppIconFromAppInfo(appInfo)
                                    if (!cacheMap.containsKey(packageInfo.packageName)) {
                                        cacheMap[packageInfo.packageName] = PackageCache(packageInfo, label, icon, userProfile.hashCode())
                                    }
                                }
                            }
                        }
                    }
                } else {
                    mutableMapOf<String, PackageCache>().also { cacheMap ->
                        for (userProfile: UserHandle in profiles) {
                            val packages = getInstalledPackagesAsUser(pm, userProfile.hashCode())
                            for (packageInfo in packages) {
                                if (packageInfo.packageName in Constants.packagesShouldNotHide) continue
                                packageInfo.applicationInfo?.let { appInfo ->
                                    val label = pm.getApplicationLabel(appInfo).toString()
                                    val icon = loadAppIconFromAppInfo(appInfo)
                                    if (!cacheMap.containsKey(packageInfo.packageName)) {
                                        cacheMap[packageInfo.packageName] = PackageCache(packageInfo, label, icon, userProfile.hashCode())
                                    }
                                }
                            }
                        }
                    }
                }
            }
            packageCache.emit(cache)
            appList.emit(cache.keys.toList())
            isRefreshing.emit(false)
        }.apply {
            if (onFinished != null) {
                invokeOnCompletion(onFinished)
            }
        }
    }

    suspend fun sortList(firstComparator: Comparator<String>) {
        var comparator = when (PrefManager.appFilter_sortMethod) {
            PrefManager.SortMethod.BY_LABEL -> Comparators.byLabel
            PrefManager.SortMethod.BY_PACKAGE_NAME -> Comparators.byPackageName
            PrefManager.SortMethod.BY_INSTALL_TIME -> Comparators.byInstallTime
            PrefManager.SortMethod.BY_UPDATE_TIME -> Comparators.byUpdateTime
        }
        if (PrefManager.appFilter_reverseOrder) comparator = comparator.reversed()
        val list = appList.first().sortedWith(firstComparator.then(comparator))
        appList.emit(list)
    }

    private suspend fun getCacheNoThrow() = try {
        packageCache.first()
    } catch (_: Throwable) {
        mapOf()
    }

    fun exists(packageName: String) = runBlocking {
        getCacheNoThrow().contains(packageName)
    }

    fun loadPackageInfo(packageName: String): PackageInfo = runBlocking {
        getCacheNoThrow()[packageName]!!.info
    }

    fun loadAppLabel(packageName: String): String = runBlocking {
        getCacheNoThrow()[packageName]?.label ?: packageName
    }

    fun loadAppIcon(packageName: String): Drawable = runBlocking {
        getCacheNoThrow()[packageName]?.icon ?:
            android.R.drawable.sym_def_app_icon.asDrawable(hmaApp)
    }

    fun loadUserId(packageName: String): Int = runBlocking {
        getCacheNoThrow()[packageName]?.userId ?: 0
    }

    fun isSystem(packageName: String): Boolean = runBlocking {
        getCacheNoThrow()[packageName]?.info?.applicationInfo?.flags?.and(ApplicationInfo.FLAG_SYSTEM) != 0
    }

    fun loadAppIconFromAppInfo(appInfo: ApplicationInfo): Drawable {
        return if (appInfo.packageName == BuildConfig.APPLICATION_ID) {
            val activityName = findEnabledAppComponent(hmaApp)
            if (activityName == null) {
                R.mipmap.ic_launcher.asDrawable(hmaApp)
            } else {
                hmaApp.packageManager.getActivityIcon(activityName)
            }
        } else {
            try {
                hmaApp.appIconLoader.loadIcon(appInfo).toDrawable(hmaApp.resources)
            } catch (e: Throwable) {
                ServiceClient.log(Log.ERROR, TAG, e.stackTraceToString())

                try {
                    appInfo.loadIcon(hmaApp.packageManager)
                } catch (x: Throwable) {
                    ServiceClient.log(Log.ERROR, TAG, x.stackTraceToString())

                    return ResourcesCompat.getDrawable(
                        hmaApp.resources,
                        android.R.drawable.sym_def_app_icon,
                        hmaApp.theme,
                    )!!
                }
            }
        }
    }

    fun findEnabledAppComponent(context: Context): ComponentName? {
        with (context.packageManager) {
            val pkgInfo =  getPackageInfo(BuildConfig.APPLICATION_ID, PackageManager.GET_ACTIVITIES)!!

            return pkgInfo.activities?.firstOrNull { it.targetActivity != null }?.asComponentName()
        }
    }

    fun getInstalledPackagesAsUser(pm: PackageManager, userId: Int): List<PackageInfo> {
        return if (userId == 0) {
            pm.getInstalledPackages(0)
        } else {
            val packages = ServiceClient.getPackageNames(userId) ?: arrayOf<String>()
            packages.mapNotNull { ServiceClient.getPackageInfo(it, userId) }
        }
    }
}
