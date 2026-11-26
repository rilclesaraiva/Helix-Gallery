// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

plugins {
    alias(libs.plugins.helix.android.library)
}

android {
    namespace = "com.helixgallery.data.media"
}

dependencies {
    // "api": as features recebem o MediaItem junto com o repositório.
    api(project(":core:model"))
    implementation(libs.kotlinx.coroutines.android)
}
