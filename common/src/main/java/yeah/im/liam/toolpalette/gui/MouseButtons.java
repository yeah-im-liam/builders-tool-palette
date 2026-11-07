package yeah.im.liam.toolpalette.gui;

import com.mojang.blaze3d.platform.InputConstants;

/**
 * Mouse button numbers, read when the game runs: 26.3 numbers them from 1 (left 1, right 3) where 26.1 and 26.2 use
 * 0 and 1. The constants can't be used directly because the compiler copies their 26.3 values into the jar.
 */
final class MouseButtons {
    static final int LEFT = constant("MOUSE_BUTTON_LEFT", 0);
    static final int RIGHT = constant("MOUSE_BUTTON_RIGHT", 1);

    private MouseButtons() {
    }

    private static int constant(String name, int fallback) {
        try {
            return InputConstants.class.getField(name).getInt(null);
        } catch (ReflectiveOperationException e) {
            return fallback;
        }
    }
}
