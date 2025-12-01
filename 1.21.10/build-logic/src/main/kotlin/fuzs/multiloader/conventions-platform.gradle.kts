package fuzs.multiloader

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
    id("dev.architectury.loom")
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
    add("commonJava", project(mapOf("path" to ":Common", "configuration" to "commonJava")))
    add("commonResources", project(mapOf("path" to ":Common", "configuration" to "commonResources")))
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
    dependsOn(project(":Common").tasks.named<ProcessResources>("processResources"))
    from(project(":Common").layout.buildDirectory.dir("generated/resources")) {
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
    val remapJar = tasks.named<RemapJarTask>("remapJar")
    file.set(remapJar.get().archiveFile)

    val changelogFile = file("../CHANGELOG.md")
    val changelogText = changelogFile.readText()
    val minecraftVersion = versionCatalog.findVersion("minecraft").get().requiredVersion
    displayName.set(
        "[${name.uppercase()}] [$minecraftVersion] ${base.archivesName.get()} v${mod.version}"
    )
    type.set(STABLE)
    version.set(mod.version)
    modLoaders.add(name.lowercase())
    dryRun.set(true)

    for (link in metadata.links) {
        val uploadTokenProperty =
            providers.gradleProperty("fuzs.multiloader.upload.${link.name.name.lowercase()}.token")
        when (link.name) {
            LinkProvider.CURSEFORGE -> {
                curseforge {
                    accessToken.set(uploadTokenProperty)
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
                    accessToken.set(uploadTokenProperty)
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
                    accessToken.set(uploadTokenProperty)
                    repository.set(link.url().replace("https://github.com/", ""))
                    commitish.set("main")
                    tagName.set("v${mod.version}+mc$minecraftVersion/${name.lowercase()}")

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
