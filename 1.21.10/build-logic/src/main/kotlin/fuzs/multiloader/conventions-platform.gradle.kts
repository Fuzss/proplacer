package fuzs.multiloader

import commonProject
import externalMods
import fuzs.multiloader.metadata.DependencyType
import fuzs.multiloader.metadata.LinkProvider
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import me.modmuss50.mpp.PublishModTask
import metadata
import mod
import net.fabricmc.loom.task.RemapJarTask
import versionCatalog
import java.io.FileNotFoundException

plugins {
    id("fuzs.multiloader.conventions-core")
}

configurations {
    named("commonJava") {
        isCanBeResolved = true
    }
    named("commonResources") {
        isCanBeResolved = true
    }
}

dependencies {
    compileOnly(project.commonProject)
    add("commonJava", project(mapOf("path" to project.commonProject.path, "configuration" to "commonJava")))
    add("commonResources", project(mapOf("path" to project.commonProject.path, "configuration" to "commonResources")))
}

tasks.named<JavaCompile>("compileJava") {
    dependsOn(configurations.named("commonJava"))
    source(configurations.named("commonJava"))
    // Create an empty refmap if none exists to prevent a warning from the mixin config that the refmap is missing, which also shows up in production.
    @Suppress("UnstableApiUsage")
    val refmapFile = layout.buildDirectory.dir("classes/java/main/${loom.mixin.defaultRefmapName.get()}")
    doLast {
        val file = refmapFile.get().asFile
        if (!file.exists() || file.readText().isBlank()) {
            file.parentFile.mkdirs()
            file.writeText(Json.encodeToString(JsonObject.serializer(), JsonObject(emptyMap())))
        }
    }
}

tasks.named<ProcessResources>("processResources") {
    dependsOn(configurations.named("commonResources"))
    from(configurations.named("commonResources"))
    dependsOn(project.commonProject.tasks.named<ProcessResources>("processResources"))
    from(project.commonProject.layout.buildDirectory.dir("generated/resources")) {
        exclude("architectury.common.json")
    }
}

tasks.named<RemapJarTask>("remapJar") {
    archiveClassifier.set("")
}

tasks.named<Jar>("jar") {
    archiveClassifier.set("dev")
}

tasks.named<Jar>("sourcesJar") {
    dependsOn(configurations.named("commonJava"))
    from(configurations.named("commonJava"))
    dependsOn(configurations.named("commonResources"))
    from(configurations.named("commonResources"))
}

tasks.named<Javadoc>("javadoc") {
    dependsOn(configurations.named("commonJava"))
    source(configurations.named("commonJava"))
}

tasks.withType<PublishModTask>().configureEach {
    notCompatibleWithConfigurationCache("The plugin stores a reference to the Gradle project object.")
    val versionString = "v${project.mod.version}-${project.versionCatalog.findVersion("minecraft").get()}"
    doFirst {
        val file = file("../CHANGELOG.md")
        if (!file.canRead()) {
            throw FileNotFoundException("Could not read changelog file")
        }

        if (!file.readText().contains(versionString)) {
            throw IllegalStateException("Missing changelog version: $versionString")
        }
    }
}

publishMods {
    val changelogFile = file("../CHANGELOG.md")
    val changelogText = changelogFile.readText()

    val remapJar = tasks.named<RemapJarTask>("remapJar")
    file.set(remapJar.get().archiveFile)

    val minecraftVersion = versionCatalog.findVersion("minecraft").get().requiredVersion
    displayName.set("[${name.uppercase()}] [$minecraftVersion] ${base.archivesName.get()} v${mod.version}")

    type.set(STABLE)
    version.set(mod.version)
    modLoaders.add(name.lowercase())

    val projectDebug = providers.gradleProperty("project.debug")
    dryRun.set(projectDebug.orNull.toBoolean())

    for (link in metadata.links) {
        val remoteToken =
            providers.gradleProperty("fuzs.multiloader.remote.${link.name.name.lowercase()}.token")

        if (remoteToken.isPresent) {
            when (link.name) {
                LinkProvider.CURSEFORGE -> {
                    curseforge {
                        accessToken.set(remoteToken)
                        projectId.set(link.id)
                        minecraftVersions.add(minecraftVersion)
                        changelog.set(changelogText)

                        for (entry in metadata.dependencies) {
                            externalMods.mods[entry.name]?.links?.firstOrNull { it.name == link.name }?.slug?.let {
                                when (entry.type) {
                                    DependencyType.REQUIRED -> requires(it)
                                    DependencyType.EMBEDDED -> embeds(it)
                                    DependencyType.OPTIONAL -> optional(it)
                                    DependencyType.UNSUPPORTED -> Unit
                                }
                            } ?: println("Unable to link dependency: $entry")
                        }
                    }
                }

                LinkProvider.MODRINTH -> {
                    modrinth {
                        accessToken.set(remoteToken)
                        projectId.set(link.id)
                        minecraftVersions.add(minecraftVersion)
                        changelog.set(changelogText)

                        for (entry in metadata.dependencies) {
                            externalMods.mods[entry.name]?.links?.firstOrNull { it.name == link.name }?.slug?.let {
                                when (entry.type) {
                                    DependencyType.REQUIRED -> requires(it)
                                    DependencyType.EMBEDDED -> embeds(it)
                                    DependencyType.OPTIONAL -> optional(it)
                                    DependencyType.UNSUPPORTED -> Unit
                                }
                            } ?: println("Unable to link dependency: $entry")
                        }
                    }
                }

                LinkProvider.GITHUB -> {
                    github {
                        accessToken.set(remoteToken)
                        repository.set(link.url().replace("https://github.com/", ""))
                        commitish.set("main")
                        tagName.set("v${mod.version}-mc$minecraftVersion/${name.lowercase()}")

                        // Only include the relevant changelog section.
                        val changelogSections = changelogText.split(Regex("(?m)^## \\["), limit = 3)
                        changelog.set("## " + changelogSections.getOrNull(1)?.trim())

                        val sourcesJar = tasks.named<Jar>("sourcesJar")
                        additionalFiles.from(sourcesJar.get().archiveFile)
                        val javadocJar = tasks.named<Jar>("javadocJar")
                        additionalFiles.from(javadocJar.get().archiveFile)
                    }
                }
            }
        }
    }
}

tasks.register("${project.name.lowercase()}-client") {
    group = "multiloader/run"
    val task = tasks.named("runClient")
    description = task.get().description
    dependsOn(task)
}

tasks.register("${project.name.lowercase()}-server") {
    group = "multiloader/run"
    val task = tasks.named("runServer")
    description = task.get().description
    dependsOn(task)
}

if (metadata.links.firstOrNull { it.name == LinkProvider.CURSEFORGE } != null) {
    tasks.register("${project.name.lowercase()}-curseforge") {
        group = "multiloader/remote"
        val task = tasks.named("publishCurseforge")
        description = task.get().description
        dependsOn(task)
    }
}

if (metadata.links.firstOrNull { it.name == LinkProvider.GITHUB } != null) {
    tasks.register("${project.name.lowercase()}-github") {
        group = "multiloader/remote"
        val task = tasks.named("publishGithub")
        description = task.get().description
        dependsOn(task)
    }
}

if (metadata.links.firstOrNull { it.name == LinkProvider.MODRINTH } != null) {
    tasks.register("${project.name.lowercase()}-modrinth") {
        group = "multiloader/remote"
        val task = tasks.named("publishModrinth")
        description = task.get().description
        dependsOn(task)
    }
}
