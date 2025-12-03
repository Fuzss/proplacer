package fuzs.multiloader

import fuzs.multiloader.architectury.ArchitecturyCommonJsonTask
import mod
import net.fabricmc.loom.task.AbstractRemapJarTask
import versionCatalog

plugins {
    id("fuzs.multiloader.conventions-core")
}

loom {
    accessWidenerPath.set(file("src/main/resources/${mod.id}.accesswidener"))
}

dependencies {
    // Using `loaderLibraries` is necessary here.
    // This is so that Mixin is properly added to the jar MANIFEST in net.fabricmc.loom.task.service.JarManifestService.getMixinVersion().
    loaderLibraries(versionCatalog.findLibrary("mixin.common").get())
    loaderLibraries(versionCatalog.findLibrary("mixinextras.common").get())
}

tasks.withType<AbstractRemapJarTask>().configureEach {
    targetNamespace.set("named")
}

val generateArchitecturyCommonJson = tasks.register<ArchitecturyCommonJsonTask>("generateArchitecturyCommonJson") {
    outputFile.set(layout.buildDirectory.file("generated/resources/architectury.common.json"))
    json {
        accessWidener.set(loom.accessWidenerPath.map { it.asFile.name })
    }
}

tasks.named<ProcessResources>("processResources") {
    dependsOn(generateArchitecturyCommonJson)
}
