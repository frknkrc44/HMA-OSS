package icu.nullptr.hidemyapplist.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import icu.nullptr.hidemyapplist.service.ServiceClient
import icu.nullptr.hidemyapplist.util.PackageHelper

class AppChangeReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AppChangeReceiver"

        private val actions = setOf(
            Intent.ACTION_PACKAGE_ADDED,
            Intent.ACTION_PACKAGE_REMOVED,
            Intent.ACTION_PACKAGE_REPLACED
        )

        fun register(context: Context) {
            val filter = IntentFilter().apply {
                actions.forEach(::addAction)
                addDataScheme("package")
            }
            context.registerReceiver(AppChangeReceiver(), filter)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in actions) {
            ServiceClient.log(Log.INFO, TAG, "Received intent: $intent")
            PackageHelper.invalidateCache()
            // ServiceClient.handlePackageEvent(intent.action, intent.data?.encodedSchemeSpecificPart)
        }
    }
}
