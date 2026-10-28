package dev.slarrties.privit.client.gui.widget.icon;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public interface Icon {
    void render(GuiGraphicsExtractor graphics, int x, int y, int availableWidth, int availableHeight, boolean hovered, boolean active);
    default IconMode getMode() { return IconMode.FIXED_CENTER; }
    int getWidth();
    int getHeight();
}