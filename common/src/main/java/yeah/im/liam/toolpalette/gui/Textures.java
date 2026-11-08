package yeah.im.liam.toolpalette.gui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import org.jspecify.annotations.Nullable;
import yeah.im.liam.toolpalette.ToolPalette;

/**
 * The mod's GUI textures (assets/builders_tool_palette/textures/gui). They are plain image files drawn whole, because the
 * game's sprite drawing changed between 26.1 and 26.3; the menu frame is cut into nine parts by hand.
 */
final class Textures {
    static final Identifier COLOR_PICKER_BUTTON = id("color_picker_button");
    static final Identifier COLOR_PICKER_BUTTON_HOVERED = id("color_picker_button_hovered");
    static final Identifier GRADIENT_HELPER_BUTTON = id("gradient_helper_button");
    static final Identifier GRADIENT_HELPER_BUTTON_HOVERED = id("gradient_helper_button_hovered");
    static final Identifier PACK_COLORS_OFF = id("pack_colors_off");
    static final Identifier PACK_COLORS_ON = id("pack_colors_on");
    static final Identifier MENU_FRAME = id("menu_frame");

    /**
     * How a frame texture is cut: its size and borders in GUI pixels (from the .mcmeta, like the game's nine_slice sprites,
     * or the image size), and whether each edge and the middle are one colour along their length, so they can be stretched
     * in one go instead of repeated.
     */
    private record Frame(int width, int height, int left, int top, int right, int bottom,
                         boolean stretchTop, boolean stretchBottom, boolean stretchLeft, boolean stretchRight, boolean stretchMiddle) {
    }

    private record Size(int width, int height) {
    }

    private static final Map<Identifier, Frame> FRAMES = new HashMap<>();
    private static final Map<Identifier, Size> SIZES = new HashMap<>();
    private static @Nullable Object loadedFor;

    private Textures() {
    }

