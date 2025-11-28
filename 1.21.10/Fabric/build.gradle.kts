plugins {
    id("fuzs.multiloader.conventions-fabric")
}

dependencies {
    modApi(libs.fabricapi.fabric)
    modApi(libs.puzzleslib.fabric)
}

multiLoader {
    modFile {
        json {
            customData.put("lithium:options", mapOf("mixin.minimal_nonvanilla.world.block_entity_ticking.support_cache" to false))
        }
//    packagePrefix.set("impl")
//    library.set(true)
    }
}
