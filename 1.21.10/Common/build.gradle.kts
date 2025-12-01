plugins {
    id("fuzs.multiloader.conventions-common")
}

dependencies {
    modApi(libs.puzzleslib.common)
}

multiloader {
    mixins {
        clientAccessor("MinecraftAccessor")
        clientAccessor("MultiPlayerGameModeAccessor")
    }
}
