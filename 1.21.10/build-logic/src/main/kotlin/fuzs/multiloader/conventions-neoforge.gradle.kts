package fuzs.multiloader

import fuzs.multiloader.mixin.MixinConfigJsonTask
import fuzs.multiloader.neoforge.configureModsToml
import fuzs.multiloader.neoforge.toml.NeoForgeModsTomlTask
import mod
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
                "-D${mod.id}.isDevelopmentEnvironment=true"
            )
        }

        named("client") {
            client()
            name("${project.name} Client ${versionCatalog.findVersion("minecraft").get()}")
            programArgs("--username", "Player####")
        }

        named("server") {
            server()
            name("${project.name} Server ${versionCatalog.findVersion("minecraft").get()}")
        }

        register("data") {
            @Suppress("UnstableApiUsage")
            clientData()
            name("${project.name} Data ${versionCatalog.findVersion("minecraft").get()}")
            programArgs("--all", "--mod", mod.id)
            programArgs(
                "--existing",
                project(":Common").file("src/main/resources").absolutePath
            )
            programArgs(
                "--output",
                project(":Common").file("src/generated/resources").absolutePath
            )
        }
    }
}

repositories {
    maven {
        name = "NeoForged"
        url = uri("https://maven.neoforged.net/releases/")
    }
    maven {
        name = "TheIllusiveC4"
        url = uri("https://maven.theillusivec4.top/")
    }
    maven {
        name = "OctoStudios"
        url = uri("https://maven.octo-studios.com/releases/")
    }
}

dependencies {
    "neoForge"(versionCatalog.findLibrary("neoforge.neoforge").get())

    versionCatalog.findLibrary("bettermodsbutton.neoforge")
        .orElse(null)
        ?.let { modLocalRuntime(it) { isTransitive = false } }
}

tasks.named<RemapJarTask>("remapJar") {
    atAccessWideners.add("${mod.id}.accesswidener")
}

val generateModsToml = tasks.register<NeoForgeModsTomlTask>("generateModsToml") {
    dependsOn(tasks.named<MixinConfigJsonTask>("generateMixinConfig"))
    configureModsToml(this)
}

tasks.named<ProcessResources>("processResources") {
    dependsOn(generateModsToml)
}
