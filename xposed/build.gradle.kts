plugins {
    alias(libs.plugins.agp.lib)
    alias(libs.plugins.refine)
    alias(libs.plugins.kotlin)
}

val appPackageName: String by rootProject.extra

android {
    namespace = "$appPackageName.xposed"

    buildFeatures {
        buildConfig = false
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(projects.common)

    implementation(libs.androidx.annotation.jvm)
    implementation(libs.com.github.kyuubiran.ezxhelper)
    implementation(libs.dev.rikka.hidden.compat)
    compileOnly(libs.de.robv.android.xposed.api)
    compileOnly(libs.dev.rikka.hidden.stub)
}
