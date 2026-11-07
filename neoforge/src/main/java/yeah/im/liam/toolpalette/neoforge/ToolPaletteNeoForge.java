package yeah.im.liam.toolpalette.neoforge;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import yeah.im.liam.toolpalette.ToolPalette;

@Mod(value = ToolPalette.MOD_ID, dist = Dist.CLIENT)
public final class ToolPaletteNeoForge {
    public ToolPaletteNeoForge(IEventBus modBus) {
        modBus.addListener(RegisterKeyMappingsEvent.class, event -> {
            KeyMapping.Category category = new KeyMapping.Category(ToolPalette.KEY_CATEGORY);
            event.registerCategory(category);
            ToolPalette.createKeys(category).forEach(event::register);
        });
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> ToolPalette.tick(Minecraft.getInstance()));
        NeoForge.EVENT_BUS.addListener(ScreenEvent.Init.Post.class, event ->
            ToolPalette.addInventoryButtons(event.getScreen(), event::addListener));
    }
}
