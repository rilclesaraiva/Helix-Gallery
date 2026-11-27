// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

plugins {
    alias(libs.plugins.helix.android.application)
    alias(libs.plugins.helix.android.compose)
}

// Fonte única da versão: APP_VERSION em gradle.properties (ver CLAUDE.md).
// Nunca edite versionCode/versionName aqui. O versionCode nunca desce.
val appVersion: String = providers.gradleProperty("APP_VERSION").get()
val appVersionCode: Int = appVersion.split(".").map { it.toInt() }.let { (major, minor, patch) ->
    major * 10000 + minor * 100 + patch
}

// Assinatura de release: o CI decodifica a keystore dos Secrets e passa o
// caminho em ANDROID_KEYSTORE_PATH (com a senha e o alias). Nada disso vai para
// o Git; sem a variável, o release sai sem assinatura.
val releaseKeystorePath: String? = System.getenv("ANDROID_KEYSTORE_PATH")

android {
    namespace = "com.helixgallery.app"

    defaultConfig {
        // NUNCA mude o applicationId depois do primeiro APK publicado.
        applicationId = "com.helixgallery.app"
        versionCode = appVersionCode
        versionName = appVersion
        // O reconhecimento de texto (ML Kit) traz código nativo por arquitetura (~4 MB cada): só arm64-v8a,
        // que cobre os aparelhos com Android 12+ em uso.
        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    // Idiomas do app (inglês, português, espanhol): gera a lista para o Android 13+
    // mostrar o app em Configurações → Idioma do app.
    androidResources {
        generateLocaleConfig = true
    }

    signingConfigs {
        if (releaseKeystorePath != null) {
            create("release") {
                storeFile = file(releaseKeystorePath)
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ANDROID_KEY_ALIAS")
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            // R8: tira do APK o que o app não usa e otimiza o código. Sem
            // ofuscação (proguard-rules.pro começa com -dontobfuscate).
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (releaseKeystorePath != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:datastore"))
    implementation(project(":data:media"))

    implementation(project(":features:photos"))
    implementation(project(":features:albums"))
    implementation(project(":features:viewer"))
    implementation(project(":features:search"))
    implementation(project(":features:settings"))
    implementation(project(":features:vault"))
    implementation(project(":features:cleanup"))
    implementation(project(":features:editor"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
}
