import com.android.ide.common.signing.KeystoreHelper
import com.v7878.zygisk.gradle.ZygoteLoader
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.PrintStream
import java.util.Locale
import kotlin.io.path.Path

plugins {
    alias(libs.plugins.agp.app)
    alias(libs.plugins.kotlin)
    alias(libs.plugins.com.github.aerathstuff.zygoteloader)
}

val appPackageName: String by rootProject.extra

android {
    namespace = "$appPackageName.zygote"

    defaultConfig {
        applicationId = namespace
    }

    sourceSets {
        getByName("main") {
            java {
                srcDirs(Path(rootDir.path, "external", "AndroidVMTools", "src", "main", "java"))
            }
        }
    }
}

tasks.clean {
    for (item in arrayOf("debug", "release")) {
        delete(File(android.sourceSets[item].assets.srcDirs.first(), "manager.apk"))
    }
}

afterEvaluate {
    android.applicationVariants.forEach { variant ->
        val variantCapped = variant.name.replaceFirstChar { it.titlecase(Locale.ROOT) }
        val variantLowered = variant.name.lowercase(Locale.ROOT)

        val outSrcDir = layout.buildDirectory.dir("generated/source/signInfo/${variantLowered}")
        val outSrc = outSrcDir.get().file("org/frknkrc44/hma_oss/zygote/Magic.java")
        val managerApk = project(":app").layout.buildDirectory.file(
            "outputs/apk/$variantLowered/${rootProject.name}-${android.defaultConfig.versionName}-${variantLowered}.apk"
        )
        val embeddedManagerApk = File(android.sourceSets[variantLowered].assets.srcDirs.first(), "manager.apk")
        val signInfoTask = tasks.register("generate${variantCapped}SignInfo") {
            description = "Generate signature info for verification"

            // The Zygisk archive embeds the matching manager APK. A standalone
            // :zygote:assembleRelease must build it, not require a prior :app run.
            dependsOn(":app:assemble$variantCapped")
            inputs.file(managerApk)
            outputs.files(outSrc, embeddedManagerApk)
            doLast {
                managerApk.get().asFile.copyTo(embeddedManagerApk, overwrite = true)

                val sign = android.buildTypes[variantLowered].signingConfig
                outSrc.asFile.parentFile.mkdirs()
                val certificateInfo = KeystoreHelper.getCertificateInfo(
                    sign?.storeType,
                    sign?.storeFile,
                    sign?.storePassword,
                    sign?.keyPassword,
                    sign?.keyAlias
                )
                PrintStream(outSrc.asFile).apply {
                    println("package org.frknkrc44.hma_oss.zygote;")
                    println("public final class Magic {")
                    print("public static final byte[] magicNumbers = {")
                    val bytes = certificateInfo.certificate.encoded
                    print(bytes.joinToString(",") { it.toString() })
                    println("};")
                    println("}")
                }
            }
        }
        variant.registerJavaGeneratingTask(signInfoTask, outSrcDir.get().asFile)
        // Both the wrapper APK and Magisk ZIP consume the copied manager.apk.
        // Its generation must finish before either task reads variant assets.
        tasks.named("merge${variantCapped}Assets") { dependsOn(signInfoTask) }
        tasks.matching { it.name == "mergeMagisk$variantCapped" }.configureEach {
            dependsOn(signInfoTask)
        }

        val kotlinCompileTask = tasks.findByName("compile${variantCapped}Kotlin") as KotlinCompile
        kotlinCompileTask.dependsOn(signInfoTask)
        val srcSet = objects.sourceDirectorySet("magic", "magic").srcDir(outSrcDir)
        kotlinCompileTask.source(srcSet)
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
    implementation(libs.dev.rikka.hidden.compat)

    api(androidvmtools.panama.core)
    api(androidvmtools.panama.unsafe)
    api(androidvmtools.panama.llvm)

    implementation(androidvmtools.sun.cleaner)
}
