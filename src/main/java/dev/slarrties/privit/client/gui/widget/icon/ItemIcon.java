package dev.slarrties.privit.client.gui.widget.icon;

import net.minecraft.item.ItemStack;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class ItemIcon implements Icon {
    private final ItemStack stack;
    private final int size;

    public ItemIcon(ItemStack stack, int size) {
        this.stack = stack.copy();
        this.size = size;
    }

    public ItemIcon(ItemStack stack) { this(stack, 16); }

    @Override
    public void render(DrawContext context, int x, int y, int availableWidth, int availableHeight, boolean hovered, boolean active) {
        MinecraftClient client = MinecraftClient.getInstance();
        context.drawItem(stack, x, y);
        context.drawItemInSlot(client.textRenderer, stack, x, y);
    }

    @Override
    public int getWidth() { return size; }

    @Override
    public int getHeight() { return size; }

    @Override
    public IconMode getMode() { return IconMode.FIXED_CENTER; }
}