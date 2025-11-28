package fuzs.multiloader

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import net.fabricmc.loom.task.RemapJarTask

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

tasks.withType<Jar>().configureEach {
    exclude("architectury.common.json")
}

tasks.named<JavaCompile>("compileJava") {
    dependsOn(configurations.named("commonJava"))
    source(configurations.named("commonJava"))
    // create an empty refmap if none exists to prevent a warning from the mixin config that the refmap is missing
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
    dependsOn(project(":Common").tasks.withType<ProcessResources>())
    dependsOn(configurations.named("commonResources"))
    from(configurations.named("commonResources"))
    from(project(":Common").layout.buildDirectory.dir("generated/resources"))
    from(project.layout.buildDirectory.dir("generated/resources"))
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
