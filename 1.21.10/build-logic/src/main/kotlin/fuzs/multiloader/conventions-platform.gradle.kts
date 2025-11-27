package fuzs.multiloader

import fuzs.multiloader.extension.MultiLoaderExtension
import fuzs.multiloader.metadata.LinkProvider
import fuzs.multiloader.mixin.MixinConfigJsonTask
import metadata
import mod
import net.fabricmc.loom.task.RemapJarTask
import versionCatalog
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

plugins {
    id("java")
    id("java-library")
    id("maven-publish")
    id("signing")
    id("idea")
    id("dev.architectury.loom")
//    id("architectury-plugin")
    id("me.modmuss50.mod-publish-plugin")
}

extensions.create<MultiLoaderExtension>("multiLoader")

base.archivesName = mod.name.replace("[^a-zA-Z]".toRegex(), "")
version = "v${mod.version}-mc${versionCatalog.findVersion("minecraft").get()}-${project.name}"
group = mod.group

repositories {
    mavenCentral()
    mavenLocal()

    maven {
        name = "Fuzs Mod Resources"
        url = uri("https://raw.githubusercontent.com/Fuzss/modresources/main/maven/")
    }
    maven {
        name = "Parchment"
        url = uri("https://maven.parchmentmc.org")
    }
    maven {
        name = "Jared"
        url = uri("https://maven.blamejared.com/")
    }
    maven {
        name = "Jitpack"
        url = uri("https://jitpack.io")
    }
    maven {
        name = "Shedaniel"
        url = uri("https://maven.shedaniel.me/")
    }
    maven {
        name = "Wisp Forest"
        url = uri("https://maven.wispforest.io/releases/")
    }
    maven {
        name = "Su5eD"
        url = uri("https://maven.su5ed.dev/releases/")
    }
    maven {
        name = "Minecraft Forge"
        url = uri("https://maven.minecraftforge.net/")
    }
    maven {
        name = "Terraformers"
        url = uri("https://maven.terraformersmc.com/")
    }

    exclusiveContent {
        forRepository {
            maven {
                name = "AppleSkin"
                url = uri("https://maven.ryanliptak.com/")
            }
        }
        filter {
            includeGroup("squeek.appleskin")
        }
    }

    exclusiveContent {
        forRepository {
            maven {
                name = "KosmX"
                url = uri("https://maven.kosmx.dev/")
            }
        }
        filter {
            includeGroup("dev.kosmx.player-anim")
        }
    }

    exclusiveContent {
        forRepository {
            maven {
                name = "RedlanceMinecraft"
                url = uri("https://repo.redlance.org/public")
            }
        }
        filter {
            includeGroupByRegex("com\\.zigythebird(\\..+)?")
        }
    }

    exclusiveContent {
        forRepository {
            maven {
                name = "GeckoLib"
                url = uri("https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/")
            }
        }
        filter {
            includeGroup("software.bernie.geckolib")
        }
    }

    exclusiveContent {
        forRepository {
            maven {
                name = "SmartBrainLib"
                url = uri("https://dl.cloudsmith.io/public/tslat/sbl/maven/")
            }
        }
        filter {
            includeGroup("net.tslat.smartbrainlib")
        }
    }

    exclusiveContent {
        forRepository {
            maven {
                name = "CurseForge"
                url = uri("https://cursemaven.com/")
            }
        }
        filter {
            includeGroup("curse.maven")
        }
    }

    exclusiveContent {
        forRepository {
            maven {
                name = "Modrinth"
                url = uri("https://api.modrinth.com/maven/")
            }
        }
        filter {
            includeGroup("maven.modrinth")
        }
    }
}

java {
    toolchain {
        val javaVersion = versionCatalog.findVersion("java").get().requiredVersion
        languageVersion.set(JavaLanguageVersion.of(javaVersion))
        vendor.set(JvmVendorSpec.ADOPTIUM)
    }

    withSourcesJar()
    withJavadocJar()
}

tasks.withType<JavaCompile>().configureEach {
    // Ensure that the encoding is set to UTF-8, no matter what the system default is.
    // This fixes some edge cases with special characters not displaying correctly.
    // See: http://yodaconditions.net/blog/fix-for-java-file-encoding-problems-with-gradle.html
    // If Javadoc is generated, this must be specified in that task too.
    options.encoding = "UTF-8"
    options.release.set(versionCatalog.findVersion("java").get().requiredVersion.toInt())
    // Disables general compiler warnings.
    options.isWarnings = false
    // Enables compiler messages for uses of deprecated methods or classes.
    options.isDeprecation = true
    // Adds the unchecked warning flag so the compiler reports raw type and unsafe cast situations.
    options.compilerArgs.add("-Xlint:unchecked")
}

tasks.withType<Javadoc>().configureEach {
    // Workaround cast for https://github.com/gradle/gradle/issues/7038
    val standardJavadocDocletOptions = options as StandardJavadocDocletOptions
    // Prevent Java 8's strict doclint for Javadocs from failing builds.
    standardJavadocDocletOptions.addStringOption("Xdoclint:none", "-quiet")
}

