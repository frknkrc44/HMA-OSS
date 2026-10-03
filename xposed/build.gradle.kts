import com.android.build.gradle.AppExtension
import com.android.ide.common.signing.KeystoreHelper
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.agp.lib)
    alias(libs.plugins.kotlin)
}

val upstreamPackage = "org.frknkrc44.hma_oss.zygote"
val backendPackage = "org.frknkrc44.hma_oss.xposed"
val upstreamSources = rootProject.file("zygote/src/main/java/org/frknkrc44/hma_oss/zygote")
val adaptedSources = layout.buildDirectory.dir("generated/source/upstream")

// Only backend-neutral business code is imported. Engine/lifecycle code stays local.
val adaptUpstreamSources by tasks.registering(Sync::class) {
    from(upstreamSources) {
        include("hook/**/*.kt", "service/HMAService*.kt", "util/**/*.kt")
        exclude("util/ZLUtils.kt")
        filter { line: String ->
            val adapted = line.replace(upstreamPackage, backendPackage)
                .replace("com.v7878.unsafe.invoke.EmulatedStackFrame",
                    "$backendPackage.service.HookFrame as EmulatedStackFrame")
            check(!adapted.trimStart().startsWith("import com.v7878.")) {
                "New upstream engine dependency needs an xposed adapter: $line"
            }
            adapted
        }
    }
    into(adaptedSources)
    filteringCharset = "UTF-8"
}

android {
    namespace = backendPackage
    sourceSets["main"].java.srcDir(adaptedSources)
    defaultConfig.consumerProguardFiles("proguard-rules.pro")
}

kotlin { jvmToolchain(21) }

tasks.withType<KotlinCompile>().configureEach { dependsOn(adaptUpstreamSources) }
tasks.named("preBuild") { dependsOn(adaptUpstreamSources) }

afterEvaluate {
    android.libraryVariants.forEach { variant ->
        val variantName = variant.name
        val capitalized = variantName.replaceFirstChar { it.uppercaseChar() }
        val output = layout.buildDirectory.dir("generated/source/signInfo/$variantName")
        val generate = tasks.register("generate${capitalized}SignInfo") {
            // Read the actual app signing configuration, rather than library debug defaults.
            dependsOn(":app:validateSigning$capitalized")
            outputs.dir(output)
            outputs.upToDateWhen { false }
            doLast {
                val app = project(":app").extensions.getByType<AppExtension>()
                val sign = requireNotNull(app.buildTypes[variant.buildType.name].signingConfig)
                val certificate = KeystoreHelper.getCertificateInfo(
                    sign.storeType, sign.storeFile, sign.storePassword, sign.keyPassword, sign.keyAlias
                ).certificate.encoded
                val target = output.get().file("org/frknkrc44/hma_oss/xposed/Magic.java").asFile
                target.parentFile.mkdirs()
                target.writeText("""
                    package $backendPackage;
                    public final class Magic {
                        public static final byte[] magicNumbers = {${certificate.joinToString(",")}};
                    }
                """.trimIndent())
            }
        }
        variant.registerJavaGeneratingTask(generate, output.get().asFile)
        tasks.named<KotlinCompile>("compile${capitalized}Kotlin") {
            dependsOn(generate)
            source(output)
        }
    }
}

dependencies {
    implementation(projects.common)
    compileOnly(projects.stub)
    compileOnly(libs.libxposed.api)
    implementation(libs.androidx.annotation.jvm)
    implementation(libs.dev.rikka.hidden.compat)
    testImplementation(libs.junit)
    testImplementation(libs.libxposed.api)
}
