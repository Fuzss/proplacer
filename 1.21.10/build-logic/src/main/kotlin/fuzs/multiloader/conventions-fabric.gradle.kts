package fuzs.multiloader

import fuzs.multiloader.fabric.setupModJsonTask
import mod
import net.fabricmc.loom.task.FabricModJsonV1Task
import net.fabricmc.loom.task.RemapJarTask
import org.gradle.kotlin.dsl.named
import versionCatalog

plugins {
    id("dev.architectury.loom")
    id("fuzs.multiloader.conventions-platform")
}

loom {
    accessWidenerPath.set(project(":Common").loom.accessWidenerPath)

    runs {
        configureEach {
            runDir("../run")
            ideConfigGenerated(true)
            startFirstThread()
            vmArgs(
                "-Xms1G",
                "-Xmx4G",
                "-Dmixin.debug.export=true",
                "-Dlog4j2.configurationFile=${
                    this@configureEach.javaClass.classLoader.getResource("log4j.xml")
                        ?: throw IllegalStateException("log4j.xml not found in plugin resources")
                }",
                "-Dpuzzleslib.isDevelopmentEnvironment=true",
                "-D${mod.id}.isDevelopmentEnvironment=true",
                "-Dfabric-tag-conventions-v2.missingTagTranslationWarning=silenced",
                "-Dfabric-tag-conventions-v1.legacyTagWarning=silenced"
            )
        }

        named("client") {
            client()
            name("${project.name} Client ${versionCatalog.findVersion("minecraft").get()}")
        }

        named("server") {
            server()
            name("${project.name} Server ${versionCatalog.findVersion("minecraft").get()}")
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

tasks.named<ProcessResources>("processResources") {
    dependsOn(generateModJson)
}
