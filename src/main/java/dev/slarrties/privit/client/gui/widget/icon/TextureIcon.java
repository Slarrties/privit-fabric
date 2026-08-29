package dev.slarrties.privit.client.gui.widget.icon;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.gui.DrawContext;

public class TextureIcon implements Icon {
    private final int texWidth;
    private final int texHeight;
    private final int u;
    private final int v;
    private final int hoverU;
    private final int hoverV;
    private final int disabledU;
    private final int disabledV;

    private final IconMode mode;
    private final Identifier texture;

    public TextureIcon(Identifier texture, int texWidth, int texHeight, int u, int v, IconMode mode) {
        this(texture, texWidth, texHeight, u, v, u, v, u, v, mode);
    }

    public TextureIcon(Identifier texture, int texWidth, int texHeight, int u, int v, int hoverU, int hoverV, int disabledU, int disabledV, IconMode mode) {
        this.texture = texture;
        this.texWidth = texWidth;
        this.texHeight = texHeight;
        this.u = u;
        this.v = v;
        this.hoverU = hoverU;
        this.hoverV = hoverV;
        this.disabledU = disabledU;
        this.disabledV = disabledV;
        this.mode = mode;
    }

    @Override
    public void render(DrawContext context, int x, int y, int availableWidth, int availableHeight, boolean hovered, boolean active) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        int drawU = active ? (hovered ? hoverU : u) : disabledU;
        int drawV = active ? (hovered ? hoverV : v) : disabledV;

        int drawX = x;
        int drawY = y;
        int drawWidth = texWidth;
        int drawHeight = texHeight;

        switch (mode) {
            case FIXED_CENTER:
                drawX += (availableWidth - texWidth) / 2;
                drawY += (availableHeight - texHeight) / 2;
                break;

            case FIT_CENTER:
                float ratio = Math.min((float) availableWidth / texWidth, (float) availableHeight / texHeight);
                drawWidth = (int) (texWidth * ratio);
                drawHeight = (int) (texHeight * ratio);
                drawX += (availableWidth - drawWidth) / 2;
                drawY += (availableHeight - drawHeight) / 2;
                break;

            case STRETCH_FILL:
                drawWidth = availableWidth;
                drawHeight = availableHeight;
                break;

            case STRETCH_FIT:
                float aspect = (float) texWidth / texHeight;
                float areaAspect = (float) availableWidth / availableHeight;
                if (aspect > areaAspect) {
                    drawWidth = availableWidth;
                    drawHeight = (int) (availableWidth / aspect);
                } else {
                    drawHeight = availableHeight;
                    drawWidth = (int) (availableHeight * aspect);
                }
                drawX += (availableWidth - drawWidth) / 2;
                drawY += (availableHeight - drawHeight) / 2;
                break;
        }

        drawWidth = MathHelper.clamp(drawWidth, 1, availableWidth);
        drawHeight = MathHelper.clamp(drawHeight, 1, availableHeight);

        context.drawTexture(
                texture,
                drawX, drawY,
                drawU, drawV,
                drawWidth, drawHeight,
                texWidth, texHeight
        );
    }

    @Override
    public int getWidth() {
        return texWidth;
    }

    @Override
    public int getHeight() {
        return texHeight;
    }

    @Override
    public IconMode getMode() {
        return mode;
    }
}