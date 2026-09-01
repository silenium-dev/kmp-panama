pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://nexus.silenium.dev/repository/maven-releases/")
        maven("https://nexus.silenium.dev/repository/maven-snapshots/")
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "kmp-panama"
