package top.secret.hma.v1.util

import android.content.res.Resources
import top.secret.hma.v1.service.PrefManager
import java.util.Locale

class ConfigUtils private constructor() {
    companion object {
        fun getSystemLocale(): Locale = Resources.getSystem().configuration.getLocales().get(0)

        fun getLocale(): Locale {
            val tag = PrefManager.locale
            return if (tag == "SYSTEM") getSystemLocale()
            else Locale.forLanguageTag(tag)
        }
    }
}
