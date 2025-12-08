package fuzs.multiloader

import expectPlatform
import fuzs.multiloader.architectury.ArchitecturyCommonJsonTask
import fuzs.multiloader.metadata.ModLoaderProvider
import mod
import org.gradle.api.internal.tasks.JvmConstants
import versionCatalog

plugins {
    id("fuzs.multiloader.conventions-core")
}

project.expectPlatform(ModLoaderProvider.COMMON)

loom {
    accessWidenerPath.set(file("src/main/resources/${mod.id}.accesswidener"))
}

dependencies {
    // Using `loaderLibraries` is necessary here.
    // This is so that Mixin is properly added to the jar MANIFEST in net.fabricmc.loom.task.service.JarManifestService.getMixinVersion().
    loaderLibraries(versionCatalog.findLibrary("mixin.common").get())
    loaderLibraries(versionCatalog.findLibrary("mixinextras.common").get())
}

val generateArchitecturyCommonJson = tasks.register<ArchitecturyCommonJsonTask>("generateArchitecturyCommonJson") {
    outputFile.set(layout.buildDirectory.file("generated/resources/architectury.common.json"))
    json {
        accessWidener.set(loom.accessWidenerPath.map { it.asFile.name })
    }
}

tasks.named<ProcessResources>(JvmConstants.PROCESS_RESOURCES_TASK_NAME) {
    dependsOn(generateArchitecturyCommonJson)
}
