package fuzs.multiloader

import fuzs.multiloader.neoforge.configureModsToml
import fuzs.multiloader.neoforge.toml.NeoForgeModsTomlTask
import mod
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
//    neoForge()
//}

loom {
    accessWidenerPath.set(project(":Common").loom.accessWidenerPath)

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
                "-D${mod.id}.isDevelopmentEnvironment=true"
            )
        }

        named("client") {
            client()
            configName = "${project.name} Client ${versionCatalog.findVersion("minecraft").get()}"
            programArgs("--username", "Player####")
        }

        named("server") {
            server()
            configName = "${project.name} Server ${versionCatalog.findVersion("minecraft").get()}"
        }

        register("data") {
            @Suppress("UnstableApiUsage")
            clientData()
            configName = "${project.name} Data ${versionCatalog.findVersion("minecraft").get()}"
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

configurations {
    create("common")
    create("shadowCommon")
    compileClasspath.get().extendsFrom(getByName("common"))
    runtimeClasspath.get().extendsFrom(getByName("common"))
    getByName("developmentNeoForge").extendsFrom(getByName("common"))
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
    configurations.getByName("common")(
        project(path = ":Common", configuration = "namedElements")
    ) { isTransitive = false }

    configurations.getByName("shadowCommon")(
        project(path = ":Common", configuration = "transformProductionNeoForge")
    ) { isTransitive = false }

    "neoForge"(versionCatalog.findLibrary("neoforge.neoforge").get())

    versionCatalog.findLibrary("bettermodsbutton.neoforge")
        .orElse(null)
        ?.let { modLocalRuntime(it) { isTransitive = false } }
}

tasks.withType<Jar>().configureEach {
//    exclude("architectury.common.json")
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    configurations = listOf(project.configurations.getByName("shadowCommon"))
    archiveClassifier.set("dev-shadow")
}

tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar") {
    inputFile.set(
        tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar")
            .flatMap { it.archiveFile })
    dependsOn(tasks.named("shadowJar"))
    archiveClassifier.set("")
    atAccessWideners.add("${mod.id}.accesswidener")
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

tasks.register<NeoForgeModsTomlTask>("generateModsToml") {
    configureModsToml(this)
}

tasks.withType<ProcessResources> {
    dependsOn(tasks.named("generateModsToml"))
    from(layout.buildDirectory.dir("generated/resources"))
}
