plugins {
    id("fuzs.multiloader.conventions-common")
}

multiLoader {
    mixins {
        clientAccessor("MinecraftAccessor")
        clientAccessor("MultiPlayerGameModeAccessor")
    }
}

dependencies {
    modApi(libs.puzzleslib.common)
}
