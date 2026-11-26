// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

plugins {
    alias(libs.plugins.helix.android.feature)
}

android {
    namespace = "com.helixgallery.features.editor"
}

dependencies {
    implementation(project(":data:media"))
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.activity.compose)
}
