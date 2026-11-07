package yeah.im.liam.toolpalette.color;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/** Every placeable block with its average colour, worked out from the textures of its block model. */
public final class PaletteIndex {
    private static final Direction[] FACES_AND_NONE = {
        Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, null
    };

    public record Entry(Item item, ItemStack stack, int rgb, float[] lab, boolean fullCube, boolean solid, boolean opaque,
                        boolean sameTexture, String search) {
        public boolean passes(Filters filters) {
            return (!filters.fullCube() || fullCube) && (!filters.solid() || solid) && (!filters.opaque() || opaque)
                && (!filters.sameTexture() || sameTexture);
        }
    }

    public record Filters(boolean fullCube, boolean solid, boolean opaque, boolean sameTexture) {
    }

    private static List<Entry> entries = List.of();
    private static Map<Item, Entry> byItem = Map.of();
    private static final Map<Item, Integer> itemColors = new HashMap<>();
    private static @Nullable Object builtFor;

    private PaletteIndex() {
    }

    /** All blocks, sorted by colour: greys from dark to light, then by hue. */
    public static List<Entry> entries() {
        Minecraft minecraft = Minecraft.getInstance();
        BlockStateModelSet models = minecraft.getModelManager().getBlockStateModelSet();
        FeatureFlagSet features = minecraft.level != null ? minecraft.level.enabledFeatures() : FeatureFlags.DEFAULT_FLAGS;
        List<Object> key = List.of(models, features, minecraft.getLanguageManager().getSelected());
        if (!key.equals(builtFor)) {
            build(minecraft, models, features);
            builtFor = key;
        }
        return entries;
    }

    public static @Nullable Entry entry(Item item) {
        entries();
        return byItem.get(item);
    }

    /** The colour of a block item's block, or for other items the average of their item texture; -1 if unknown. */
    public static int colorOf(ItemStack stack) {
        if (stack.isEmpty()) {
            return -1;
        }
        Entry entry = entry(stack.getItem());
        if (entry != null) {
            return entry.rgb();
        }
        return itemColors.computeIfAbsent(stack.getItem(), item -> {
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            TextureAverages.Average average = new TextureAverages().of(id.withPath(path -> "item/" + path), 0, 0);
            return average == null ? -1 : ColorMath.fromLinear(average.r(), average.g(), average.b());
        });
    }

