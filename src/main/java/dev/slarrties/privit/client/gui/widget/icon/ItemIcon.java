package dev.slarrties.privit.client.gui.widget.icon;

import net.minecraft.world.item.ItemStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class ItemIcon implements Icon {
    private final ItemStack stack;
    private final int size;

    public ItemIcon(ItemStack stack, int size) {
        this.stack = stack.copy();
        this.size = size;
    }

    public ItemIcon(ItemStack stack) {
        this(stack, 16);
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, int x, int y, int availableWidth, int availableHeight, boolean hovered, boolean active) {
        Minecraft minecraft = Minecraft.getInstance();
        graphics.item(stack, x, y);
        graphics.itemDecorations(minecraft.font, stack, x, y);
    }

    @Override
    public int getWidth() {
        return size;
    }

    @Override
    public int getHeight() {
        return size;
    }

    @Override
    public IconMode getMode() {
        return IconMode.FIXED_CENTER;
    }
}