plugins {
    alias(libs.plugins.agp.lib)
    alias(libs.plugins.refine)
}

android {
    namespace = "org.frknkrc44.stub"
}

dependencies {
    implementation(libs.androidx.annotation.jvm)
}
