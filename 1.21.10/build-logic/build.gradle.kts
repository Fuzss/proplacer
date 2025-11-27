plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    kotlin("jvm") version "2.2.0" // or kotlin("multiplatform") or any other kotlin plugin
    kotlin("plugin.serialization") version "2.2.0"
}

repositories {
    gradlePluginPortal()
    maven { url = uri("https://maven.architectury.dev/") }
    maven { url = uri("https://maven.fabricmc.net/") }
    maven { url = uri("https://maven.neoforged.net/releases/") }
    maven { url = uri("https://maven.minecraftforge.net/") }
}

dependencies {
    // TODO migrate this to a local version catalog and include kotlin if possible
    implementation(pluginLibs.shadow)
    implementation(pluginLibs.architecturyloom)
    implementation(pluginLibs.architecturyplugin)
    implementation(pluginLibs.modpublishplugin)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("com.akuleshov7:ktoml-core:0.7.1")
    implementation("org.gradle.toolchains.foojay-resolver-convention:org.gradle.toolchains.foojay-resolver-convention.gradle.plugin:1.0.0")
}

gradlePlugin {
    plugins {
        register("conventionsSettings") {
            id = "fuzs.multiloader.conventions-settings"
            implementationClass = "fuzs.multiloader.plugin.SettingsConventionPlugin"
        }
//        register("conventions") {
//            id = "fuzs.multiloader.conventions"
//            implementationClass = "fuzs.multiloader.ConventionPlugin"
//        }
//        register("conventionsCommon") {
//            id = "fuzs.multiloader.conventions-common"
//            implementationClass = "fuzs.multiloader.CommonConventionPlugin"
//        }
//        register("conventionsFabric") {
//            id = "fuzs.multiloader.conventions-fabric"
//            implementationClass = "fuzs.multiloader.FabricConventionPlugin"
//        }
//        register("conventionsNeoForge") {
//            id = "fuzs.multiloader.conventions-neoforge"
//            implementationClass = "fuzs.multiloader.NeoForgeConventionPlugin"
//        }
    }
}
