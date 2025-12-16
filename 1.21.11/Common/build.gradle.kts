plugins {
    id("fuzs.multiloader.conventions-common")
}

dependencies {
    modCompileOnlyApi(libs.puzzleslib.common)
}

multiloader {
    mixins {
        clientAccessor("MinecraftAccessor")
        clientAccessor("MultiPlayerGameModeAccessor")
    }
}
