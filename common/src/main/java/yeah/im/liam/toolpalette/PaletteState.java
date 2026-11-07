package yeah.im.liam.toolpalette;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Properties;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;
import yeah.im.liam.toolpalette.color.ColorMath;
import yeah.im.liam.toolpalette.color.PaletteIndex;

/** What the palette screen shows, kept between openings and saved in config/builders_tool_palette.properties. */
public final class PaletteState {
    public static final int GRADIENT_SLOTS = 9;

    private static @Nullable PaletteState instance;

    public boolean gradientTab;
    public float hue;
    public float saturation;
    public float value;
    public ItemStack eyedropper = ItemStack.EMPTY;
    public boolean fullCube;
    public boolean solid;
    public boolean opaque;
    public boolean sameTexture;
    /** Whether block colours come from the applied texture packs rather than the default textures. */
    public boolean packColors;
    public final ItemStack[] gradient = new ItemStack[GRADIENT_SLOTS];
    /** For each gradient slot, which match is shown: 0 is the closest. */
    public final int[] choice = new int[GRADIENT_SLOTS];

    private PaletteState() {
        Arrays.fill(gradient, ItemStack.EMPTY);
        setColor(0x5B8731);
    }

    public static PaletteState get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    public int color() {
        return ColorMath.fromHsv(hue, saturation, value);
    }

    /** Sets the colour, keeping the hue when the new colour is grey and the saturation when it is black. */
    public void setColor(int rgb) {
        float[] hsv = ColorMath.hsv(rgb);
        if (hsv[2] > 0f) {
            if (hsv[1] > 0f) {
                hue = hsv[0];
            }
            saturation = hsv[1];
        }
        value = hsv[2];
    }

    public PaletteIndex.Filters filters() {
        return new PaletteIndex.Filters(fullCube, solid, opaque, sameTexture);
    }

    private static Path file() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve(ToolPalette.MOD_ID + ".properties");
    }

    private static PaletteState load() {
        PaletteState state = new PaletteState();
        Path file = file();
        if (!Files.isRegularFile(file)) {
            return state;
        }
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException e) {
            return state;
        }
        state.gradientTab = "gradient".equals(properties.getProperty("tab"));
        int rgb = ColorMath.parseHex(properties.getProperty("color", ""));
        if (rgb >= 0) {
            state.setColor(rgb);
        }
        state.hue = parseFloat(properties.getProperty("hue"), state.hue);
        state.eyedropper = stack(properties.getProperty("eyedropper"));
        state.fullCube = Boolean.parseBoolean(properties.getProperty("filter.full_cube"));
        state.solid = Boolean.parseBoolean(properties.getProperty("filter.solid"));
        state.opaque = Boolean.parseBoolean(properties.getProperty("filter.opaque"));
        state.sameTexture = Boolean.parseBoolean(properties.getProperty("filter.same_texture"));
        state.packColors = Boolean.parseBoolean(properties.getProperty("pack_colors"));
        for (int i = 0; i < GRADIENT_SLOTS; i++) {
            state.gradient[i] = stack(properties.getProperty("gradient." + i));
        }
        return state;
    }

    public void save() {
        Properties properties = new Properties();
        properties.setProperty("tab", gradientTab ? "gradient" : "color");
        properties.setProperty("color", ColorMath.hex(color()));
        properties.setProperty("hue", Float.toString(hue));
        properties.setProperty("eyedropper", id(eyedropper));
        properties.setProperty("filter.full_cube", Boolean.toString(fullCube));
        properties.setProperty("filter.solid", Boolean.toString(solid));
        properties.setProperty("filter.opaque", Boolean.toString(opaque));
        properties.setProperty("filter.same_texture", Boolean.toString(sameTexture));
        properties.setProperty("pack_colors", Boolean.toString(packColors));
        for (int i = 0; i < GRADIENT_SLOTS; i++) {
            properties.setProperty("gradient." + i, id(gradient[i]));
        }
        Path file = file();
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                properties.store(writer, "Builder's Tool Palette");
            }
        } catch (IOException e) {
            ToolPalette.LOGGER.warn("Could not save {}", file, e);
        }
    }

    private static float parseFloat(@Nullable String text, float fallback) {
        try {
            return text == null ? fallback : Float.parseFloat(text);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static String id(ItemStack stack) {
        return stack.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    private static ItemStack stack(@Nullable String id) {
        if (id == null || id.isEmpty()) {
            return ItemStack.EMPTY;
        }
        Identifier key = Identifier.tryParse(id);
        if (key == null) {
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.ITEM.getOptional(key).orElse(Items.AIR);
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }
}
