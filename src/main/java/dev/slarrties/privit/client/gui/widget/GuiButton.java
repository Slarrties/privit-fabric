package dev.slarrties.privit.client.gui.widget;

import dev.slarrties.privit.client.gui.widget.icon.Icon;
import dev.slarrties.privit.client.gui.widget.icon.IconMode;
import dev.slarrties.privit.client.gui.widget.icon.ItemIcon;
import dev.slarrties.privit.client.gui.widget.icon.TextureIcon;

import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.item.ItemStack;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.screen.ButtonTextures;

import org.jetbrains.annotations.Nullable;

public class GuiButton extends ButtonWidget {

    public enum BackgroundMode {
        VANILLA,
        NONE,
        CUSTOM
    }

    private static final ButtonTextures VANILLA_TEXTURES = new ButtonTextures(
            Identifier.ofVanilla("widget/button"),
            Identifier.ofVanilla("widget/button_disabled"),
            Identifier.ofVanilla("widget/button_highlighted")
    );

    public static final NarrationSupplier DEFAULT_NARRATION_SUPPLIER =
            textSupplier -> (net.minecraft.text.MutableText) textSupplier.get();

    private Icon icon;
    private final BackgroundMode backgroundMode;
    private final ButtonTextures backgroundTextures;
    private boolean toggleState = false;
    private final int iconPadding;

    private GuiButton(Builder builder) {
        super(
                builder.x,
                builder.y,
                builder.width,
                builder.height,
                builder.message,
                builder.onPress,
                builder.narrationSupplier
        );
        this.icon = builder.icon;
        this.backgroundMode = builder.backgroundMode;
        this.backgroundTextures = builder.backgroundTextures;
        this.iconPadding = builder.iconPadding;
        super.setTooltip(builder.tooltip);
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean hasText = !this.getMessage().getString().isBlank();

        if (backgroundMode != BackgroundMode.NONE) {
            Identifier textureId;

            if (backgroundMode == BackgroundMode.CUSTOM && backgroundTextures != null) {
                if (!this.active) {
                    textureId = backgroundTextures.disabled();
                } else if (this.isHovered() || this.isSelected()) {
                    textureId = backgroundTextures.enabledFocused();
                } else {
                    textureId = backgroundTextures.enabled();
                }

                context.drawTexture(
                        textureId,
                        this.getX(), this.getY(),
                        0, 0,
                        this.getWidth(), this.getHeight(),
                        this.getWidth(), this.getHeight()
                );
            } else {
                textureId = VANILLA_TEXTURES.get(this.active, this.isSelected());
                context.drawGuiTexture(
                        textureId,
                        this.getX(), this.getY(),
                        this.getWidth(), this.getHeight()
                );
            }
        }

        if (icon != null) {
            int iconX = this.getX() + iconPadding;
            int iconY = this.getY() + (this.getHeight() - icon.getHeight()) / 2;

            icon.render(
                    context,
                    iconX,
                    iconY,
                    this.getWidth() - iconPadding * 2,
                    this.getHeight() - iconPadding * 2,
                    this.isHovered(),
                    this.active
            );
        }

        if (hasText) {
            int color = this.active ? 16777215 : 10526880;
            int alpha = (int) (this.alpha * 255.0F) << 24;

            int textX = this.getX() + (this.getWidth() - client.textRenderer.getWidth(this.getMessage())) / 2;
            int textY = this.getY() + (this.getHeight() - 10) / 2;

            if (icon != null) textX += icon.getWidth() + iconPadding;

            context.drawTextWithShadow(
                    client.textRenderer,
                    this.getMessage(),
                    textX,
                    textY,
                    color | alpha
            );
        }
    }

    public void setIcon(@Nullable Icon newIcon) {
        this.icon = newIcon;
    }

    public void setIcon(@Nullable ItemStack newIcon) {
        this.icon = (newIcon == null || newIcon.isEmpty()) ? null : new ItemIcon(newIcon);
    }

    public void setIcon(Identifier texture, int texWidth, int texHeight, int u, int v) {
        this.icon = new TextureIcon(texture, texWidth, texHeight, u, v, IconMode.FIXED_CENTER);
    }

    public void setTooltip(@Nullable Tooltip newTooltip) {
        super.setTooltip(newTooltip);
    }

    public void setToggleState(boolean state) {
        this.toggleState = state;
    }

    public boolean isToggled() {
        return toggleState;
    }

    // ===================================================================
    // Builder
    // ===================================================================

    public static class Builder {
        private final Text message;
        private final PressAction onPress;

        private int x = 0;
        private int y = 0;
        private int width = 150;
        private int height = 20;

        private NarrationSupplier narrationSupplier = DEFAULT_NARRATION_SUPPLIER;
        private BackgroundMode backgroundMode = BackgroundMode.VANILLA;
        private ButtonTextures backgroundTextures;
        private Tooltip tooltip;
        private Icon icon;
        private int iconPadding = 2;

        public Builder(Text message, PressAction onPress) {
            this.message = message;
            this.onPress = onPress;
        }

        public Builder position(int x, int y) {
            this.x = x;
            this.y = y;
            return this;
        }

        public Builder size(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder dimensions(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder width(int width) {
            this.width = width;
            return this;
        }

        public Builder height(int height) {
            this.height = height;
            return this;
        }

        public Builder tooltip(@Nullable Tooltip tooltip) {
            this.tooltip = tooltip;
            return this;
        }

        public Builder narration(NarrationSupplier supplier) {
            this.narrationSupplier = supplier;
            return this;
        }

        public Builder icon(@Nullable ItemStack icon) {
            this.icon = (icon == null || icon.isEmpty()) ? null : new ItemIcon(icon);
            return this;
        }

        public Builder icon(Identifier texture, int texWidth, int texHeight, int u, int v) {
            this.icon = new TextureIcon(texture, texWidth, texHeight, u, v, IconMode.FIXED_CENTER);
            return this;
        }

        public Builder icon(Identifier texture, int texWidth, int texHeight, int u, int v, IconMode mode) {
            this.icon = new TextureIcon(texture, texWidth, texHeight, u, v, mode);
            return this;
        }

        public Builder icon(Identifier texture, int texWidth, int texHeight, int u, int v, int hoverU, int hoverV, int disabledU, int disabledV, IconMode mode) {
            this.icon = new TextureIcon(texture, texWidth, texHeight, u, v, hoverU, hoverV, disabledU, disabledV, mode);
            return this;
        }

        public Builder icon(@Nullable Icon icon) {
            this.icon = icon;
            return this;
        }

        public Builder iconPadding(int padding) {
            this.iconPadding = padding;
            return this;
        }

        public Builder noBackground() {
            this.backgroundMode = BackgroundMode.NONE;
            return this;
        }

        public Builder setBackground(ButtonTextures textures) {
            this.backgroundMode = BackgroundMode.CUSTOM;
            this.backgroundTextures = textures;
            return this;
        }

        public GuiButton build() {
            return new GuiButton(this);
        }
    }
}