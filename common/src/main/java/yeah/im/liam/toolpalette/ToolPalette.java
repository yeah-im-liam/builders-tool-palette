package yeah.im.liam.toolpalette;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import yeah.im.liam.toolpalette.gui.PaletteButton;
import yeah.im.liam.toolpalette.gui.PaletteScreen;
import yeah.im.liam.toolpalette.gui.Screens;
import yeah.im.liam.toolpalette.mixin.AbstractContainerScreenAccessor;

public final class ToolPalette {
    public static final String MOD_ID = "builders_tool_palette";
    public static final Logger LOGGER = LoggerFactory.getLogger("Builder's Tool Palette");
    public static final Identifier KEY_CATEGORY = Identifier.fromNamespaceAndPath(MOD_ID, "palette");

    private static @Nullable KeyMapping colorPickerKey;
    private static @Nullable KeyMapping gradientKey;

    private ToolPalette() {
    }

    /** The key mappings, unbound by default; the loaders register them. */
    public static List<KeyMapping> createKeys(KeyMapping.Category category) {
        colorPickerKey = new KeyMapping("key.builders_tool_palette.color_picker", InputConstants.UNKNOWN.getValue(), category);
        gradientKey = new KeyMapping("key.builders_tool_palette.gradient_helper", InputConstants.UNKNOWN.getValue(), category);
        return List.of(colorPickerKey, gradientKey);
    }

    public static boolean isOpenKey(KeyEvent event) {
        return colorPickerKey != null && colorPickerKey.matches(event) || gradientKey != null && gradientKey.matches(event);
    }

    public static void tick(Minecraft minecraft) {
        if (colorPickerKey == null || gradientKey == null) {
            return;
        }
        while (colorPickerKey.consumeClick()) {
            openFromWorld(minecraft, false);
        }
        while (gradientKey.consumeClick()) {
            openFromWorld(minecraft, true);
        }
    }

    private static void openFromWorld(Minecraft minecraft, boolean gradient) {
        if (minecraft.player != null && Screens.current() == null) {
            Screens.set(new PaletteScreen(null, gradient));
        }
    }

    /** Adds the two palette buttons to the survival and creative inventories. */
    public static void addInventoryButtons(Screen screen, Consumer<AbstractWidget> add) {
        if (screen instanceof CreativeModeInventoryScreen) {
            AbstractContainerScreenAccessor panel = (AbstractContainerScreenAccessor) screen;
            add.accept(new PaletteButton(screen, false, () -> panel.toolpalette$leftPos() - 22, () -> panel.toolpalette$topPos() + 4));
            add.accept(new PaletteButton(screen, true, () -> panel.toolpalette$leftPos() - 22, () -> panel.toolpalette$topPos() + 24));
        } else if (screen instanceof InventoryScreen) {
            // Beside the recipe book button, which moves with the panel when the book opens.
            AbstractContainerScreenAccessor panel = (AbstractContainerScreenAccessor) screen;
            add.accept(new PaletteButton(screen, false, () -> panel.toolpalette$leftPos() + 126, () -> panel.toolpalette$topPos() + 61));
            add.accept(new PaletteButton(screen, true, () -> panel.toolpalette$leftPos() + 148, () -> panel.toolpalette$topPos() + 61));
        }
    }
}
