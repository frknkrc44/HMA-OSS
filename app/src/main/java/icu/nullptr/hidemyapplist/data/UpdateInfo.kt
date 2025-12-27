package icu.nullptr.hidemyapplist.data

import kotlinx.coroutines.runBlocking
import java.net.URL
import kotlin.concurrent.thread

import org.json.JSONObject

class UpdateInfo(
    val versionName: String,
    val content: String,
    val downloadUrl: String,
)

fun fetchLatestUpdate(onGetUpdateInfo: suspend (UpdateInfo) -> Unit) {
    thread {
        val jsonText = runCatching {
            URL(AppConstants.UPDATE_CHECK_URL).readText()
        }.getOrNull() ?: return@thread

        val json = JSONObject(jsonText)

        runBlocking {
            onGetUpdateInfo(
                UpdateInfo(
                    versionName = json.getString("tag_name"),
                    content = json.getString("body"),
                    downloadUrl = json.getString("html_url"),
                )
            )
        }
    }
}
