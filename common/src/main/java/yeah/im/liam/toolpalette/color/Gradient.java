package yeah.im.liam.toolpalette.color;

import java.util.List;
import net.minecraft.world.item.ItemStack;

/** Fills the empty slots between the blocks placed in a row with the blocks closest to an OKLab blend of them. */
public final class Gradient {
    /**
     * One slot of the result. {@code target} is the colour the slot aims for (-1 outside the gradient), {@code input} says
     * whether the slot holds one of the placed blocks, and {@code last} whether no worse match is left to step to.
     */
    public record Slot(ItemStack stack, int target, boolean input, boolean last) {
        static final Slot NONE = new Slot(ItemStack.EMPTY, -1, false, true);
    }

    private Gradient() {
    }

    /** {@code choice[i]} picks the match for slot i: 0 is the closest, 1 the next closest and so on; it is lowered to the last match. */
    public static Slot[] compute(ItemStack[] inputs, int[] choice, PaletteIndex.Filters filters) {
        int n = inputs.length;
        Slot[] slots = new Slot[n];
        float[][] labs = new float[n][];
        for (int i = 0; i < n; i++) {
            slots[i] = Slot.NONE;
            int rgb = PaletteIndex.colorOf(inputs[i]);
            if (rgb >= 0) {
                labs[i] = ColorMath.oklab(rgb);
                slots[i] = new Slot(inputs[i], rgb, true, true);
            } else if (!inputs[i].isEmpty()) {
                slots[i] = new Slot(inputs[i], -1, true, true);
            }
        }
        int previous = -1;
        for (int i = 0; i < n; i++) {
            if (labs[i] == null) {
                continue;
            }
            if (previous >= 0) {
                for (int k = previous + 1; k < i; k++) {
                    float t = (k - previous) / (float) (i - previous);
                    slots[k] = match(ColorMath.lerp(labs[previous], labs[i], t), choice, k, filters);
                }
            }
            previous = i;
        }
        return slots;
    }

    private static Slot match(float[] lab, int[] choice, int slot, PaletteIndex.Filters filters) {
        int target = ColorMath.fromOklab(lab);
        List<PaletteIndex.Entry> ranked = PaletteIndex.nearest(lab, filters, Math.max(0, choice[slot]) + 2);
        if (ranked.isEmpty()) {
            return new Slot(ItemStack.EMPTY, target, false, true);
        }
        if (choice[slot] >= ranked.size()) {
            choice[slot] = ranked.size() - 1;
        }
        PaletteIndex.Entry entry = ranked.get(Math.max(0, choice[slot]));
        return new Slot(entry.stack(), target, false, choice[slot] >= ranked.size() - 1);
    }
}
