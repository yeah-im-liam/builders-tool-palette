package yeah.im.liam.toolpalette.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.KeyMapping;
import yeah.im.liam.toolpalette.ToolPalette;

public final class ToolPaletteFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(ToolPalette.KEY_CATEGORY);
        ToolPalette.createKeys(category).forEach(KeyMappingHelper::registerKeyMapping);
        ClientTickEvents.END_CLIENT_TICK.register(ToolPalette::tick);
        ScreenEvents.AFTER_INIT.register((minecraft, screen, width, height) ->
            ToolPalette.addInventoryButtons(screen, Screens.getWidgets(screen)::add));
    }
}
