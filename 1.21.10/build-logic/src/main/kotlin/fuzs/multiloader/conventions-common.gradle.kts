package fuzs.multiloader

import mod
import versionCatalog

plugins {
    id("java")
    id("java-library")
    id("dev.architectury.loom")
    id("fuzs.multiloader.conventions-platform")
}

//architectury {
//    common(rootProject.subprojects.map { it.name.lowercase() }.filterNot { it.contains("common") })
//}

loom {
    accessWidenerPath.set(file("src/main/resources/${mod.id}.accesswidener"))
}

dependencies {
    modCompileOnly(versionCatalog.findLibrary("fabricloader.fabric").get())
}

tasks.withType<net.fabricmc.loom.task.AbstractRemapJarTask>().configureEach {
    targetNamespace.set("named")
}
