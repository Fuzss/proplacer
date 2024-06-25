package fuzs.proplacer.data.client;

import fuzs.proplacer.ProPlacer;
import fuzs.proplacer.client.handler.KeyMappingHandler;
import fuzs.puzzleslib.api.client.data.v2.AbstractLanguageProvider;
import fuzs.puzzleslib.api.data.v2.core.DataProviderContext;

public class ModLanguageProvider extends AbstractLanguageProvider {

    public ModLanguageProvider(DataProviderContext context) {
        super(context);
    }

    @Override
    protected void addTranslations(TranslationBuilder builder) {
        builder.add(KeyMappingHandler.KEY_CATEGORY, ProPlacer.MOD_NAME);
        builder.add(KeyMappingHandler.KEY_TOGGLE_FAST_PLACEMENT, "Toggle Fast Block Placement");
        builder.add(KeyMappingHandler.KEY_FAST_PLACEMENT_MESSAGE, "Fast Block Placement: %s");
    }
}
