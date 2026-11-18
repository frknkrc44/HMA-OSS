package icu.nullptr.hidemyapplist.util

import androidx.core.net.toUri
import icu.nullptr.hidemyapplist.MyApp.Companion.hmaApp

object WikiUtils {
    private const val WIKI_MAIN_PAGE = 0
    private const val WIKI_FILTER_CATEGORIES = 1
    private const val WIKI_BULK_CONFIG_WIZARD = 2

    val mainPageURL get() = getLink(WIKI_MAIN_PAGE).toUri()
    val filterCategoriesURL get() = getLink(WIKI_FILTER_CATEGORIES).toUri()
    val bulkConfigWizardURL get() = getLink(WIKI_BULK_CONFIG_WIZARD).toUri()

    private val wikiUrls = mapOf(
        "en" to mapOf(
            WIKI_MAIN_PAGE to "https://github.com/frknkrc44/HMA-OSS/wiki/About-HMA%E2%80%90OSS",
            WIKI_FILTER_CATEGORIES to "https://github.com/frknkrc44/HMA-OSS/wiki/About-HMA%E2%80%90OSS#filter-logs-categories",
            WIKI_BULK_CONFIG_WIZARD to "https://github.com/frknkrc44/HMA-OSS/wiki/About-HMA%E2%80%90OSS#bulk-config-wizard",
        ),
        "fr" to mapOf(
            WIKI_MAIN_PAGE to "https://github.com/frknkrc44/HMA-OSS/wiki/%C3%80-propos-de-HMA%E2%80%90OSS",
            WIKI_FILTER_CATEGORIES to "https://github.com/frknkrc44/HMA-OSS/wiki/%C3%80-propos-de-HMA%E2%80%90OSS#filtrer-les-cat%C3%A9gories-de-journaux",
            WIKI_BULK_CONFIG_WIZARD to "https://github.com/frknkrc44/HMA-OSS/wiki/%C3%80-propos-de-HMA%E2%80%90OSS#assistant-de-configuration-group%C3%A9e",
        ),
        // TODO: Add more languages
    )

    @Suppress("DEPRECATION")
    private val linksForLanguage get(): Map<Int, String> {
        val locale = hmaApp.resources.configuration.locale
        return wikiUrls[locale.language.lowercase()] ?: wikiUrls["en"]!!
    }

    private fun getLink(purpose: Int) = linksForLanguage[purpose] ?: linksForLanguage[WIKI_MAIN_PAGE]!!
}
