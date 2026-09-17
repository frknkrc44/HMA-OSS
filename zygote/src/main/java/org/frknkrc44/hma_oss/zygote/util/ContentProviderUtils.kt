@file:Suppress("UNCHECKED_CAST")

package org.frknkrc44.hma_oss.zygote.util

import android.provider.Settings
import icu.nullptr.hidemyapplist.common.Constants.SETTINGS_GLOBAL
import icu.nullptr.hidemyapplist.common.Constants.SETTINGS_SECURE
import icu.nullptr.hidemyapplist.common.Constants.SETTINGS_SYSTEM
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.getStaticObjectField

object ContentProviderUtils {
    fun getOverriddenDatabaseName(database: String, name: String): String {
        if (SettingsSecure.movedToGlobal?.contains(name) ?: false ||
            SettingsSystem.movedToGlobal?.contains(name) ?: false ||
            SettingsSystem.movedToSecureThenGlobal?.contains(name) ?: false) {
            return SETTINGS_GLOBAL
        }

        if (SettingsGlobal.movedToSecure?.contains(name) ?: false ||
            SettingsSystem.movedToSecure?.contains(name) ?: false) {
            return SETTINGS_SECURE
        }

        if (SettingsGlobal.movedToSystem?.contains(name) ?: false) {
            return SETTINGS_SYSTEM
        }

        return database
    }

    private object SettingsSystem {
        val movedToSecure by lazy {
            runCatching {
                getStaticObjectField(
                    Settings.System::class.java.name,
                    "MOVED_TO_SECURE",
                ) as? HashSet<String>
            }.getOrNull()
        }

        val movedToGlobal by lazy {
            runCatching {
                getStaticObjectField(
                    Settings.System::class.java.name,
                    "MOVED_TO_GLOBAL",
                ) as? HashSet<String>
            }.getOrNull()
        }

        val movedToSecureThenGlobal by lazy {
            runCatching {
                getStaticObjectField(
                    Settings.System::class.java.name,
                    "MOVED_TO_SECURE_THEN_GLOBAL",
                ) as? HashSet<String>
            }.getOrNull()
        }
    }

    private object SettingsSecure {
        val movedToGlobal by lazy {
            runCatching {
                getStaticObjectField(
                    Settings.Secure::class.java.name,
                    "MOVED_TO_GLOBAL",
                ) as? HashSet<String>
            }.getOrNull()
        }
    }

    private object SettingsGlobal {
        val movedToSecure by lazy {
            runCatching {
                getStaticObjectField(
                    Settings.Global::class.java.name,
                    "MOVED_TO_SECURE",
                ) as? HashSet<String>
            }.getOrNull()
        }

        val movedToSystem by lazy {
            runCatching {
                getStaticObjectField(
                    Settings.Global::class.java.name,
                    "MOVED_TO_SYSTEM",
                ) as? HashSet<String>
            }.getOrNull()
        }
    }
}
