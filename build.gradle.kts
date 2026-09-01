import dev.silenium.build.ProjectConfig
import dev.silenium.gradle.conventions.android
import dev.silenium.gradle.conventions.jvm
import dev.silenium.gradle.conventions.publishing
import dev.silenium.gradle.conventions.compileSdk

plugins {
    dev.silenium.gradle.conventions.kmp
}

group = "dev.silenium.libs.panama"

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(kotlin("reflect"))
                implementation(libs.slf4j.api)
            }
        }
        androidMain {
            dependencies {
                implementation(libs.panama.android.core)
            }
        }
    }
}

conventions {
    jvm {
        jvmTarget = ProjectConfig.JVM_TARGET
    }
    android {
        compileSdk {
            version = release(ProjectConfig.COMPILE_SDK)
        }
        minSdk = ProjectConfig.MIN_SDK
        jvmTarget = ProjectConfig.ANDROID_JVM_TARGET

        namespace = "dev.silenium.libs.panama"
    }
    publishing {
        enabled = true
        licenseFile = rootProject.file("LICENSE")
        pomSpec.set {
            name = project.name
            description = "A KMP wrapper for Panama API supporting all JVM-based platforms"
            url = "https://github.com/silenium-dev/kmp-panama"
            inceptionYear = "2026"
            licenses {
                license {
                    name = "GPL-3.0-or-later"
                    url = "https://spdx.org/licenses/GPL-3.0-or-later.html"
                }
            }
            developers {
                developer {
                    id = "silenium-dev"
                    email = "support@silenium-dev.net"
                }
            }
            scm {
                connection = "scm:git:git://github.com/silenium-dev/kmp-panama.git"
                developerConnection = "scm:git:ssh://github.com/silenium-dev/kmp-panama.git"
                url = "https://github.com/silenium-dev/kmp-panama"
            }
        }
    }
}
