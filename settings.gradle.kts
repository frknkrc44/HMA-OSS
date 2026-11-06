import kotlin.io.path.Path

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        mavenLocal()
        maven("https://jitpack.io")
        maven("https://mirrors.cloud.tencent.com/nexus/repository/maven-public")
        maven("https://maven.aliyun.com/repository/public")
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        mavenLocal()
        maven("https://jitpack.io")
        maven("https://mirrors.cloud.tencent.com/nexus/repository/maven-public")
        maven("https://maven.aliyun.com/repository/public")
    }

    versionCatalogs {
        create("androidvmtools") {
            from(files(
                Path(rootDir.path, "external", "AndroidVMTools", "gradle", "libs.versions.toml")))
        }
    }
}

rootProject.name = "HMA-OSS"

include(
    ":app",
    ":common",
    ":stub",
    ":zygote",
)
