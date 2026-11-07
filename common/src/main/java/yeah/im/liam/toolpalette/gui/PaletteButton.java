package yeah.im.liam.toolpalette.gui;

import java.util.function.IntSupplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** A small button in the inventory that opens the colour picker or the gradient helper, following the inventory panel. */
public final class PaletteButton extends AbstractWidget {
    private final Screen inventory;
    private final boolean gradient;
    private final IntSupplier x;
    private final IntSupplier y;

    public PaletteButton(Screen inventory, boolean gradient, IntSupplier x, IntSupplier y) {
        super(x.getAsInt(), y.getAsInt(), 20, 18, Component.translatable(gradient
            ? "builders_tool_palette.tab.gradient_helper" : "builders_tool_palette.tab.color_picker"));
        this.inventory = inventory;
        this.gradient = gradient;
        this.x = x;
        this.y = y;
        setTooltip(Tooltip.create(getMessage()));
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        setX(x.getAsInt());
        setY(y.getAsInt());
        Draw.button(g, getX(), getY(), getX() + width, getY() + height, isHoveredOrFocused());
        Draw.icon(g, getX() + 4, getY() + 4, gradient);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        Screens.set(new PaletteScreen(inventory, gradient));
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
    }
}
