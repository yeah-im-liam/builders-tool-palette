package yeah.im.liam.toolpalette.gui;

import java.util.function.BooleanSupplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/**
 * Switches block colours between the default textures and the applied texture packs. Off, it is the game's button; on,
 * the mod's selected_button, cut up the same way. The pack_colors_on or pack_colors_off picture is drawn over it.
 */
final class PackColorsButton extends Button {
    static final int WIDTH = 20;

    private final BooleanSupplier on;

    PackColorsButton(int x, int y, BooleanSupplier on, Runnable toggle) {
        super(x, y, WIDTH, 20, Component.translatable("builders_tool_palette.pack_colors"), button -> toggle.run(), narration -> narration.get());
        this.on = on;
        setTooltip(Tooltip.create(Component.empty().append(getMessage()).append("\n")
            .append(Component.translatable("builders_tool_palette.pack_colors.description").withStyle(ChatFormatting.GRAY))));
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        if (on.getAsBoolean()) {
            Textures.frame(g, isHoveredOrFocused() ? Textures.SELECTED_BUTTON_HIGHLIGHTED : Textures.SELECTED_BUTTON, getX(), getY(), width, height);
            Textures.draw(g, Textures.PACK_COLORS_ON, getX(), getY(), width, height);
        } else {
            extractDefaultSprite(g);
            Textures.draw(g, Textures.PACK_COLORS_OFF, getX(), getY(), width, height);
        }
    }
}
