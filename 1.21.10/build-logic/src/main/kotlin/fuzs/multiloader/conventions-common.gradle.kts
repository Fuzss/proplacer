package fuzs.multiloader

import fuzs.multiloader.architectury.ArchitecturyCommonJsonTask
import mod
import net.fabricmc.loom.task.AbstractRemapJarTask
import versionCatalog

plugins {
    id("dev.architectury.loom")
    id("fuzs.multiloader.conventions-core")
}

loom {
    accessWidenerPath.set(file("src/main/resources/${mod.id}.accesswidener"))
}

dependencies {
    // TODO use version catalog
    loaderLibraries("net.fabricmc:sponge-mixin:0.16.5+mixin.0.8.7")
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
