// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.

pluginManagement {
    // Plugins de convenção: a configuração compartilhada entre módulos mora aqui.
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "HelixGallery"

include(":app")

include(":core:model")
include(":core:common")
include(":core:designsystem")
include(":core:datastore")

include(":data:media")
include(":data:updates")

include(":features:photos")
include(":features:albums")
include(":features:viewer")
include(":features:search")
include(":features:settings")
