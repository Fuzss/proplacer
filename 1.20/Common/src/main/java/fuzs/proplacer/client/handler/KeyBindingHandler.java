package fuzs.proplacer.client.handler;

import com.mojang.blaze3d.platform.InputConstants;
import fuzs.proplacer.ProPlacer;
import fuzs.proplacer.config.ClientConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class KeyBindingHandler {
    public static final String KEY_CATEGORY = "key.categories." + ProPlacer.MOD_ID;
    public static final KeyMapping KEY_TOGGLE_FAST_PLACEMENT = new KeyMapping("key.fast_placement",
            InputConstants.UNKNOWN.getValue(),
            KEY_CATEGORY
    );
    public static final String KEY_FAST_PLACEMENT_MESSAGE = "gui.fast_placement";
    private static final Component COMPONENT_ON = Component.empty()
            .append(CommonComponents.OPTION_ON)
            .withStyle(ChatFormatting.GREEN);
    private static final Component COMPONENT_OFF = Component.empty()
            .append(CommonComponents.OPTION_OFF)
            .withStyle(ChatFormatting.RED);

    private static boolean isFastPlacementActive;

    public static void onStartClientTick(Minecraft minecraft) {

        while (KeyBindingHandler.KEY_TOGGLE_FAST_PLACEMENT.consumeClick()) {

            isFastPlacementActive =
                    !isFastPlacementActive && ProPlacer.CONFIG.get(ClientConfig.class).allowFastPlacement;
            minecraft.gui.setOverlayMessage(Component.translatable(KEY_FAST_PLACEMENT_MESSAGE,
                    isFastPlacementActive ? COMPONENT_ON : COMPONENT_OFF
            ), false);
        }
    }

    public static void onLoadComplete() {
        // set this only once during launch, not on every config reload
        isFastPlacementActive = ProPlacer.CONFIG.get(ClientConfig.class).allowFastPlacement &&
                ProPlacer.CONFIG.get(ClientConfig.class).defaultFastPlacement;
    }

    public static boolean isFastPlacementActive() {
        return isFastPlacementActive && ProPlacer.CONFIG.get(ClientConfig.class).allowFastPlacement;
    }
}
