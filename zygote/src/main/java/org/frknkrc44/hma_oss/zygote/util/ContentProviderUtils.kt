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
            )
        }.getOrNull() as? HashSet<String>
    }

    val movedToGlobal by lazy {
        runCatching {
            getStaticObjectField(
                Settings.System::class.java.name,
                "MOVED_TO_GLOBAL",
            )
        }.getOrNull() as? HashSet<String>
    }

    val movedToSecureThenGlobal by lazy {
        runCatching {
            getStaticObjectField(
                Settings.System::class.java.name,
                "MOVED_TO_SECURE_THEN_GLOBAL",
            )
        }.getOrNull() as? HashSet<String>
    }
}

object SettingsSecure {
    val movedToGlobal by lazy {
        runCatching {
            getStaticObjectField(
                Settings.Secure::class.java.name,
                "MOVED_TO_GLOBAL",
            )
        }.getOrNull() as? HashSet<String>
    }
}

object SettingsGlobal {
    val movedToSecure by lazy {
        runCatching {
            getStaticObjectField(
                Settings.Global::class.java.name,
                "MOVED_TO_SECURE",
            )
        }.getOrNull() as? HashSet<String>
    }

    val movedToSystem by lazy {
        runCatching {
            getStaticObjectField(
                Settings.Global::class.java.name,
                "MOVED_TO_SYSTEM",
            )
        }.getOrNull() as? HashSet<String>
    }
}
