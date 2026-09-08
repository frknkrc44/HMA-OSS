@file:Suppress("UNCHECKED_CAST")

package org.frknkrc44.hma_oss.zygote.util

import android.provider.Settings
import icu.nullptr.hidemyapplist.common.Constants.SETTINGS_GLOBAL
import icu.nullptr.hidemyapplist.common.Constants.SETTINGS_SECURE
import icu.nullptr.hidemyapplist.common.Constants.SETTINGS_SYSTEM
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.getStaticObjectField

object ContentProviderUtils {
    fun getOverriddenDatabaseName(database: String, name: String): String {
        when (database) {
            SETTINGS_GLOBAL -> {
                if (SettingsGlobal.movedToSecure?.contains(name) ?: false) {
                    return SETTINGS_SECURE
                } else if (SettingsGlobal.movedToSystem?.contains(name) ?: false) {
                    return SETTINGS_SYSTEM
                }
            }
            SETTINGS_SECURE -> {
                if (SettingsSecure.movedToGlobal?.contains(name) ?: false) {
                    return SETTINGS_GLOBAL
                }
            }
            SETTINGS_SYSTEM -> {
                if (SettingsSystem.movedToSecure?.contains(name) ?: false) {
                    return SETTINGS_SECURE
                } else if (SettingsSystem.movedToGlobal?.contains(name) ?: false ||
                    SettingsSystem.movedToSecureThenGlobal?.contains(name) ?: false) {
                    return SETTINGS_GLOBAL
                }
            }
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
