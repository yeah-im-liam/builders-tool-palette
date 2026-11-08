package yeah.im.liam.toolpalette.gui;

import java.util.function.BooleanSupplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/**
 * Switches block colours between the default textures and the applied texture packs: the game's button with the
 * pack_colors_on or pack_colors_off picture drawn over it.
 */
final class PackColorsButton extends Button {
    static final int WIDTH = 20;

    private final BooleanSupplier on;

    PackColorsButton(int x, int y, BooleanSupplier on, Runnable toggle) {
        super(x, y, WIDTH, 20, Component.translatable("builders_tool_palette.pack_colors"), button -> {
            toggle.run();
            ((PackColorsButton) button).updateTooltip();
        }, narration -> narration.get());
        this.on = on;
        updateTooltip();
    }

    private void updateTooltip() {
        String state = on.getAsBoolean() ? "on" : "off";
        setTooltip(Tooltip.create(Component.empty().append(getMessage()).append("\n")
            .append(Component.translatable("builders_tool_palette.pack_colors." + state).withStyle(ChatFormatting.GRAY))));
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        extractDefaultSprite(g);
        Textures.draw(g, on.getAsBoolean() ? Textures.PACK_COLORS_ON : Textures.PACK_COLORS_OFF, getX(), getY(), width, height);
    }
}
