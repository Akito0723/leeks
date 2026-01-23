plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("org.jetbrains.intellij.platform") version "2.10.5"
}

pluginManagement {
    repositories {
        maven("https://central.sonatype.com/repository/maven-snapshots/")
        gradlePluginPortal()
    }
}

rootProject.name = "leeks"
