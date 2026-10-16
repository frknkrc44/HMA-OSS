plugins {
    alias(libs.plugins.agp.lib)
    alias(libs.plugins.kotlin.serialization)
}

val configVerCode =  rootProject.extra["configVerCode"] as Int
val serviceVerCode = rootProject.extra["serviceVerCode"] as Int
val minBackupVerCode = rootProject.extra["minBackupVerCode"] as Int
val appPackageName = rootProject.extra["appPackageName"] as String
val appVerName = rootProject.extra["appVerName"] as String
val appVerCode = rootProject.extra["appVerCode"] as Int

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

    compileOnly(projects.stub)
}
