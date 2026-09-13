package icu.nullptr.hidemyapplist

import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import icu.nullptr.hidemyapplist.receiver.AppChangeReceiver
import icu.nullptr.hidemyapplist.service.ConfigManager
import icu.nullptr.hidemyapplist.service.PrefManager
import icu.nullptr.hidemyapplist.service.ServiceClient
import icu.nullptr.hidemyapplist.util.ConfigUtils.Companion.getLocale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class MyApp : Application() {
    companion object {
        lateinit var hmaApp: MyApp
    }

    val globalScope = CoroutineScope(Dispatchers.Default)
    var updateDialogSkipped: Boolean = false

    @Suppress("DEPRECATION")
    fun loadConfiguration() {
        if (ServiceClient.serviceVersion > 0) {
            ConfigManager.init()

            AppCompatDelegate.setDefaultNightMode(PrefManager.darkTheme)
            val config = resources.configuration
            config.setLocale(getLocale())
            resources.updateConfiguration(config, resources.displayMetrics)
        }
    }

    override fun onCreate() {
        super.onCreate()
        hmaApp = this
        AppChangeReceiver.register(this)

        val handler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            ServiceClient.log(Log.ERROR, t.name, e.stackTraceToString())
            handler?.uncaughtException(t, e)
        }
    }
}
