import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.ide.common.signing.KeystoreHelper
import com.v7878.zygisk.gradle.ZygoteLoader
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.PrintStream
import java.util.Locale

plugins {
    alias(libs.plugins.agp.app)
    alias(libs.plugins.com.github.aerathstuff.zygoteloader)
}

val appPackageName: String by rootProject.extra

configure<ApplicationExtension> {
    namespace = "$appPackageName.zygote"

    defaultConfig {
        applicationId = namespace
    }
}

val androidExt get() = extensions.findByType(ApplicationExtension::class)!!

fun getAssetDir(sourceSet: String) = File(
    project.projectDir,
    "${File.separator}${androidExt.sourceSets[sourceSet].assets.directories.first()}"
)

fun getManagerApk(sourceSet: String) = File(getAssetDir(sourceSet), "manager.apk")

tasks.clean {
    for (sourceSet in arrayOf("debug", "release")) {
        delete(getManagerApk(sourceSet))
    }
}

extensions.findByType(ApplicationAndroidComponentsExtension::class)?.run {
    onVariants(selector().all()) { variant ->
        val variantCapped = variant.name.replaceFirstChar { it.titlecase(Locale.ROOT) }
        val variantLowered = variant.name.lowercase(Locale.ROOT)

        val outSrcDir = layout.buildDirectory.dir("generated/source/signInfo/${variantLowered}")
        val outSrc = outSrcDir.get().file("org/frknkrc44/hma_oss/zygote/Magic.java")
        val signInfoTask = tasks.register("generate${variantCapped}SignInfo") {
            outputs.file(outSrc)
            doLast {
                addManagerApp(variantLowered)

                val sign = androidExt.buildTypes[variantLowered].signingConfig
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

        variant.sources.java?.addStaticSourceDirectory(outSrcDir.get().asFile.toString())

        tasks.whenTaskAdded {
            if (name == "compile${variantCapped}Kotlin") {
                val kotlinCompileTask = tasks.findByName("compile${variantCapped}Kotlin") as KotlinCompile
                kotlinCompileTask.dependsOn(signInfoTask)
                val srcSet = objects.sourceDirectorySet("magic", "magic").srcDir(outSrcDir)
                kotlinCompileTask.source(srcSet)
            }
        }
    }
}

fun addManagerApp(variant: String) {
    val builtFile = File(
        layout.buildDirectory.get().asFile.toString().replace(project.name, "app"),
        "outputs/apk/$variant/${rootProject.name}-${androidExt.defaultConfig.versionName}-${variant}.apk",
    )

    if (!builtFile.exists()) {
        throw GradleException("The manager app for $variant ($builtFile) is not built yet")
    }

    builtFile.copyTo(
        getManagerApk(variant),
        overwrite = true,
    )
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
    archiveName = "${rootProject.name}-ZYGISK-${androidExt.defaultConfig.versionName}"
    isAddVariantToArchiveName = true
}

dependencies {
    implementation(projects.common)

    implementation(libs.androidx.annotation.jvm)
    implementation(libs.com.android.tools.build.apksig)
    implementation(libs.io.github.vova7878.androidvmtools)
    implementation(libs.io.github.vova7878.r8annotations)
    implementation(libs.dev.rikka.hidden.compat)

    compileOnly(libs.dev.rikka.hidden.stub)
}
