package fuzs.multiloader

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import fuzs.multiloader.fabric.configureModJson
import fuzs.multiloader.neoforge.configureModsToml
import fuzs.multiloader.neoforge.toml.NeoForgeModsTomlTask
import mod
import net.fabricmc.loom.task.FabricModJsonV1Task
import net.fabricmc.loom.task.RemapJarTask
import org.gradle.kotlin.dsl.register
import versionCatalog

plugins {
    id("java")
    id("java-library")
    id("com.gradleup.shadow")
//    id("dev.architectury.loom")
    id("fuzs.multiloader.conventions-platform")
}

//architectury {
//    platformSetupLoomIde()
//    fabric()
//}

loom {
    accessWidenerPath = project(":Common").loom.accessWidenerPath

    runs {
        configureEach {
            runDir = "../run"
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
            configName = "${project.name} Client ${versionCatalog.findVersion("minecraft").get()}"
        }

        named("server") {
            server()
            configName = "${project.name} Server ${versionCatalog.findVersion("minecraft").get()}"
        }
    }
}

configurations {
    create("common")
    create("shadowCommon")
    compileClasspath.get().extendsFrom(getByName("common"))
    runtimeClasspath.get().extendsFrom(getByName("common"))
    getByName("developmentFabric").extendsFrom(getByName("common"))
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
    configurations.getByName("common")(
        project(path = ":Common", configuration = "namedElements")
    ) { isTransitive = false }

    configurations.getByName("shadowCommon")(
        project(path = ":Common", configuration = "transformProductionFabric")
    ) { isTransitive = false }

    modApi(versionCatalog.findLibrary("fabricloader.fabric").get())

    versionCatalog.findLibrary("modmenu.fabric")
        .orElse(null)
        ?.let { modLocalRuntime(it) { isTransitive = false } }
}

tasks.withType<Jar>().configureEach {
    exclude("architectury.common.json")
}

tasks.named<ShadowJar>("shadowJar") {
    configurations = listOf(project.configurations.getByName("shadowCommon"))
    archiveClassifier.set("dev-shadow")
}

tasks.named<RemapJarTask>("remapJar") {
    inputFile.set(
        tasks.named<ShadowJar>("shadowJar")
            .flatMap { it.archiveFile })
    dependsOn(tasks.named("shadowJar"))
    archiveClassifier.set("")
    injectAccessWidener.set(true)
}

tasks.named<Jar>("jar") {
    archiveClassifier.set("dev")
}

tasks.named<Jar>("sourcesJar") {
    val commonSources = project(":Common").tasks.named<Jar>("sourcesJar")
    dependsOn(commonSources)
    from(provider {
        zipTree(commonSources.get().archiveFile.get())
    })
}


tasks.register<FabricModJsonV1Task>("generateModJson") {
    configureModJson(this)
}

tasks.withType<ProcessResources> {
    dependsOn(tasks.named("generateModJson"))
    from(layout.buildDirectory.dir("generated/resources"))
}
