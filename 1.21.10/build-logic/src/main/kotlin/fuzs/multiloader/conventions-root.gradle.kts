package fuzs.multiloader

import fuzs.multiloader.discord.ChangelogSectionType
import fuzs.multiloader.discord.DiscordWebhookTask
import fuzs.multiloader.discord.MessageFlags
import fuzs.multiloader.metadata.LinkProvider
import fuzs.multiloader.metadata.ModLoaderProvider
import fuzs.multiloader.metadata.loadMetadata
import fuzs.multiloader.task.IncrementBuildNumber
import kotlinx.serialization.json.Json
import metadata
import mod
import platformProjects
import projectPlatform
import versionCatalog
import java.time.Instant

afterEvaluate {
    val metadata = loadMetadata()
    val json = Json { prettyPrint = true }
    val output = project.layout.projectDirectory.file("metadata.json").asFile
    output.writeText(json.encodeToString(metadata))
}

tasks.register<IncrementBuildNumber>("incrementBuildNumber") {
    val propertiesFile = layout.buildDirectory.file("build.properties")
    // Check file existence here, as the property requires it when set.
    if (propertiesFile.orNull?.asFile?.exists() == true) {
        inputFile.set(propertiesFile)
    }

    outputFile.set(propertiesFile)
}

fun readChangelogFields(): Map<String, String> {
    val changelogText = project.file("CHANGELOG.md").readText()
    // Extract most recent section (everything until next "## " or EOF)
    val sectionRegex = Regex("""## \[.*?] - \d{4}-\d{2}-\d{2}\r?\n(?s)(.*?)(?=\r?\n## |$)""")
    val latestSection = sectionRegex.find(changelogText)?.groups?.get(1)?.value?.trim() ?: ""
    val changelogSections = mutableMapOf<String, String>()
    val subsectionRegex = Regex("""### (.*?)\r?\n(?s)(.*?)(?=\r?\n### |$)""")

    for (section in subsectionRegex.findAll(latestSection)) {
        val title = section.groups[1]?.value?.trim() ?: ""
        val body = section.groups[2]?.value?.trim() ?: ""
        val emoji = ChangelogSectionType.emojiByName(title)
        val formattedBody = body.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .fold(mutableListOf<String>()) { accumulator, line ->
                if (line.startsWith("- ")) {
                    accumulator.add(line.replaceFirst("- ", "\u2022 "))
                } else if (accumulator.isNotEmpty()) {
                    accumulator[accumulator.lastIndex] = accumulator.last() + " " + line
                } else {
                    accumulator.add(line)
                }
                accumulator
            }
            .joinToString("\n")
        changelogSections += "$emoji $title" to formattedBody
    }

    return changelogSections
}

tasks.register<DiscordWebhookTask>("sendDiscordWebhook") {
    val discordChannelProperty = providers.gradleProperty("fuzs.multiloader.remote.discord.channel")
    val discordTokenProperty = providers.gradleProperty("fuzs.multiloader.remote.discord.token")
    val remoteResourcesProperty = providers.gradleProperty("fuzs.multiloader.remote.resources")

    val minecraftVersion = versionCatalog.findVersion("minecraft").get()

    payload {
        channel.set(discordChannelProperty.get())
        token.set(discordTokenProperty.get())
        val epochSeconds = System.currentTimeMillis() / 1000
        content.set("<t:$epochSeconds:R>")
        flags.set(MessageFlags.SUPPRESS_NOTIFICATIONS)
        debug.set(true)

        embed {
            title.set("[$minecraftVersion] ${mod.name} v${mod.version}")
            description.set(mod.description)
            metadata.links.firstOrNull { it.name == LinkProvider.MODRINTH }
                ?.url()
                ?.let { url.set(it) }
            timestamp.set(Instant.now().toString())
            color.set(5814783)

            val footerValues = listOf(mod.name, "v${mod.version}", minecraftVersion)
            footer(footerValues.joinToString(" \u2022 "))
            image("${remoteResourcesProperty.get()}/pages/data/${mod.id}/banner.png")
            thumbnail("${remoteResourcesProperty.get()}/pages/data/${mod.id}/logo.png")

            author("Fuzs") {
                url.set("https://modrinth.com/user/Fuzs")
                iconUrl.set("${remoteResourcesProperty.get()}/pages/commons/avatar.png")
            }

            readChangelogFields().forEach { field(it.key, it.value) }

            val downloadLinks = metadata.links.mapNotNull {
                when (it.name) {
                    LinkProvider.CURSEFORGE -> "<:CurseForge:893088361634471948> [CurseForge](${it.url()})"
                    LinkProvider.MODRINTH -> "<:modrinth:1176378033578459206> [Modrinth](${it.url()})"
                    else -> null
                }
            }

            if (downloadLinks.isNotEmpty()) {
                field("\uD83D\uDCE5 Downloads", downloadLinks.joinToString("\n")) {
                    inline.set(true)
                }
            }

            metadata.links.firstOrNull { it.name == LinkProvider.GITHUB }
                ?.url()
                ?.let {
                    field(
                        "<:github:1422695832951455814> GitHub",
                        listOf(
                            "\uD83D\uDC68\u200D\uD83D\uDCBB [Source]($it)",
                            "\u26A0\uFE0F [Issues]($it/issues)"
                        ).joinToString("\n")
                    ) {
                        inline.set(true)
                    }
                }

            field("\uD83D\uDCAC Support", "<:Fuzs:993195872131235881> <#917550806922846299>") {
                inline.set(true)
            }
        }
    }
}

tasks.register("root-build") {
    group = "multiloader/build"
    dependsOn(project.subprojects.map { it.tasks.named("build") })
}

tasks.register("root-clean") {
    group = "multiloader/build"
    dependsOn(project.subprojects.map { it.tasks.named("clean") })
}

tasks.register("root-publish") {
    group = "multiloader/publish"
    dependsOn(project.subprojects.map { it.tasks.named("publishMavenJavaPublicationToFuzsModResourcesRepository") })
}

tasks.register("root-curseforge") {
    group = "multiloader/remote"
    dependsOn(project.platformProjects.map { it.tasks.named("publishCurseforge") })
}

tasks.register("root-discord") {
    group = "multiloader/remote"
    dependsOn(tasks.named("sendDiscordWebhook"))
}

tasks.register("root-fabric") {
    group = "multiloader/remote"
    dependsOn(
        project.subprojects
            .filter { it.projectPlatform == ModLoaderProvider.FABRIC }
            .map { it.tasks.named("publishMods") }
    )
}

tasks.register("root-github") {
    group = "multiloader/remote"
    dependsOn(project.platformProjects.map { it.tasks.named("publishGithub") })
}

tasks.register("root-modrinth") {
    group = "multiloader/remote"
    dependsOn(project.platformProjects.map { it.tasks.named("publishModrinth") })
}

tasks.register("root-neoforge") {
    group = "multiloader/remote"
    dependsOn(
        project.subprojects
            .filter { it.projectPlatform == ModLoaderProvider.NEOFORGE }
            .map { it.tasks.named("publishMods") }
    )
}

tasks.register("root-root") {
    group = "multiloader/remote"
    dependsOn(project.platformProjects.map { it.tasks.named("publishMods") })
    dependsOn(tasks.named("sendDiscordWebhook"))
}

tasks.register("root-sources") {
    group = "multiloader/setup"
    dependsOn(project.subprojects.map { it.tasks.named("genSources") })
}
