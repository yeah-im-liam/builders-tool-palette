package yeah.im.liam.toolpalette.color;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jspecify.annotations.Nullable;

/**
 * Average colours of texture files: either from the applied texture packs, or from the default textures (the game's or
 * the mod's own, the bottom of each file's pack stack).
 */
final class TextureAverages {
    /** Alpha-weighted mean colour in linear light, the mean alpha, and whether every pixel is fully opaque. */
    record Average(float r, float g, float b, float coverage, boolean opaque) {
    }

    private static final Average NONE = new Average(0f, 0f, 0f, 0f, false);
    private final Map<Identifier, Average> cache = new HashMap<>();
    private final boolean packs;

    TextureAverages(boolean packs) {
        this.packs = packs;
    }

    /** The average of the first frame ({@code frameWidth} by {@code frameHeight}) of a texture, or null if it can't be read. */
    @Nullable Average of(Identifier texture, int frameWidth, int frameHeight) {
        // A pack may change a texture's size, so the frame size of the sprite only fits the top texture.
        Average average = cache.computeIfAbsent(texture, id -> packs ? read(id, frameWidth, frameHeight) : read(id, 0, 0));
        return average == NONE ? null : average;
    }

    private Average read(Identifier texture, int frameWidth, int frameHeight) {
        Identifier file = Identifier.fromNamespaceAndPath(texture.getNamespace(), "textures/" + texture.getPath() + ".png");
        ResourceManager manager = Minecraft.getInstance().getResourceManager();
        Optional<Resource> resource;
        if (packs) {
            resource = manager.getResource(file);
        } else {
            List<Resource> stack = manager.getResourceStack(file);
            resource = stack.isEmpty() ? Optional.empty() : Optional.of(stack.get(0));
        }
        if (resource.isEmpty()) {
            return NONE;
        }
        try (InputStream in = resource.get().open(); NativeImage image = NativeImage.read(in)) {
            int w = frameWidth > 0 ? Math.min(frameWidth, image.getWidth()) : image.getWidth();
            int h = frameHeight > 0 ? Math.min(frameHeight, image.getHeight()) : Math.min(image.getWidth(), image.getHeight());
            double r = 0;
            double g = 0;
            double b = 0;
            double alpha = 0;
            boolean opaque = true;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int argb = image.getPixel(x, y);
                    int a = argb >>> 24;
                    if (a < 255) {
                        opaque = false;
                    }
                    if (a == 0) {
                        continue;
                    }
                    float weight = a / 255f;
                    r += ColorMath.toLinear(argb >> 16 & 0xFF) * weight;
                    g += ColorMath.toLinear(argb >> 8 & 0xFF) * weight;
                    b += ColorMath.toLinear(argb & 0xFF) * weight;
                    alpha += weight;
                }
            }
            if (alpha <= 0) {
                return NONE;
            }
            return new Average((float) (r / alpha), (float) (g / alpha), (float) (b / alpha), (float) (alpha / (w * h)), opaque);
        } catch (Exception e) {
            return NONE;
        }
    }
}
