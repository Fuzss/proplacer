package fuzs.multiloader

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
    compileOnly("net.fabricmc:sponge-mixin:0.16.5+mixin.0.8.7")
    compileOnly(versionCatalog.findLibrary("mixinextras.common").get())
    annotationProcessor(versionCatalog.findLibrary("mixinextras.common").get())
}

tasks.withType<AbstractRemapJarTask>().configureEach {
    targetNamespace.set("named")
}
