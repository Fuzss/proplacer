pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.architectury.dev/")
        maven("https://maven.fabricmc.net/")
        maven("https://maven.neoforged.net/releases/")
        maven("https://maven.minecraftforge.net/")
    }

    includeBuild("build-logic")
}

plugins {
    id("fuzs.multiloader.conventions-settings")
}
