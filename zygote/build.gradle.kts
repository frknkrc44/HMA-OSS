import com.android.ide.common.signing.KeystoreHelper
import com.v7878.zygisk.gradle.ZygoteLoader
import java.util.Locale
import kotlin.io.path.Path

plugins {
    alias(libs.plugins.agp.app)
    alias(libs.plugins.com.github.aerathstuff.zygoteloader)
}

val appPackageName = rootProject.extra["appPackageName"] as String
val appVerName = rootProject.extra["appVerName"] as String

android {
    namespace = "$appPackageName.zygote"

    defaultConfig {
        applicationId = namespace
    }

    sourceSets {
        getByName("main") {
            java {
                directories.add(
                    Path(rootDir.path, "external", "AndroidVMTools", "src", "main", "java").toString()
                )
            }
        }
    }
}

kotlin {
    jvmToolchain(21)
}

abstract class CopyManagerAppTask : DefaultTask() {

    @get:InputFile
    abstract val managerApk: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun copyManagerApp() {
        managerApk.get().asFile.copyTo(
            outputDir.get().file("manager.apk").asFile,
            overwrite = true,
        )
    }
}

abstract class GenerateSignInfoTask : DefaultTask() {

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Input
    @get:Optional
    abstract val storeType: Property<String>

    @get:InputFile
    @get:Optional
    abstract val storeFile: RegularFileProperty

    @get:Input
    @get:Optional
    abstract val keyAlias: Property<String>

    @get:Internal
    abstract val storePassword: Property<String>

    @get:Internal
    abstract val keyPassword: Property<String>

    @TaskAction
    fun generate() {
        val certificateInfo = KeystoreHelper.getCertificateInfo(
            storeType.orNull,
            storeFile.asFile.orNull,
            storePassword.orNull,
            keyPassword.orNull,
            keyAlias.orNull,
        )

        val outSrc = outputDir.get().file("org/frknkrc44/hma_oss/zygote/Magic.java").asFile
        outSrc.parentFile.mkdirs()

        val bytes = certificateInfo.certificate.encoded
        outSrc.writeText(
            buildString {
                appendLine("package org.frknkrc44.hma_oss.zygote;")
                appendLine("public final class Magic {")
                appendLine("public static final byte[] magicNumbers = {")
                appendLine(bytes.joinToString(",") { it.toString() })
                appendLine("};")
                appendLine("}")
            }
        )
    }
}

androidComponents {
    onVariants { variant ->
        val variantCapped = variant.name.replaceFirstChar { it.titlecase(Locale.ROOT) }
        val variantLowered = variant.name.lowercase(Locale.ROOT)

        val managerApkFile = rootProject.layout.projectDirectory.file(
            "app/build/outputs/apk/$variantLowered/${rootProject.name}-$appVerName-$variantLowered.apk"
        )

        val copyManagerApp = tasks.register<CopyManagerAppTask>("copy${variantCapped}ManagerApp") {
            description = "Copies the manager APK into the $variantLowered module assets"

            dependsOn(":app:assemble$variantCapped")
            managerApk.set(managerApkFile)
        }
        variant.sources.assets?.addGeneratedSourceDirectory(
            copyManagerApp,
            CopyManagerAppTask::outputDir,
        )

        val sign = android.buildTypes[variantLowered].signingConfig
        val generateSignInfo = tasks.register<GenerateSignInfoTask>("generate${variantCapped}SignInfo") {
            description = "Generates the signature info used to verify the manager APK"

            storeType.set(sign?.storeType)
            storeFile.set(sign?.storeFile)
            storePassword.set(sign?.storePassword)
            keyAlias.set(sign?.keyAlias)
            keyPassword.set(sign?.keyPassword)
        }
        variant.sources.java?.addGeneratedSourceDirectory(
            generateSignInfo,
            GenerateSignInfoTask::outputDir,
        )
    }
}

zygisk {
    // inject to system_server
    packages(ZygoteLoader.PACKAGE_SYSTEM_SERVER)

    // module properties
    id = "hma_oss_zygisk"
    name = "HMA-OSS Zygisk"
    author = "frknkrc44"
    description = "A Zygisk backend for HMA-OSS"
    entrypoint = "org.frknkrc44.hma_oss.zygote.ZygoteEntry"
    archiveName = "${rootProject.name}-ZYGISK-${android.defaultConfig.versionName}"
    updateJson = "https://furkank.net/hma_oss_update_checker.json"
    isAddVariantToArchiveName = true
}

dependencies {
    implementation(projects.common)
    compileOnly(projects.stub)

    implementation(libs.androidx.annotation.jvm)
    implementation(libs.io.github.vova7878.r8annotations)

    api(androidvmtools.panama.core)
    api(androidvmtools.panama.unsafe)
    api(androidvmtools.panama.llvm)

    implementation(androidvmtools.sun.cleaner)
}
