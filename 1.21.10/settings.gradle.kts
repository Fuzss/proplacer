pluginManagement {
    repositories {
        gradlePluginPortal()
        maven {
            name = "Architectury"
            url = uri("https://maven.architectury.dev/")
        }
        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }
        maven {
            name = "NeoForge"
            url = uri("https://maven.neoforged.net/releases/")
        }
        exclusiveContent {
            forRepository {
                maven {
                    name = "Forge"
                    url = uri("https://maven.minecraftforge.net")
                }
            }
            filter {
                @Suppress("UnstableApiUsage")
                includeGroupAndSubgroups("net.minecraftforge")
            }
        }
    }

    includeBuild("build-logic")
}

plugins {
    id("fuzs.multiloader.conventions-settings")
}
