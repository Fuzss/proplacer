package fuzs.proplacer.config;

import fuzs.puzzleslib.api.config.v3.Config;
import fuzs.puzzleslib.api.config.v3.ConfigCore;

public class ClientConfig implements ConfigCore {
    @Config(description = "Is Bedrock Edition-like fast block placement enabled by default. Toggle in-game via the dedicated key binding.")
    public boolean defaultFastPlacement = false;
}
