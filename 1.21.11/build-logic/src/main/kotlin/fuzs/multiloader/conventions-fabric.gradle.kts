package fuzs.multiloader

import expectPlatform
import fuzs.multiloader.fabric.setupModJsonTask
import fuzs.multiloader.metadata.ModLoaderProvider
import net.fabricmc.loom.task.FabricModJsonV1Task
import net.fabricmc.loom.task.RemapJarTask
import org.gradle.api.internal.tasks.JvmConstants
import versionCatalog

plugins {
    id("fuzs.multiloader.conventions-platform")
}

project.expectPlatform(ModLoaderProvider.FABRIC)

loom {
    runs {
        named("client") {
            client()
            name("${project.name} Client ${versionCatalog.findVersion("minecraft").get()}")
            vmArgs(
                "-Dfabric-tag-conventions-v2.missingTagTranslationWarning=silenced",
                "-Dfabric-tag-conventions-v1.legacyTagWarning=silenced"
            )
        }

        named("server") {
            server()
            name("${project.name} Server ${versionCatalog.findVersion("minecraft").get()}")
            vmArgs(
                "-Dfabric-tag-conventions-v2.missingTagTranslationWarning=silenced",
                "-Dfabric-tag-conventions-v1.legacyTagWarning=silenced"
            )
        }
    }
}

repositories {
    maven {
        name = "Modmuss"
        url = uri("https://maven.modmuss50.me/")
    }
    maven {
        name = "Ladysnake Libs"
        url = uri("https://maven.ladysnake.org/releases/")
    }
    maven {
        name = "jamieswhiteshirt"
        url = uri("https://maven.jamieswhiteshirt.com/libs-release/")
    }
}

dependencies {
    modApi(versionCatalog.findLibrary("fabricloader.fabric").get())

    versionCatalog.findLibrary("modmenu.fabric")
        .orElse(null)
        ?.let { modLocalRuntime(it) { isTransitive = false } }
}

tasks.named<RemapJarTask>("remapJar") {
    injectAccessWidener.set(true)
}

val generateModJson = tasks.register<FabricModJsonV1Task>("generateModJson") {
    setupModJsonTask()
}

tasks.named<ProcessResources>(JvmConstants.PROCESS_RESOURCES_TASK_NAME) {
    dependsOn(generateModJson)
}
