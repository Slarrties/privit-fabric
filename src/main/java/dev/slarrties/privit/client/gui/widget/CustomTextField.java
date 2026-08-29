package dev.slarrties.privit.client.gui.widget;

import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.jetbrains.annotations.Nullable;

public class CustomTextField extends TextFieldWidget {

    private @Nullable Identifier normalBackground;
    private @Nullable Identifier focusedBackground;

    public CustomTextField(TextRenderer textRenderer, int x, int y, int width, int height, Text text) {
        super(textRenderer, x, y, width, height, text);
        this.setDrawsBackground(false);
    }

    public void setCustomBackground(@Nullable Identifier normal, @Nullable Identifier focused) {
        this.normalBackground = normal;
        this.focusedBackground = focused;
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!this.visible) return;

        int fieldX = this.getX();
        int fieldY = this.getY();
        int fieldW = this.width;
        int fieldH = this.height;

        Identifier currentBg = this.isFocused() && focusedBackground != null
                ? focusedBackground
                : normalBackground;

        if (currentBg != null) {
            context.drawTexture(currentBg, fieldX, fieldY, 0, 0, fieldW, fieldH, fieldW, fieldH);
        }

        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        String text = this.getText();
        int cursor = MathHelper.clamp(this.getCursor(), 0, text.length());

        int paddingLeft = 6;
        int paddingRight = 6;
        int textX = fieldX + paddingLeft;
        int textY = fieldY + (fieldH - 7) / 2;
        int availableWidth = Math.max(0, fieldW - paddingLeft - paddingRight);
        int cursorXRel = tr.getWidth(text.substring(0, cursor));
        int scrollX = 0;
        if (cursorXRel > availableWidth) {
            scrollX = cursorXRel - availableWidth;
        }

        context.enableScissor(fieldX + 1, fieldY, fieldX + fieldW - 1, fieldY + fieldH);
        context.drawTextWithShadow(tr, text, textX - scrollX, textY, 0xE0E0E0);

        if (this.isFocused() && (System.currentTimeMillis() / 300 & 1) == 0) {
            int cursorX = textX - scrollX + cursorXRel;
            context.fill(cursorX, textY - 1, cursorX + 1, textY + 8, 0xFFFFFFFF);
        }

        context.disableScissor();
    }
}