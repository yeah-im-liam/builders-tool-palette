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
import net.minecraft.resources.Identifier;

/**
 * A button in the inventory that opens the colour picker or the gradient helper. It is as big as its texture and sits 2
 * pixels left of the given x, following the inventory panel.
 */
public final class PaletteButton extends AbstractWidget {
    private final Screen inventory;
    private final boolean gradient;
    private final IntSupplier right;
    private final IntSupplier y;

    public PaletteButton(Screen inventory, boolean gradient, IntSupplier right, IntSupplier y) {
        super(0, y.getAsInt(), Textures.width(texture(gradient, false)), Textures.height(texture(gradient, false)),
            Component.translatable(gradient ? "builders_tool_palette.tab.gradient_helper" : "builders_tool_palette.tab.color_picker"));
        this.inventory = inventory;
        this.gradient = gradient;
        this.right = right;
        this.y = y;
        setX(right.getAsInt() - width - 2);
        setTooltip(Tooltip.create(getMessage()));
    }

    private static Identifier texture(boolean gradient, boolean hovered) {
        return gradient
            ? hovered ? Textures.GRADIENT_HELPER_BUTTON_HOVERED : Textures.GRADIENT_HELPER_BUTTON
            : hovered ? Textures.COLOR_PICKER_BUTTON_HOVERED : Textures.COLOR_PICKER_BUTTON;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        setX(right.getAsInt() - width - 2);
        setY(y.getAsInt());
        Textures.draw(g, texture(gradient, isHoveredOrFocused()), getX(), getY(), width, height);
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
