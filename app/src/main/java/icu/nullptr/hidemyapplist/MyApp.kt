package icu.nullptr.hidemyapplist

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import icu.nullptr.hidemyapplist.receiver.AppChangeReceiver
import icu.nullptr.hidemyapplist.service.ConfigManager
import icu.nullptr.hidemyapplist.service.PrefManager
import icu.nullptr.hidemyapplist.service.ServiceClient
import icu.nullptr.hidemyapplist.ui.util.showToast
import icu.nullptr.hidemyapplist.util.ConfigUtils.Companion.getLocale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import me.zhanghai.android.appiconloader.AppIconLoader
import org.frknkrc44.hma_oss.R
import kotlin.system.exitProcess

lateinit var hmaApp: MyApp

class MyApp : Application() {
    val globalScope = CoroutineScope(Dispatchers.Default)
    val appIconLoader by lazy {
        val iconSize = resources.getDimensionPixelSize(R.dimen.app_icon_size)
        AppIconLoader(iconSize, false, this)
    }

    @Suppress("DEPRECATION")
    @SuppressLint("SdCardPath")
    override fun onCreate() {
        super.onCreate()
        hmaApp = this
        if (!filesDir.absolutePath.startsWith("/data/user/0/")) {
            showToast(R.string.do_not_dual)
            exitProcess(0)
        }
        AppChangeReceiver.register(this)
        ConfigManager.init()

        AppCompatDelegate.setDefaultNightMode(PrefManager.darkTheme)
        val config = resources.configuration
        config.setLocale(getLocale())
        resources.updateConfiguration(config, resources.displayMetrics)

        val handler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            ServiceClient.log(Log.ERROR, t.name, e.stackTraceToString())
            handler?.uncaughtException(t, e)
        }
    }
}
