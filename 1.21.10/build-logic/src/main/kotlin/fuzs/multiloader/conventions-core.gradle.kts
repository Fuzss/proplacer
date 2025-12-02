package fuzs.multiloader

import fuzs.multiloader.extension.MultiLoaderExtension
import fuzs.multiloader.metadata.LinkProvider
import fuzs.multiloader.mixin.MixinConfigJsonTask
import fuzs.multiloader.task.IncrementBuildNumber
import metadata
import mod
import net.fabricmc.loom.LoomGradlePlugin
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
    id("me.modmuss50.mod-publish-plugin")
}

extensions.create<MultiLoaderExtension>("multiloader")

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
            @Suppress("UnstableApiUsage")
            includeGroupAndSubgroups("com.zigythebird")
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
    // Workaround cast for: https://github.com/gradle/gradle/issues/7038
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
        attributes(
            mapOf(
                "Specification-Title" to mod.name,
                "Specification-Version" to mod.version,
                "Specification-Vendor" to mod.authors.joinToString(", "),
                "Implementation-Title" to mod.name,
                "Implementation-Version" to mod.version,
                "Implementation-Vendor" to mod.authors.joinToString(", "),
                "Implementation-Timestamp" to ZonedDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ"))
            )
        )
        metadata.links.firstOrNull { it.name == LinkProvider.GITHUB }
            ?.url()
            ?.let { attributes["Implementation-URL"] = it }
        attributes(
            mapOf(
                "Build-Tool-Name" to "Architectury Loom",
                "Build-Tool-Version" to (LoomGradlePlugin::class.java.getPackage().implementationVersion ?: "unknown"),
                "Build-Jdk-Name" to System.getProperty("java.vm.name"),
                "Build-Jdk-Version" to System.getProperty("java.vm.version"),
                "Build-Jdk-Vendor" to System.getProperty("java.vm.vendor"),
                "Build-Jdk-Spec-Name" to System.getProperty("java.vm.specification.name"),
                "Build-Jdk-Spec-Version" to System.getProperty("java.vm.specification.version"),
                "Build-Jdk-Spec-Vendor" to System.getProperty("java.vm.specification.vendor"),
                "Build-Os-Name" to "${System.getProperty("os.name")} (${System.getProperty("os.arch")})",
                "Build-Os-Version" to System.getProperty("os.version")
            )
        )
    }

    group = "jar"
}

tasks.withType<GenerateModuleMetadata>().configureEach {
    // Disables Gradle's custom module metadata from being published to maven.
    // The metadata includes mapped dependencies which are not reasonably consumable by other mod developers.
    enabled = false
}

tasks.withType<AbstractArchiveTask>().configureEach {
    // Activate reproducible builds:
    // https://docs.gradle.org/current/userguide/working_with_files.html#sec:reproducible_archives
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
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
        defaultRefmapName = "${mod.id}.refmap.json"
    }
    decompilers {
        get("vineflower").apply {
            // Shows the method name of lambdas in a comment.
            options.put("mark-corresponding-synthetics", "1")
        }
    }
}

sourceSets.main {
    resources {
        srcDir("src/generated/resources")
        exclude(".cache/**")
    }
}

configurations {
    create("commonJava") {
        isCanBeResolved = false
        isCanBeConsumed = true
    }
    create("commonResources") {
        isCanBeResolved = false
        isCanBeConsumed = true
    }
}

artifacts {
    val main by sourceSets.named("main")
    main.java.srcDirs.forEach { add("commonJava", it) }
    main.resources.srcDirs.forEach { add("commonResources", it) }
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

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "${mod.id}-${project.name.lowercase()}"
            version = mod.version

            from(components["java"])

            pom {
                name.set("${mod.name} [${project.name}]")
                description.set(mod.description)
                metadata.links.firstOrNull { it.name == LinkProvider.GITHUB }
                    ?.url()
                    ?.let {
                        url.set(it)

                        scm {
                            url.set(it)
                            connection.set(it.replace("https", "scm:git:git") + ".git")
                            developerConnection.set(
                                it.replace(
                                    "https://github.com/",
                                    "scm:git:git@github.com:"
                                ) + ".git"
                            )
                        }

                        issueManagement {
                            system.set("github")
                            url.set("${it}/issues")
                        }
                    }

                licenses {
                    license {
                        name.set(mod.license)
                        url.set("https://spdx.org/licenses/${mod.license}.html")
                    }
                }

                developers {
                    for (author in mod.authors) {
                        developer {
                            id.set(author.lowercase())
                            name.set(author)
                        }
                    }
                }
            }
        }
    }

    repositories {
        maven {
            name = "FuzsModResources"
            url = uri(
                project.providers.gradleProperty("modResources")
                    .map { "$it/maven" }
                    .orElse(System.getenv("local_maven"))
            )
        }
    }
}

signing {
    sign(publishing.publications.named<MavenPublication>("mavenJava").get())
}

val generateMixinConfig = tasks.register<MixinConfigJsonTask>("generateMixinConfig") {
    val multiLoaderExtension = project.extensions.getByType(MultiLoaderExtension::class.java)
    outputFile.set(layout.buildDirectory.file("generated/resources/${mod.id}.${project.name.lowercase()}.mixins.json"))

    json {
        val platform = if (project.extra.has("loom.platform")) project.extra["loom.platform"] else null
        mixinPackage.set("${project.group}.${platform?.let { "${it}." }.orEmpty()}mixin")
        minVersion.set("0.8")
        required.set(true)
        compatibilityLevel.set("JAVA_${versionCatalog.findVersion("java").get().requiredVersion}")
        @Suppress("UnstableApiUsage")
        refmap.set(loom.mixin.defaultRefmapName)
        injectors {
            defaultRequire.set(1)
        }

        overwrites {
            requireAnnotations.set(true)
        }

        multiLoaderExtension.mixins.orNull?.execute(this)
    }
}

tasks.named<ProcessResources>("processResources") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    dependsOn(generateMixinConfig)
    from(project.layout.buildDirectory.dir("generated/resources"))
}

val copyDevelopmentJar = tasks.register<Copy>("copyDevelopmentJar") {
    val buildOutputProperty = providers.gradleProperty("fuzs.multiloader.build.output")
    onlyIf { buildOutputProperty.isPresent }

    val incrementBuildNumber = rootProject.tasks.named<IncrementBuildNumber>("incrementBuildNumber")
    dependsOn(incrementBuildNumber)

    val remapJar = tasks.named<RemapJarTask>("remapJar")
    dependsOn(remapJar)
    from(remapJar.flatMap { it.archiveFile })
    into(buildOutputProperty.get())

    // This runs at configuration time before the properties file is updated.
    // But that is fine as the value from the last run is still unique.
    val buildPropertiesFile = rootProject.layout.buildDirectory.file("build.properties").get().asFile
    val projectBuildNumber = Properties().apply {
        if (buildPropertiesFile.exists()) load(buildPropertiesFile.inputStream())
    }.getProperty("project.build") ?: "1"
    val oldValue = "v${mod.version}-mc"
    val newValue = "v${mod.version}-dev.${projectBuildNumber}-mc"

    rename { it.replace(oldValue, newValue) }
}

tasks.named("build") {
    finalizedBy(copyDevelopmentJar)
}
