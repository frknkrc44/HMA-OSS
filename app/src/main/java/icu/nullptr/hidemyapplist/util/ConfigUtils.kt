package icu.nullptr.hidemyapplist.util

import android.content.res.Resources
import icu.nullptr.hidemyapplist.service.PrefManager
import java.util.Locale

object ConfigUtils {
    fun getSystemLocale(): Locale = Resources.getSystem().configuration.locales.get(0)

    fun getLocale(tag: String = PrefManager.locale): Locale {
        return if (tag == "SYSTEM") getSystemLocale()
        else Locale.forLanguageTag(tag)
    }
}
