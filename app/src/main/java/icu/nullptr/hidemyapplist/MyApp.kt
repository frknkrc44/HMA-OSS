package top.secret.hma.v1

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import top.secret.hma.v1.receiver.AppChangeReceiver
import top.secret.hma.v1.service.ConfigManager
import top.secret.hma.v1.service.PrefManager
import top.secret.hma.v1.service.ServiceClient
import top.secret.hma.v1.ui.util.showToast
import top.secret.hma.v1.util.ConfigUtils.Companion.getLocale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import me.zhanghai.android.appiconloader.AppIconLoader
import top.secret.hma.v1.R
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
