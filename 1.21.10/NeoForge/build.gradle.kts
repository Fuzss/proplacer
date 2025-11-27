plugins {
    id("fuzs.multiloader.conventions-neoforge")
}

dependencies {
    modApi(libs.puzzleslib.neoforge)
}

multiLoader {
    modFileMetadata {
//    enumExtensions.set("enumextensions.json")
        toml {
            mod("examplemod") {
            }
            modProperties("examplemod", mapOf("catalogueImageIcon40" to 'c', "catalogueImageIcon" to "mod_logo.png", "catalogueImageIcon2" to 15, "catalogueImageIcon3" to true))
            modProperties("examplemod", mapOf("catalogueImageIcon40" to "aaaaa", "catalogueImageIcon" to "mod_banner.png", "catalogueImageIcon52" to 18, "catalogueImageIcon4" to false))
            properties.put("example", "1.2.3")
//        extraProperties("lithium:options", "mixin.minimal_nonvanilla.world.block_entity_ticking.support_cache", false)
//        extraProperties("lithium.options", "mixinblock_entity_tickingsupport_cache", 15)
//        extraProperties("lithiumoptions", "mixinminimal_nonvanillaworldblock_entity_tickingsupport_cache", "ccc")
//        extraProperties("lithiumoptions", "mixinminimal_nonvanillaworldblock_entity_tickingsupport_cache", "cccc")
//        extraProperties("lithiumoptions", "mixinminimal_nonvanillaworldblock_entity_tickingsupport_cache2", "ccc")
            extraArrayProperties("lithium:options", "mixin.minimal_nonvanilla.world.block_entity_ticking.support_cache", false)
            extraArrayProperties("lithium.options", "mixinblock_entity_tickingsupport_cache", 15)
            extraArrayProperties("lithiumoptions", "mixinminimal_nonvanillaworldblock_entity_tickingsupport_cache", "ccc")
            extraArrayProperties("lithiumoptions", "mixinminimal_nonvanillaworldblock_entity_tickingsupport_cache", "ccc")
            extraArrayProperties("lithiumoptions", "mixinminimal_nonvanillaworldblock_entity_tickingsupport_cache", "ccc")
//        features("examplemod", "openGLVersion", "[3.2,)")
        }
    }
}
