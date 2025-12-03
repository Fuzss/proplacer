import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `java`
    `java-library`
    `maven-publish`
    `signing`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

base.archivesName = extra["mod.id"].toString()
version = extra["mod.version"].toString()
group = extra["mod.group"].toString()

repositories {
    gradlePluginPortal()
    maven {
        name = "Architectury"
        url = uri("https://maven.architectury.dev/")
    }
    maven {
        name = "Fabric"
        url = uri("https://maven.fabricmc.net/")
    }
    maven {
        name = "NeoForge"
        url = uri("https://maven.neoforged.net/releases/")
    }
    exclusiveContent {
        forRepository {
            maven {
                name = "Forge"
                url = uri("https://maven.minecraftforge.net")
            }
        }
        filter {
            @Suppress("UnstableApiUsage")
            includeGroupAndSubgroups("net.minecraftforge")
        }
    }
}

dependencies {
    implementation(libs.architectury.loom)
    implementation(libs.mod.publish.plugin)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktoml)
    implementation(libs.foojay.resolver.convention)
}

gradlePlugin {
    plugins {
        register("conventionsSettings") {
            id = "fuzs.multiloader.conventions-settings"
            implementationClass = "fuzs.multiloader.plugin.SettingsConventionPlugin"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(libs.versions.java.get()))
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get()))
        vendor.set(JvmVendorSpec.ADOPTIUM)
    }

    withSourcesJar()
}

tasks.withType<Jar>().configureEach {
    from(rootProject.file("LICENSE.md"))
    manifest {
        attributes("Implementation-Version" to project.version)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(libs.versions.java.get().toInt())
}

// The pluginMaven publication is only available after evaluation.
afterEvaluate {
    publishing {
        publications {
            fun populatePomFile(pom: MavenPom) = with(pom) {
                name.set(extra["mod.name"].toString())
                description.set(extra["mod.description"].toString())
                project.providers.gradleProperty("distributions.github.slug").orNull
                    ?.let { "https://github.com/Fuzss/$it" }
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
                            url.set("$it/issues")
                        }
                    }

                licenses {
                    license {
                        name.set(extra["mod.license"].toString())
                        url.set("https://spdx.org/licenses/${extra["mod.license"]}.html")
                    }
                }

                developers {
                    for (author in extra["mod.authors"].toString()
                        .split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }) {
                        developer {
                            id.set(author.lowercase())
                            name.set(author)
                        }
                    }
                }
            }

            val pluginMaven = getByName("pluginMaven") as MavenPublication
            pluginMaven.artifactId = extra["mod.id"].toString()
            pluginMaven.version = extra["mod.version"].toString()
            pluginMaven.groupId = extra["mod.group"].toString()
            pluginMaven.pom { populatePomFile(this) }

            create<MavenPublication>("snapshotMaven") {
                artifactId = extra["mod.id"].toString()
                version = extra["mod.version"].toString().substringBeforeLast('.') + "-SNAPSHOT"
                groupId = extra["mod.group"].toString()
                from(components["java"])
                pom { populatePomFile(this) }
            }
        }

        repositories {
            project.providers.gradleProperty("fuzs.multiloader.project.resources").orNull
                ?.let { "$it/maven" }
                ?.let {
                    maven {
                        name = "FuzsModResources"
                        url = uri(it)
                    }
                }
        }
    }
}

tasks.register("${project.name.lowercase().replace(Regex("[^a-zA-Z]"), "")}-publish") {
    group = "multiloader/publish"
    val task = project.tasks.named("publishAllPublicationsToFuzsModResourcesRepository")
    description = task.get().description
    dependsOn(task)
}
