package yeah.im.liam.toolpalette.gui;

import java.util.function.BooleanSupplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Switches block colours between the default textures and the applied texture packs. */
final class PackColorsButton extends AbstractWidget {
    static final int WIDTH = 20;

    private final BooleanSupplier on;
    private final Runnable toggle;

    PackColorsButton(int x, int y, BooleanSupplier on, Runnable toggle) {
        super(x, y, WIDTH, 20, Component.translatable("builders_tool_palette.pack_colors"));
        this.on = on;
        this.toggle = toggle;
        updateTooltip();
    }

    private void updateTooltip() {
        String state = on.getAsBoolean() ? "on" : "off";
        setTooltip(Tooltip.create(Component.empty().append(getMessage()).append("\n")
            .append(Component.translatable("builders_tool_palette.pack_colors." + state).withStyle(ChatFormatting.GRAY))));
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        boolean hovered = isHoveredOrFocused();
        Identifier texture = on.getAsBoolean()
            ? hovered ? Textures.PACK_COLORS_ON_HOVERED : Textures.PACK_COLORS_ON
            : hovered ? Textures.PACK_COLORS_OFF_HOVERED : Textures.PACK_COLORS_OFF;
        Textures.draw(g, texture, getX(), getY(), width, height);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        toggle.run();
        updateTooltip();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
    }
}
