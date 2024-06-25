package fuzs.proplacer.config;

import fuzs.puzzleslib.api.config.v3.Config;
import fuzs.puzzleslib.api.config.v3.ConfigCore;

public class ClientConfig implements ConfigCore {
    @Config(description = "Allow using Bedrock Edition-like fast block placement, with blocks being placed without leaving gaps or unwanted placements. Also enables placing blocks in mid-air.")
    public boolean allowFastPlacement = true;
    @Config(description = "Allow using Bedrock Edition-like reach-around block placement, where a block can be placed directly in front of the block the player is standing on in mid-air.")
    public boolean allowReachAroundPlacement = true;
    @Config(description = "Is fast block placement enabled by default. Toggle in-game via the dedicated key binding.")
    public boolean defaultFastPlacement = false;
    @Config(description = "Treat sneaking as active while placing blocks via the fast placement mechanic or reach-around. Allows block placement to work with interactable blocks such as chests and fence gates.")
    public boolean bypassUseBlock = false;
}
