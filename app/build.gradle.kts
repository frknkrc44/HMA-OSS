import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import com.google.gson.JsonParser
import org.jose4j.json.internal.json_simple.JSONObject
import java.io.DataInputStream
import java.net.HttpURLConnection
import java.net.URL

plugins {
    alias(libs.plugins.agp.app)
    alias(libs.plugins.autoresconfig)
    alias(libs.plugins.refine)
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.nav.safeargs.kotlin)
    alias(libs.plugins.naterialthemebuilder)
}

materialThemeBuilder {
    themes {
        for ((name, color) in listOf(
            "Red" to "F44336",
            "Pink" to "E91E63",
            "Purple" to "9C27B0",
            "DeepPurple" to "673AB7",
            "Indigo" to "3F51B5",
            "Blue" to "2196F3",
            "LightBlue" to "03A9F4",
            "Cyan" to "00BCD4",
            "Teal" to "009688",
            "Green" to "4FAF50",
            "LightGreen" to "8BC3A4",
            "Lime" to "CDDC39",
            "Yellow" to "FFEB3B",
            "Amber" to "FFC107",
            "Orange" to "FF9800",
            "DeepOrange" to "FF5722",
            "Brown" to "795548",
            "BlueGrey" to "607D8F",
            "Sakura" to "FF9CA8"
        )) {
            create("Material$name") {
                lightThemeFormat = "ThemeOverlay.Light.%s"
                darkThemeFormat = "ThemeOverlay.Dark.%s"
                primaryColor = "#$color"
            }
        }
    }
    // Add Material Design 3 color tokens (such as palettePrimary100) in generated theme
    // rikka.material >= 2.0.0 provides such attributes
    generatePalette = false
}

val appPackageName: String by rootProject.extra
val crowdinProjectId: String by rootProject.extra
val crowdinApiKey: String by rootProject.extra
val localBuild: Boolean by rootProject.extra
val officialBuild: Boolean by rootProject.extra

@Suppress("deprecation")
afterEvaluate {
    val srcDir = android.sourceSets["main"].assets.srcDirs.first()
    logger.lifecycle("Asset dir: $srcDir")
    if (!srcDir.exists()) srcDir.mkdirs()

    val translatorsMap = mutableMapOf(
        // I used GitHub to get translations before moving to Crowdin.
        // Since nearly all of GitHub translators are listed in Crowdin
        // too, I wanted to add one profile here.

        "cvnertnc" to "https://avatars.githubusercontent.com/u/148134890?v=4",
    )

    val urlConnection = if (crowdinApiKey.isNotBlank()) {
        val url = URL("https://crowdin.com/api/v2/projects/$crowdinProjectId/members")
        (url.openConnection() as HttpURLConnection).apply {
            setRequestProperty("authorization", "Bearer $crowdinApiKey")
        }
    } else {
        val url = URL("https://raw.githubusercontent.com/frknkrc44/HMA-OSS/refs/heads/crowdin/translators.json")
        url.openConnection() as HttpURLConnection
    }

    val inputStream = DataInputStream(urlConnection.getInputStream())
    val str = String(inputStream.readAllBytes())
    inputStream.close()
    urlConnection.disconnect()

    val json = JsonParser.parseString(str).asJsonObject

    if (crowdinApiKey.isNotBlank()) {
        val translators = json.getAsJsonArray("data")

        for (item in translators) {
            val translator = item.asJsonObject.getAsJsonObject("data")
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
    } else {
        json.keySet().forEach { translatorsMap[it] = json.get(it).asString }
    }

    val translatorJson = JSONObject(translatorsMap).toJSONString()
    File(srcDir, "translators.json").writeText(translatorJson)
}

android {
    namespace = appPackageName

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

kotlin {
    jvmToolchain(21)
}

autoResConfig {
    generateClass.set(true)
    generateRes.set(false)
    generatedClassFullName.set("icu.nullptr.hidemyapplist.util.LangList")
    generatedArrayFirstItem.set("SYSTEM")
}

dependencies {
    implementation(projects.common)
    runtimeOnly(projects.xposed)

    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.com.github.bumptech.glide)
    implementation(libs.com.github.kirich1409.viewbindingpropertydelegate)
    implementation(libs.com.github.topjohnwu.libsu.core)
    implementation(libs.dev.rikka.hidden.compat)
    implementation(libs.me.zhanghai.android.appiconloader)
    compileOnly(libs.dev.rikka.hidden.stub)

    implementation(libs.androidx.appcompat.appcompat)
    implementation(libs.material)
}

android.applicationVariants.all {
    outputs.all {
        (this as BaseVariantOutputImpl).apply {
            outputFileName = "${rootProject.name.replace(" ", "_")}-${versionName}-${buildType.name}.apk"
        }
    }
}