    private static void build(Minecraft minecraft, BlockStateModelSet models, FeatureFlagSet features) {
        TextureAverages textures = new TextureAverages();
        List<Entry> list = new ArrayList<>();
        Map<Item, Entry> map = new HashMap<>();
        List<BlockStateModelPart> parts = new ArrayList<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            Item item = block.asItem();
            if (!(item instanceof BlockItem blockItem) || blockItem.getBlock() != block || !item.isEnabled(features)) {
                continue;
            }
            if (block instanceof GameMasterBlock || BuiltInRegistries.BLOCK.getKey(block).getPath().startsWith("infested_")) {
                continue;
            }
            BlockState state = block.defaultBlockState();
            parts.clear();
            try {
                models.get(state).collectParts(RandomSource.create(42L), parts);
            } catch (Exception e) {
                continue;
            }
            double r = 0;
            double g = 0;
            double b = 0;
            double weight = 0;
            boolean texturesOpaque = true;
            Set<Identifier> sprites = new HashSet<>();
            for (BlockStateModelPart part : parts) {
                for (Direction face : FACES_AND_NONE) {
                    for (BakedQuad quad : part.getQuads(face)) {
                        TextureAtlasSprite sprite = quad.materialInfo().sprite();
                        Identifier name = sprite.contents().name();
                        TextureAverages.Average average = textures.of(name, sprite.contents().width(), sprite.contents().height());
                        if (average == null) {
                            continue;
                        }
                        sprites.add(name);
                        texturesOpaque &= average.opaque();
                        float area = area(quad);
                        float w = area * average.coverage();
                        if (w <= 0f) {
                            continue;
                        }
                        float tr = 1f;
                        float tg = 1f;
                        float tb = 1f;
                        int tint = tint(minecraft, state, quad.materialInfo().tintIndex());
                        if (tint != -1) {
                            tr = ColorMath.toLinear(ColorMath.red(tint));
                            tg = ColorMath.toLinear(ColorMath.green(tint));
                            tb = ColorMath.toLinear(ColorMath.blue(tint));
                        }
                        r += average.r() * tr * w;
                        g += average.g() * tg * w;
                        b += average.b() * tb * w;
                        weight += w;
                    }
                }
            }
            if (weight <= 0) {
                continue;
            }
            int rgb = ColorMath.fromLinear((float) (r / weight), (float) (g / weight), (float) (b / weight));
            ItemStack stack = new ItemStack(item);
            String search = (stack.getHoverName().getString() + " " + BuiltInRegistries.ITEM.getKey(item)).toLowerCase(Locale.ROOT);
            Entry entry = new Entry(item, stack, rgb, ColorMath.oklab(rgb), fullCube(state), solid(state),
                texturesOpaque && opaque(state), sprites.size() == 1, search);
            list.add(entry);
            map.put(item, entry);
        }
        list.sort(BY_COLOR);
        entries = Collections.unmodifiableList(list);
        byItem = map;
        itemColors.clear();
    }

    private static final Comparator<Entry> BY_COLOR = Comparator
        .comparingInt((Entry e) -> chroma(e.lab()) < 0.035f ? 0 : 1)
        .thenComparingInt(e -> chroma(e.lab()) < 0.035f ? 0 : hueBucket(e.lab()))
        .thenComparingDouble(e -> e.lab()[0]);

    private static float chroma(float[] lab) {
        return (float) Math.sqrt(lab[1] * lab[1] + lab[2] * lab[2]);
    }

    /** One of 24 hue steps, starting at red. */
    private static int hueBucket(float[] lab) {
        double degrees = Math.toDegrees(Math.atan2(lab[2], lab[1]));
        double fromRed = (degrees - 20 + 360) % 360;
        return (int) (fromRed / 15);
    }

    private static float area(BakedQuad quad) {
        Vector3fc p0 = quad.position(0);
        Vector3fc p1 = quad.position(1);
        Vector3fc p3 = quad.position(3);
        float ax = p1.x() - p0.x();
        float ay = p1.y() - p0.y();
        float az = p1.z() - p0.z();
        float bx = p3.x() - p0.x();
        float by = p3.y() - p0.y();
        float bz = p3.z() - p0.z();
        float cx = ay * bz - az * by;
        float cy = az * bx - ax * bz;
        float cz = ax * by - ay * bx;
        return (float) Math.sqrt(cx * cx + cy * cy + cz * cz);
    }

    private static int tint(Minecraft minecraft, BlockState state, int tintIndex) {
        if (tintIndex < 0) {
            return -1;
        }
        try {
            BlockTintSource source = minecraft.getBlockColors().getTintSource(state, tintIndex);
            return source == null ? -1 : source.color(state) & 0xFFFFFF;
        } catch (Exception e) {
            return -1;
        }
    }

    private static boolean fullCube(BlockState state) {
        try {
            return Block.isShapeFullBlock(state.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean solid(BlockState state) {
        try {
            return !state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean opaque(BlockState state) {
        try {
            return state.canOcclude();
        } catch (Exception e) {
            return false;
        }
    }

    /** The blocks closest in colour to {@code lab} that pass the filters, nearest first. */
    public static List<Entry> nearest(float[] lab, Filters filters, int count) {
        List<Entry> candidates = new ArrayList<>();
        for (Entry entry : entries()) {
            if (entry.passes(filters)) {
                candidates.add(entry);
            }
        }
        candidates.sort(Comparator.comparingDouble(e -> ColorMath.distanceSquared(e.lab(), lab)));
        return candidates.subList(0, Math.min(count, candidates.size()));
    }
}
