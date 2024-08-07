package fuzs.proplacer.config;

import fuzs.puzzleslib.api.config.v3.Config;
import fuzs.puzzleslib.api.config.v3.ConfigCore;
import fuzs.puzzleslib.api.config.v3.serialization.ConfigDataSet;
import fuzs.puzzleslib.api.config.v3.serialization.KeyedValueProvider;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

public class ClientConfig implements ConfigCore {
    @Config(
            description = {
                    "Allow using Bedrock Edition-like fast block placement, with blocks being placed without leaving gaps or unwanted placements. Also enables placing blocks when clicking in mid-air while building.",
                    "Additionally introduces a similar mechanic for quickly breaking blocks in a row or column in creative mode.",
                    "Toggle in-game via the dedicated key binding."
            }
    )
    public boolean allowFastPlacement = true;
    @Config(description = "Allow using Bedrock Edition-like reach-around block placement, where a block can be placed directly in front of the block the player is standing on when clicking in mid-air for fast bridging.")
    public boolean allowReachAroundPlacement = true;
    @Config(description = "Treat sneaking as active while placing blocks via the fast placement mechanic or reach-around. Allows block placement to work with interactable blocks such as chests and fence gates.")
    public boolean bypassUseBlock = false;
    @Config(name = "normal_placement_blocks",
            description = {
                    "Blocks that are excluded from the fast placement mechanic.",
                    ConfigDataSet.CONFIG_DESCRIPTION
            }
    )
    List<String> normalPlacementRaw = KeyedValueProvider.toString(Registries.BLOCK, Blocks.SCAFFOLDING);

    public ConfigDataSet<Block> normalPlacement;

    @Override
    public void afterConfigReload() {
        this.normalPlacement = ConfigDataSet.from(Registries.BLOCK, this.normalPlacementRaw);
    }
}
