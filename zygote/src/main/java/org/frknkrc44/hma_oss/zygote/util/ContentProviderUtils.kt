@file:Suppress("UNCHECKED_CAST")

package org.frknkrc44.hma_oss.zygote.util

import android.provider.Settings
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.getStaticObjectField

object SettingsSystem {
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

object SettingsSecure {
    val movedToGlobal by lazy {
        runCatching {
            getStaticObjectField(
                Settings.Secure::class.java.name,
                "MOVED_TO_GLOBAL",
            ) as? HashSet<String>
        }.getOrNull()
    }
}

object SettingsGlobal {
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
