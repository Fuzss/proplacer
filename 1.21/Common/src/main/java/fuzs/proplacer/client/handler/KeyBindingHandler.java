package fuzs.proplacer.client.handler;

import fuzs.proplacer.ProPlacer;
import fuzs.proplacer.config.ClientConfig;
import fuzs.puzzleslib.api.client.core.v1.context.KeyMappingsContext;
import fuzs.puzzleslib.api.client.key.v1.KeyActivationHandler;
import fuzs.puzzleslib.api.client.key.v1.KeyMappingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class KeyBindingHandler {
    public static final KeyMapping KEY_TOGGLE_FAST_PLACEMENT = KeyMappingHelper.registerUnboundKeyMapping(ProPlacer.id(
            "fast_placement"));
    public static final String KEY_FAST_PLACEMENT_MESSAGE = "gui.fast_placement";
    private static final Component COMPONENT_ON = Component.empty()
            .append(CommonComponents.OPTION_ON)
            .withStyle(ChatFormatting.GREEN);
    private static final Component COMPONENT_OFF = Component.empty()
            .append(CommonComponents.OPTION_OFF)
            .withStyle(ChatFormatting.RED);

    private static boolean isFastPlacementActive;

    public static void onRegisterKeyMappings(KeyMappingsContext context) {
        context.registerKeyMapping(KeyBindingHandler.KEY_TOGGLE_FAST_PLACEMENT,
                KeyActivationHandler.forGame(minecraft -> {
                    isFastPlacementActive = !isFastPlacementActive;
                    minecraft.gui.setOverlayMessage(Component.translatable(KEY_FAST_PLACEMENT_MESSAGE,
                            isFastPlacementActive ? COMPONENT_ON : COMPONENT_OFF
                    ), false);
                })
        );
    }

    public static void onLoadComplete() {
        // set this only once during launch, not on every config reload
        isFastPlacementActive = ProPlacer.CONFIG.get(ClientConfig.class).allowFastPlacement;
    }

    public static boolean isFastPlacementActive() {
        return isFastPlacementActive;
    }
}
