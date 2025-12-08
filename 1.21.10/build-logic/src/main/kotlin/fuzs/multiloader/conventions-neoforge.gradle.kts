package fuzs.multiloader

import commonProject
import expectPlatform
import fuzs.multiloader.architectury.ArchitecturyCommonJsonTask
import fuzs.multiloader.metadata.LinkProvider
import fuzs.multiloader.metadata.ModLoaderProvider
import fuzs.multiloader.neoforge.setupModsTomlTask
import fuzs.multiloader.neoforge.toml.NeoForgeModsTomlTask
import fuzs.multiloader.neoforge.update.NeoForgeUpdateJson
import kotlinx.serialization.json.Json
import me.modmuss50.mpp.PublishModTask
import metadata
import mod
import net.fabricmc.loom.task.RemapJarTask
import org.gradle.api.internal.tasks.JvmConstants
import versionCatalog

plugins {
    id("fuzs.multiloader.conventions-platform")
}

project.expectPlatform(ModLoaderProvider.NEOFORGE)

loom {
    accessWidenerPath.set(project.commonProject.loom.accessWidenerPath)

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
                project.commonProject.file("src/main/resources").absolutePath
            )
            programArgs(
                "--output",
                project.commonProject.file("src/generated/resources").absolutePath
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
    atAccessWideners.add(project.commonProject.loom.accessWidenerPath.map { it.asFile.name })
    // We need to have an access widener in the NeoForge jar for transitive entries to apply in an Architectury Loom setup.
    // Since we compile on the native loaders, common access widener changes will otherwise not carry over and lead to errors.
    from(project.commonProject.loom.accessWidenerPath) {
        into("/META-INF")
    }
}

val generateArchitecturyCommonJson = tasks.register<ArchitecturyCommonJsonTask>("generateArchitecturyCommonJson") {
    outputFile.set(layout.buildDirectory.file("generated/resources/architectury.common.json"))
    json {
        accessWidener.set(loom.accessWidenerPath.map { "META-INF/${it.asFile.name}" })
    }
}

val generateModsToml = tasks.register<NeoForgeModsTomlTask>("generateModsToml") {
    setupModsTomlTask()
}

tasks.named<ProcessResources>(JvmConstants.PROCESS_RESOURCES_TASK_NAME) {
    dependsOn(generateArchitecturyCommonJson, generateModsToml)
}

val refreshUpdateJson = tasks.register("refreshUpdateJson") {
    val projectResources = providers.gradleProperty("fuzs.multiloader.project.resources")
    val file = File(projectResources.get(), "update/${mod.id}.json")
    val homepage = metadata.links.firstOrNull { it.name == LinkProvider.MODRINTH }
        ?.url()
    val minecraftVersion = versionCatalog.findVersion("minecraft").get()
    val modVersion = mod.version

    onlyIf { projectResources.isPresent && homepage != null }

    doLast {
        val json = Json { prettyPrint = true }
        val promos = if (file.exists()) {
            val updateJson = json.decodeFromString<NeoForgeUpdateJson>(file.readText())
            updateJson.promos.toMutableMap()
        } else {
            mutableMapOf()
        }

        promos["${minecraftVersion}-latest"] = modVersion
        promos["${minecraftVersion}-recommended"] = modVersion

        file.writeText(json.encodeToString(NeoForgeUpdateJson(homepage!!, promos.toSortedMap())))
    }
}

tasks.withType(PublishModTask::class.java).configureEach {
    finalizedBy(refreshUpdateJson)
}

tasks.register("${project.name.lowercase()}-data") {
    group = "multiloader/run"
    val task = tasks.named("runData")
    description = task.get().description
    dependsOn(task)
}