    private static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath(ToolPalette.MOD_ID, "textures/gui/" + name + ".png");
    }

    static void draw(GuiGraphicsExtractor g, Identifier texture, int x, int y, int width, int height) {
        g.blit(texture, x, y, x + width, y + height, 0f, 1f, 0f, 1f);
    }

    /** Draws a frame texture over the box: corners as drawn, edges and middle repeated (or stretched when plain). */
    static void frame(GuiGraphicsExtractor g, Identifier texture, int x, int y, int width, int height) {
        Frame f = frameOf(texture);
        int innerW = width - f.left() - f.right();
        int innerH = height - f.top() - f.bottom();
        int sourceW = f.width() - f.left() - f.right();
        int sourceH = f.height() - f.top() - f.bottom();
        part(g, texture, f, x, y, f.left(), f.top(), 0, 0);
        part(g, texture, f, x + width - f.right(), y, f.right(), f.top(), f.width() - f.right(), 0);
        part(g, texture, f, x, y + height - f.bottom(), f.left(), f.bottom(), 0, f.height() - f.bottom());
        part(g, texture, f, x + width - f.right(), y + height - f.bottom(), f.right(), f.bottom(), f.width() - f.right(), f.height() - f.bottom());
        fillArea(g, texture, f, x + f.left(), y, innerW, f.top(), f.left(), 0, sourceW, f.top(), f.stretchTop(), false);
        fillArea(g, texture, f, x + f.left(), y + height - f.bottom(), innerW, f.bottom(), f.left(), f.height() - f.bottom(), sourceW, f.bottom(),
            f.stretchBottom(), false);
        fillArea(g, texture, f, x, y + f.top(), f.left(), innerH, 0, f.top(), f.left(), sourceH, false, f.stretchLeft());
        fillArea(g, texture, f, x + width - f.right(), y + f.top(), f.right(), innerH, f.width() - f.right(), f.top(), f.right(), sourceH,
            false, f.stretchRight());
        fillArea(g, texture, f, x + f.left(), y + f.top(), innerW, innerH, f.left(), f.top(), sourceW, sourceH, f.stretchMiddle(), f.stretchMiddle());
    }

    /** Copies the source rectangle (sx, sy, w, h) of the texture to (x, y) at the same size. */
    private static void part(GuiGraphicsExtractor g, Identifier texture, Frame f, int x, int y, int w, int h, int sx, int sy) {
        if (w > 0 && h > 0) {
            g.blit(texture, x, y, x + w, y + h, sx / (float) f.width(), (sx + w) / (float) f.width(), sy / (float) f.height(),
                (sy + h) / (float) f.height());
        }
    }

    /** Covers (x, y, w, h) with the source rectangle, stretched along x or y where asked and repeated otherwise. */
    private static void fillArea(GuiGraphicsExtractor g, Identifier texture, Frame f, int x, int y, int w, int h, int sx, int sy, int sw, int sh,
                                 boolean stretchX, boolean stretchY) {
        if (w <= 0 || h <= 0 || sw <= 0 || sh <= 0) {
            return;
        }
        int stepX = stretchX ? w : sw;
        int stepY = stretchY ? h : sh;
        for (int dy = 0; dy < h; dy += stepY) {
            int ch = Math.min(stepY, h - dy);
            float v0 = sy / (float) f.height();
            float v1 = (sy + (stretchY ? sh : ch)) / (float) f.height();
            for (int dx = 0; dx < w; dx += stepX) {
                int cw = Math.min(stepX, w - dx);
                float u0 = sx / (float) f.width();
                float u1 = (sx + (stretchX ? sw : cw)) / (float) f.width();
                g.blit(texture, x + dx, y + dy, x + dx + cw, y + dy + ch, u0, u1, v0, v1);
            }
        }
    }

    /** The texture's size in GUI pixels: the image size, or the .mcmeta's width and height for higher-resolution images. */
    static int width(Identifier texture) {
        return sizeOf(texture).width();
    }

    static int height(Identifier texture) {
        return sizeOf(texture).height();
    }

    private static void forgetOnReload() {
        // The block model set is rebuilt on every resource reload, so it tells when packs may have changed the textures.
        Object marker = Minecraft.getInstance().getModelManager().getBlockStateModelSet();
        if (marker != loadedFor) {
            FRAMES.clear();
            SIZES.clear();
            loadedFor = marker;
        }
    }

    private static Frame frameOf(Identifier texture) {
        forgetOnReload();
        return FRAMES.computeIfAbsent(texture, Textures::loadFrame);
    }

    private static Size sizeOf(Identifier texture) {
        forgetOnReload();
        return SIZES.computeIfAbsent(texture, Textures::loadSize);
    }

    private static Size loadSize(Identifier texture) {
        Optional<Resource> image = Minecraft.getInstance().getResourceManager().getResource(texture);
        if (image.isEmpty()) {
            return new Size(16, 16);
        }
        try (InputStream in = image.get().open(); NativeImage pixels = NativeImage.read(in)) {
            JsonObject scaling = scaling(texture);
            int width = scaling != null && scaling.has("width") ? scaling.get("width").getAsInt() : pixels.getWidth();
            int height = scaling != null && scaling.has("height") ? scaling.get("height").getAsInt() : pixels.getHeight();
            return new Size(width, height);
        } catch (Exception e) {
            ToolPalette.LOGGER.warn("Could not read {}", texture, e);
            return new Size(16, 16);
        }
    }

    private static Frame loadFrame(Identifier texture) {
        Frame fallback = new Frame(16, 16, 4, 4, 4, 4, true, true, true, true, true);
        Optional<Resource> image = Minecraft.getInstance().getResourceManager().getResource(texture);
        if (image.isEmpty()) {
            return fallback;
        }
        try (InputStream in = image.get().open(); NativeImage pixels = NativeImage.read(in)) {
            int pixelW = pixels.getWidth();
            int pixelH = pixels.getHeight();
            int width = pixelW;
            int height = pixelH;
            int left = 4;
            int top = 4;
            int right = 4;
            int bottom = 4;
            JsonObject scaling = scaling(texture);
            if (scaling != null) {
                width = scaling.has("width") ? scaling.get("width").getAsInt() : width;
                height = scaling.has("height") ? scaling.get("height").getAsInt() : height;
                JsonElement border = scaling.get("border");
                if (border != null && border.isJsonPrimitive()) {
                    left = top = right = bottom = border.getAsInt();
                } else if (border != null && border.isJsonObject()) {
                    JsonObject sides = border.getAsJsonObject();
                    left = sides.get("left").getAsInt();
                    top = sides.get("top").getAsInt();
                    right = sides.get("right").getAsInt();
                    bottom = sides.get("bottom").getAsInt();
                }
            }
            // Border sizes in image pixels, for the plain-colour checks.
            float sx = pixelW / (float) width;
            float sy = pixelH / (float) height;
            int pl = Math.round(left * sx);
            int pt = Math.round(top * sy);
            int pr = pixelW - Math.round(right * sx);
            int pb = pixelH - Math.round(bottom * sy);
            return new Frame(width, height, left, top, right, bottom,
                rowsPlain(pixels, pl, pr, 0, pt), rowsPlain(pixels, pl, pr, pb, pixelH),
                columnsPlain(pixels, 0, pl, pt, pb), columnsPlain(pixels, pr, pixelW, pt, pb),
                rowsPlain(pixels, pl, pr, pt, pb) && columnsPlain(pixels, pl, pr, pt, pb));
        } catch (Exception e) {
            ToolPalette.LOGGER.warn("Could not read {}", texture, e);
            return fallback;
        }
    }

    private static @Nullable JsonObject scaling(Identifier texture) {
        Identifier meta = texture.withPath(texture.getPath() + ".mcmeta");
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(meta);
        if (resource.isEmpty()) {
            return null;
        }
        try (Reader reader = new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject gui = root.getAsJsonObject("gui");
            return gui == null ? null : gui.getAsJsonObject("scaling");
        } catch (Exception e) {
            ToolPalette.LOGGER.warn("Could not read {}", meta, e);
            return null;
        }
    }

    /** Whether every row of the area is one colour, so it may be stretched sideways. */
    private static boolean rowsPlain(NativeImage pixels, int x1, int x2, int y1, int y2) {
        for (int y = y1; y < y2; y++) {
            for (int x = x1 + 1; x < x2; x++) {
                if (pixels.getPixel(x, y) != pixels.getPixel(x1, y)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Whether every column of the area is one colour, so it may be stretched up and down. */
    private static boolean columnsPlain(NativeImage pixels, int x1, int x2, int y1, int y2) {
        for (int x = x1; x < x2; x++) {
            for (int y = y1 + 1; y < y2; y++) {
                if (pixels.getPixel(x, y) != pixels.getPixel(x, y1)) {
                    return false;
                }
            }
        }
        return true;
    }
}
