package fuzs.multiloader.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.*

@Deprecated("forRemoval")
class ConventionPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = with(project) {

        val modId: String = providers.gradleProperty("mod.id").get()
        val modGroup: String = providers.gradleProperty("mod.group").get()
        val modName: String = providers.gradleProperty("mod.name").get()
        val modVersion: String = providers.gradleProperty("mod.version").get()
        val modAuthor: String = providers.gradleProperty("mod.author").get()
        val modDescription: String = providers.gradleProperty("mod.description").get()
        val modLicense: String = providers.gradleProperty("mod.license").get()

        val pluginLibs = project.extensions.getByType<VersionCatalogsExtension>().named("pluginLibs")
        pluginLibs.findPlugin("architecturyloom")

//        project.buildscript {
//            dependencies.add("classpath", pluginLibs.findPlugin("architecturyloom").get())
//        }

        project.plugins.apply("java")
        project.plugins.apply("java-library")
        project.plugins.apply("maven-publish")
        project.plugins.apply("signing")
        project.plugins.apply("idea")
        project.plugins.apply("dev.architectury.loom")
//        project.plugins.apply("architectury-plugin")
//        project.plugins.apply("me.modmuss50.mod-publish-plugin")

//        extensions.configure<LoomGradleExtension>("loom") {
//            silentMojangMappingsLicense()
//
//            mixin {
//                useLegacyMixinAp = true
//                defaultRefmapName = "${modId}.${project.name.lowercase()}.refmap.json"
//            }
//        }
//
//        dependencies {
//            add("minecraft", "com.mojang:minecraft:${libs.versions.game.get()}")
//
//            add("mappings", extensions.getByType<LoomGradleExtension>().layered {
//                officialMojangMappings {
//                    setNameSyntheticMembers(true)
//                }
//                parchment(
//                    "org.parchmentmc.data:parchment-${libs.versions.parchment.minecraft.get()}:${libs.versions.parchment.version.get()}@zip"
//                )
//            })
//        }
    }
}
