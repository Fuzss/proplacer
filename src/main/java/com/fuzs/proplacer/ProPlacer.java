package com.fuzs.proplacer;

import com.fuzs.proplacer.client.FastPlacementHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@SuppressWarnings({"WeakerAccess", "unused"})
@Mod(ProPlacer.MODID)
public class ProPlacer {

    public static final String MODID = "proplacer";
    public static final String NAME = "Pro Placer";
    public static final Logger LOGGER = LogManager.getLogger(ProPlacer.NAME);

    public ProPlacer() {

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onClientSetup);
    }

    private void onClientSetup(final FMLClientSetupEvent evt) {

        MinecraftForge.EVENT_BUS.register(new FastPlacementHandler());
    }

}