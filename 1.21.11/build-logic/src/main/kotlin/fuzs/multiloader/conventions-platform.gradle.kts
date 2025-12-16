package fuzs.multiloader

import commonProject
import externalMods
import fuzs.multiloader.bytecode.FieldMapping
import fuzs.multiloader.bytecode.loadConventionalTagsMappings
import fuzs.multiloader.discord.changelogVersion
import fuzs.multiloader.discord.verifyChangelogVersion
import fuzs.multiloader.metadata.DependencyType
import fuzs.multiloader.metadata.LinkProvider
import me.modmuss50.mpp.PublishModTask
import metadata
import mod
import net.fabricmc.loom.task.RemapJarTask
import org.gradle.api.internal.tasks.JvmConstants
import org.objectweb.asm.*
import projectPlatform
import versionCatalog

plugins {
    id("fuzs.multiloader.conventions-core")
}

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
    }
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
    compileOnly(project(project.commonProject.path)) { isTransitive = false }
    "commonJava"(project(mapOf("path" to project.commonProject.path, "configuration" to "commonJava")))
    "commonResources"(project(mapOf("path" to project.commonProject.path, "configuration" to "commonResources")))
}

tasks.named<JavaCompile>(JvmConstants.COMPILE_JAVA_TASK_NAME) {
    dependsOn(configurations.named("commonJava"))
    source(configurations.named("commonJava"))
}

tasks.named<ProcessResources>(JvmConstants.PROCESS_RESOURCES_TASK_NAME) {
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

tasks.named<Jar>(JvmConstants.JAR_TASK_NAME) {
    archiveClassifier.set("dev")
}

tasks.named<Jar>("sourcesJar") {
    dependsOn(configurations.named("commonJava"))
    from(configurations.named("commonJava"))
    dependsOn(configurations.named("commonResources"))
    from(configurations.named("commonResources"))
}

tasks.named<Javadoc>(JvmConstants.JAVADOC_TASK_NAME) {
    dependsOn(configurations.named("commonJava"))
    source(configurations.named("commonJava"))
}

tasks.withType<PublishModTask>().configureEach {
    notCompatibleWithConfigurationCache("The plugin stores a reference to the Gradle project object.")
    val versionString = project.changelogVersion
    doFirst {
        verifyChangelogVersion(file("../CHANGELOG.md"), versionString)
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
                            if (entry.platforms.any { it.matches(projectPlatform) }) {
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
                }

                LinkProvider.MODRINTH -> {
                    modrinth {
                        accessToken.set(remoteToken)
                        projectId.set(link.id)
                        minecraftVersions.add(minecraftVersion)
                        changelog.set(changelogText)

                        for (entry in metadata.dependencies) {
                            if (entry.platforms.any { it.matches(projectPlatform) }) {
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

tasks.named("classes") {
    doLast {
        val outputDirectory = layout.buildDirectory.dir("classes/java/main").get().asFile
        val mappings: Map<String, Map<String, FieldMapping>> = loadConventionalTagsMappings()

        outputDirectory.walkTopDown().forEach { file ->
            if (file.isFile && file.extension == "class") {
                val inputBytes = file.readBytes()
                val classReader = ClassReader(inputBytes)

                var applyTransformation = false

                classReader.accept(object : ClassVisitor(Opcodes.ASM9) {
                    override fun visitMethod(
                        access: Int,
                        name: String?,
                        descriptor: String?,
                        signature: String?,
                        exceptions: Array<out String>?
                    ): MethodVisitor {
                        val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
                        return object : MethodVisitor(Opcodes.ASM9, mv) {
                            override fun visitFieldInsn(opcode: Int, owner: String, name: String, desc: String) {
                                if (mappings[owner]?.containsKey(name) == true) {
                                    applyTransformation = true
                                }

                                super.visitFieldInsn(opcode, owner, name, desc)
                            }
                        }
                    }
                }, ClassReader.SKIP_FRAMES or ClassReader.SKIP_DEBUG)

                if (!applyTransformation) return@forEach

                val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
                val classVisitor = object : ClassVisitor(Opcodes.ASM9, classWriter) {
                    override fun visitMethod(
                        access: Int,
                        name: String,
                        descriptor: String,
                        signature: String?,
                        exceptions: Array<out String>?
                    ): MethodVisitor {
                        val methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions)
                        return object : MethodVisitor(Opcodes.ASM9, methodVisitor) {
                            override fun visitFieldInsn(opcode: Int, owner: String, name: String, descriptor: String) {
                                if (opcode == Opcodes.GETSTATIC) {
                                    val mapped = mappings[owner]?.get(name)
                                    super.visitFieldInsn(
                                        opcode,
                                        mapped?.owner ?: owner,
                                        mapped?.name ?: name,
                                        mapped?.descriptor ?: descriptor
                                    )
                                } else
                                    super.visitFieldInsn(opcode, owner, name, descriptor)
                            }
                        }
                    }
                }

                classReader.accept(classVisitor, 0)
                file.writeBytes(classWriter.toByteArray())
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