tasks.withType<Jar>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(rootProject.file("../LICENSE.md"))
    from(rootProject.file("../LICENSE-ASSETS.md"))
    from(rootProject.file("CHANGELOG.md"))

    manifest {
        val attributeMap = mutableMapOf<String, Any>(
            "Specification-Title" to mod.name,
            "Specification-Version" to mod.version,
            "Specification-Vendor" to mod.authors.joinToString(", "),
            "Implementation-Title" to mod.name,
            "Implementation-Version" to mod.version,
            "Implementation-Vendor" to mod.authors.joinToString(", "),
            "Implementation-Timestamp" to ZonedDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ")),
            "Implementation-Timestamp-Milli" to System.currentTimeMillis(),
            "Built-On-Java" to "${System.getProperty("java.vm.version")} (${System.getProperty("java.vm.vendor")})",
            "Built-On-Minecraft" to versionCatalog.findVersion("game").get()
        )
        metadata.links.firstOrNull { it.name == LinkProvider.GITHUB }
            ?.url()
            ?.let { attributeMap["Implementation-URL"] = it }
        attributes(attributeMap)
    }

    group = "jar"
}

tasks.withType<GenerateModuleMetadata>().configureEach {
    // Disables Gradle's custom module metadata from being published to maven.
    // The metadata includes mapped dependencies which are not reasonably consumable by other mod developers.
    enabled = false
}

tasks.withType<AbstractArchiveTask>().configureEach {
    // Activate reproducible builds
    // https://docs.gradle.org/current/userguide/working_with_files.html#sec:reproducible_archives
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

val incrementBuildNumber = tasks.register("incrementBuildNumber") {
    onlyIf { project.hasProperty("uniqueBuildNumber") }
    doLast {
        val file = File(project.gradle.gradleUserHomeDir, "gradle.properties")
        val properties = Properties().apply { if (file.exists()) load(file.inputStream()) }
        val uniqueBuildNumber = (properties.getProperty("uniqueBuildNumber")?.toIntOrNull() ?: 0) + 1
        properties.setProperty("uniqueBuildNumber", uniqueBuildNumber.toString())
        file.outputStream().use { properties.store(it, null) }
    }
}

val copyDevelopmentJar = tasks.register<Copy>("copyDevelopmentJar") {
    onlyIf { project.hasProperty("uniqueBuildNumber") && project.hasProperty("buildJarOutputDir") }
    val remapJar = tasks.named<RemapJarTask>("remapJar")
    dependsOn(remapJar)
    from(remapJar.map { it.archiveFile })
    into(project.findProperty("buildJarOutputDir")!!)
    rename { fileName ->
        val oldValue = "v${mod.version}-mc"
        val newValue = "v${mod.version}-dev.${project.findProperty("uniqueBuildNumber")}-mc"
        fileName.replace(oldValue, newValue)
    }
}

tasks.named("build") {
    finalizedBy(copyDevelopmentJar, incrementBuildNumber)
}

idea {
    module {
        // IDEA no longer automatically downloads Sources / Javadoc jars for dependencies.
        // Now we need to explicitly enable the behavior.
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}

loom {
    silentMojangMappingsLicense()
    @Suppress("UnstableApiUsage")
    mixin {
        useLegacyMixinAp = true
        defaultRefmapName = "${mod.id}.${project.name.lowercase()}.refmap.json"
    }
    decompilers {
        get("vineflower").apply {
            // Shows the method name of lambdas in a comment
            options.put("mark-corresponding-synthetics", "1")
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${versionCatalog.findVersion("game").get()}")
    @Suppress("UnstableApiUsage")
    mappings(loom.layered {
        officialMojangMappings {
            setNameSyntheticMembers(true)
        }

        parchment(
            "org.parchmentmc.data:parchment-${
                versionCatalog.findVersion("parchment.minecraft").get()
            }:${versionCatalog.findVersion("parchment.version").get()}@zip"
        )
    })
}

tasks.register<MixinConfigJsonTask>("generateMixinConfig") {
    outputFile.set(file("src/main/resources/mixin.config.json"))
    config {
        mixinPackage.set("com.example.mixin")
        minVersion.set("0.8")
        required.set(true)
        client.set(listOf("ClientMixin"))
        server.set(listOf("ServerMixin"))
        // ... other configuration
    }
}

//tasks.withType<ProcessResources>().configureEach {
//    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
//
//    from(project(":Common").sourceSets["main"].resources) {
//        exclude("${mod.id}.common.mixins.json", "common.mixins.json")
//    }
//    exclude("architectury.common.json")
//
//    from(project(":Common").file("src/generated/resources")) {
//        exclude(".cache/")
//    }
//
//    val fileNames = mapOf(
//        "^common.mixins.json\$" to "${mod.id}.common.mixins.json",
//        "^fabric.mixins.json\$" to "${mod.id}.fabric.mixins.json",
//        "^neoforge.mixins.json\$" to "${mod.id}.neoforge.mixins.json",
//        "^forge.mixins.json\$" to "${mod.id}.forge.mixins.json"
//    )
//
//    fileNames.forEach { from, to -> rename(from, to) }
//
//    val resourceProperties = mapOf(
//        "modId" to mod.id,
//        "modName" to mod.name,
//        "modVersion" to mod.version,
//        "modDescription" to mod.description,
//        "modGroup" to project.group.toString(),
//        "modAuthor" to mod.authors.joinToString(", "),
//        "modLicense" to mod.license
//    )
//
//    inputs.properties(resourceProperties)
//
//    val matchingFiles = listOf(
//        "${mod.id}.common.mixins.json",
//        "${mod.id}.fabric.mixins.json",
//        "${mod.id}.neoforge.mixins.json",
//        "${mod.id}.forge.mixins.json",
//        "common.mixins.json",
//        "fabric.mixins.json",
//        "neoforge.mixins.json",
//        "forge.mixins.json"
//    )
//
//    matchingFiles.forEach { pattern ->
//        filesMatching(pattern) {
//            expand(resourceProperties)
//        }
//    }
//}
