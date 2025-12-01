package icu.nullptr.hidemyapplist.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toDrawable
import icu.nullptr.hidemyapplist.common.Constants
import icu.nullptr.hidemyapplist.hmaApp
import icu.nullptr.hidemyapplist.service.ConfigManager
import icu.nullptr.hidemyapplist.service.PrefManager
import icu.nullptr.hidemyapplist.service.ServiceClient
import icu.nullptr.hidemyapplist.ui.util.ThemeUtils.asDrawable
import icu.nullptr.hidemyapplist.ui.util.asComponentName
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
    class PackageCache(
        val info: PackageInfo,
        val label: String,
        val icon: Drawable
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

                if (ConfigManager.packageQueryWorkaround) {
                    val packages = ServiceClient.getPackageNames(0) ?: arrayOf<String>()
                    mutableMapOf<String, PackageCache>().also {
                        for (packageName in packages) {
                            val packageInfo = ServiceClient.getPackageInfo(packageName, 0)!!
                            if (packageInfo.packageName in Constants.packagesShouldNotHide) continue
                            packageInfo.applicationInfo?.let { appInfo ->
                                val label = pm.getApplicationLabel(appInfo).toString()
                                val icon = loadAppIconFromAppInfo(appInfo)
                                it[packageInfo.packageName] = PackageCache(packageInfo, label, icon)
                            }
                        }
                    }
                } else {
                    val packages = pm.getInstalledPackages(0)
                    mutableMapOf<String, PackageCache>().also {
                        for (packageInfo in packages) {
                            if (packageInfo.packageName in Constants.packagesShouldNotHide) continue
                            packageInfo.applicationInfo?.let { appInfo ->
                                val label = pm.getApplicationLabel(appInfo).toString()
                                val icon = loadAppIconFromAppInfo(appInfo)
                                it[packageInfo.packageName] = PackageCache(packageInfo, label, icon)
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
            hmaApp.appIconLoader.loadIcon(appInfo).toDrawable(hmaApp.resources)
        }
    }

    fun findEnabledAppComponent(context: Context): ComponentName? {
        with (context.packageManager) {
            val pkgInfo =  getPackageInfo(BuildConfig.APPLICATION_ID, PackageManager.GET_ACTIVITIES)!!

            return pkgInfo.activities?.firstOrNull { it.targetActivity != null }?.asComponentName()
        }
    }
}
