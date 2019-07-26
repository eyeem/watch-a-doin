rootProject.name = "watch-a-doin"

pluginManagement {
    repositories {
        maven(url = "https://kotlin.bintray.com/kotlinx") {
            name = "kotlinx"
        }
        gradlePluginPortal()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "kotlinx-serialization") {
                useModule("org.jetbrains.kotlin:kotlin-serialization:${requested.version}")
            }
        }
    }
}