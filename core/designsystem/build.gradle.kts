// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

plugins {
    alias(libs.plugins.helix.android.library)
    alias(libs.plugins.helix.android.compose)
}

android {
    namespace = "com.helixgallery.core.designsystem"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    // "api": quem usa o design system recebe os componentes do Material 3 junto.
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.core)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    // Imagens da grade e do visualizador: só o design system conhece o Coil.
    // coil-video e coil-gif se registram sozinhos (ServiceLoader).
    implementation(libs.coil.compose)
    implementation(libs.coil.video)
    implementation(libs.coil.gif)
}
