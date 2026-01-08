plugins {
    alias(libs.plugins.agp.lib)
    alias(libs.plugins.refine)
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlin.serialization)
}

val configVerCode: Int by rootProject.extra
val serviceVerCode: Int by rootProject.extra
val minBackupVerCode: Int by rootProject.extra
val appPackageName: String by rootProject.extra
val appVerName: String by rootProject.extra
val appVerCode: Int by rootProject.extra

android {
    namespace = "$appPackageName.common"

    buildFeatures {
        aidl = true
        buildConfig = true
    }

    defaultConfig {
        buildConfigField("int", "CONFIG_VERSION", configVerCode.toString())
        buildConfigField("int", "SERVICE_VERSION", serviceVerCode.toString())
        buildConfigField("int", "MIN_BACKUP_VERSION", minBackupVerCode.toString())
        buildConfigField("String", "APP_PACKAGE_NAME", "\"$appPackageName\"")
        buildConfigField("String", "APP_VERSION_NAME", "\"$appVerName\"")
        buildConfigField("int", "APP_VERSION_CODE", appVerCode.toString())
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    api(libs.kotlinx.serialization.json)
    compileOnly(libs.dev.rikka.hidden.stub)
}
