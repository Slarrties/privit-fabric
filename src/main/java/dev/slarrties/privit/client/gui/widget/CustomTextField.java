package dev.slarrties.privit.client.gui.widget;

import net.minecraft.util.Mth;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.renderer.RenderPipelines;

import org.jetbrains.annotations.Nullable;

public class CustomTextField extends EditBox {
    private @Nullable Identifier normalBackground;
    private @Nullable Identifier focusedBackground;
    private boolean numericOnly = false;

    public CustomTextField(Font font, int x, int y, int width, int height, Component text) {
        super(font, x, y, width, height, text);
        this.setBordered(false);
    }

    public void setCustomBackground(@Nullable Identifier normal, @Nullable Identifier focused) {
        this.normalBackground = normal;
        this.focusedBackground = focused;
    }

    public void setNumericOnly(boolean numericOnly) {
        this.numericOnly = numericOnly;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (numericOnly) {
            if (!event.codepointAsString().matches("\\d+")) {
                return false;
            }
        }
        return super.charTyped(event);
    }

    @Override
    public void insertText(String input) {
        if (numericOnly) {
            String filtered = input.replaceAll("[^0-9]", "");
            if (!filtered.isEmpty()) super.insertText(filtered);
        } else {
            super.insertText(input);
        }
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (!this.isVisible()) return;

        int fieldX = this.getX();
        int fieldY = this.getY();
        int fieldW = this.getWidth();
        int fieldH = this.getHeight();

        Identifier currentBg = this.isFocused() && focusedBackground != null
                ? focusedBackground
                : normalBackground;

        if (currentBg != null) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    currentBg,
                    fieldX, fieldY,
                    0, 0,
                    fieldW, fieldH,
                    fieldW, fieldH
            );
        }

        Font font = Minecraft.getInstance().font;
        String text = this.getValue();
        int cursor = Mth.clamp(this.getCursorPosition(), 0, text.length());
        int paddingLeft = 6;
        int paddingRight = 6;
        int textX = fieldX + paddingLeft;
        int textY = fieldY + (fieldH - 7) / 2;
        int availableWidth = Math.max(0, fieldW - paddingLeft - paddingRight);
        int cursorXRel = font.width(text.substring(0, cursor));
        int scrollX = 0;

        if (cursorXRel > availableWidth) {
            scrollX = cursorXRel - availableWidth;
        }

        graphics.enableScissor(fieldX + 1, fieldY, fieldX + fieldW - 1, fieldY + fieldH);
        graphics.text(font, text, textX - scrollX, textY, 0xFFE0E0E0, true);

        if (this.isFocused() && (System.currentTimeMillis() / 300 & 1) == 0) {
            int cursorX = textX - scrollX + cursorXRel;
            graphics.fill(cursorX, textY - 1, cursorX + 1, textY + 8, 0xFFFFFFFF);
        }

        graphics.disableScissor();
    }
}