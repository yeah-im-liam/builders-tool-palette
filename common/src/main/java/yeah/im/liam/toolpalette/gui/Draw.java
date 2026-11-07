package yeah.im.liam.toolpalette.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Inventory-style slots and buttons drawn from plain fills. */
final class Draw {
    static final int TEXT = 0xFF404040;
    static final int TEXT_LIGHT = 0xFF6F6F6F;

    private Draw() {
    }

    /** A sunken 18x18 slot like the inventory's. */
    static void slot(GuiGraphicsExtractor g, int x, int y) {
        inset(g, x, y, x + 18, y + 18, 0xFF8B8B8B);
    }

    static void inset(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int fill) {
        g.fill(x1, y1, x2, y2, fill);
        g.fill(x1, y1, x2 - 1, y1 + 1, 0xFF373737);
        g.fill(x1, y1, x1 + 1, y2 - 1, 0xFF373737);
        g.fill(x1 + 1, y2 - 1, x2, y2, 0xFFFFFFFF);
        g.fill(x2 - 1, y1 + 1, x2, y2, 0xFFFFFFFF);
    }

    /** A raised button face, lighter while hovered. */
    static void button(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, boolean hovered) {
        g.fill(x1, y1, x2, y2, 0xFF000000);
        g.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, hovered ? 0xFFA9B5E0 : 0xFF8B8B8B);
        g.fill(x1 + 1, y1 + 1, x2 - 2, y1 + 2, hovered ? 0xFFDDE3FF : 0xFFC6C6C6);
        g.fill(x1 + 1, y1 + 1, x1 + 2, y2 - 2, hovered ? 0xFFDDE3FF : 0xFFC6C6C6);
        g.fill(x1 + 2, y2 - 2, x2 - 1, y2 - 1, 0xFF555555);
        g.fill(x2 - 2, y1 + 2, x2 - 1, y2 - 1, 0xFF555555);
    }

    static void hoverHighlight(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x + 1, y + 1, x + 17, y + 17, 0x80FFFFFF);
    }

    /** A small triangle 5 pixels wide, pointing up or down, with its top-left corner at (x, y). */
    static void arrow(GuiGraphicsExtractor g, int x, int y, boolean up, int color) {
        for (int row = 0; row < 3; row++) {
            int half = up ? row : 2 - row;
            g.fill(x + 2 - half, y + row, x + 3 + half, y + row + 1, color);
        }
    }
}
