import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.jose4j.json.internal.json_simple.JSONObject
import java.net.HttpURLConnection
import java.net.URL

plugins {
    alias(libs.plugins.agp.app)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.nav.safeargs.kotlin)
}

abstract class GenerateTranslatorsTask : DefaultTask() {
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Internal
    abstract val crowdinProjectId: Property<String>

    @get:Internal
    abstract val crowdinApiKey: Property<String>

    @TaskAction
    fun run() {
        val translatorsMap = mutableMapOf(
            // I used GitHub to get translations before moving to Crowdin.
            // Since nearly all of GitHub translators are listed in Crowdin
            // too, I wanted to add one profile here.

            "cvnertnc" to "https://avatars.githubusercontent.com/u/148134890?v=4",
        )

        val apiKey = crowdinApiKey.get()
        val projectId = crowdinProjectId.get()
        @Suppress("DEPRECATION")
        val urlConnection = if (apiKey.isNotBlank()) {
            logger.lifecycle("Found Crowdin API key")

            val url = URL("https://crowdin.com/api/v2/projects/$projectId/members")
            (url.openConnection() as HttpURLConnection).apply {
                setRequestProperty("authorization", "Bearer $apiKey")
            }
        } else {
            val url = URL("https://github.com/frknkrc44/HMA-OSS/releases/latest/download/translators.json")
            url.openConnection() as HttpURLConnection
        }

        // add timeouts
        urlConnection.readTimeout = 5_000
        urlConnection.connectTimeout = 5_000

        val str = runCatching {
            urlConnection.getInputStream().bufferedReader(Charsets.UTF_8).use { it.readText() }
        }.getOrElse { "{}" }
        urlConnection.disconnect()

        val json = runCatching {
            JsonParser.parseString(str).asJsonObject
        }.getOrElse {
            JsonObject()
        }

        if (apiKey.isNotBlank()) {
            val translators = json.getAsJsonArray("data")

            if (translators != null) {
                for (item in translators) {
                    val translator = runCatching {
                        item.asJsonObject.getAsJsonObject("data")
                    }.getOrNull()
                    if (translator == null) continue
                    val avatarUrl = translator.get("avatarUrl").asString
                    val username = translator.get("username").asString
                    val fullName = try {
                        translator.get("fullName").asString
                    } catch (_: Throwable) {
                        ""
                    }

                    if (fullName.isNotEmpty() && fullName != username) {
                        translatorsMap["$fullName ($username)"] = avatarUrl
                    } else {
                        translatorsMap[username] = avatarUrl
                    }
                }
            }
        } else {
            json.keySet().forEach { translatorsMap[it] = json.get(it).asString }
        }

        val translatorJson = JSONObject(translatorsMap).toJSONString()
        val outFile = outputDir.get().file("translators.json").asFile
        outFile.parentFile.mkdirs()
        outFile.writeText(translatorJson)
    }
}

val generateTranslators = tasks.register<GenerateTranslatorsTask>("generateTranslators") {
    description = "Generate translators JSON"
    crowdinProjectId.set(rootProject.extra["crowdinProjectId"] as String)
    crowdinApiKey.set(rootProject.extra["crowdinApiKey"] as String)
}

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(
            generateTranslators,
            GenerateTranslatorsTask::outputDir,
        )
    }
}

android {
    namespace = rootProject.extra["appPackageName"] as String

    defaultConfig {
        buildConfigField("String[]", "SUPPORTED_LOCALES", generateSupportedLocales())
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    packaging {
        dex.useLegacyPackaging = true
        resources {
            excludes += arrayOf(
                "/META-INF/*",
                "/META-INF/androidx/**",
                "/kotlin/**",
                "/okhttp3/**",
            )
        }
    }
}

base {
    archivesName = "${rootProject.name}-${android.defaultConfig.versionName!!.replace("/", "_")}"
}

kotlin {
    jvmToolchain(21)
}

// Inspired from https://github.com/XayahSuSuSu/Android-DataBackup/pull/260
fun generateSupportedLocales(): String {
    val foundLocales = StringBuilder()
    foundLocales.append("new String[]{")

    fun appendLangCode(code: String) {
        foundLocales.append("\"").append(code).append("\"").append(",")
    }

    appendLangCode("SYSTEM")

    fileTree(android.sourceSets["main"].res.directories.first()).files.mapNotNull {
        if (it.name == "strings.xml") {
            val baseName = it.parent.substringAfterLast(File.separator)
            if (baseName == "values") {
                "en"
            } else {
                baseName.substringAfter('-')
                    .replace("-r", "-")
            }
        } else {
            null
        }
    }.sortedWith { file1, file2 ->
        file1.compareTo(file2)
    }.forEach { appendLangCode(it) }

    return "${foundLocales.removeSuffix(",")}}"
}

dependencies {
    implementation(projects.common)

    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.io.coilkt.coil3.coil)
    implementation(libs.io.coilkt.coil3.coil.network.okhttp)
    implementation(libs.dev.androidbroadcast.vbpd)
    implementation(libs.dev.androidbroadcast.vbpd.reflection)

    implementation(libs.androidx.appcompat.appcompat)
    implementation(libs.material)
}
