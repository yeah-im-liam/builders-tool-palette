package yeah.im.liam.toolpalette.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * A narrow game-style button with a one-letter label placed by hand: the game's centring puts a letter one pixel left of
 * the middle of an odd-width button, because a letter's width counts the gap after it.
 */
final class LetterButton extends Button {
    LetterButton(int x, int y, int width, int height, OnPress onPress) {
        super(x, y, width, height, Component.empty(), onPress, narration -> narration.get());
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        extractDefaultSprite(g);
        Font font = Minecraft.getInstance().font;
        Component label = getMessage();
        int inkWidth = font.width(label) - 1;
        int textX = getX() + (getWidth() - inkWidth) / 2;
        g.text(font, label, textX, getY() + (getHeight() - 8) / 2, active ? 0xFFFFFFFF : 0xFFA0A0A0, true);
    }
}
