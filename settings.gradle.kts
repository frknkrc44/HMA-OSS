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
}

rootProject.name = "HMA-OSS"

include(
    ":app",
    ":common",
    ":zygote",
)
