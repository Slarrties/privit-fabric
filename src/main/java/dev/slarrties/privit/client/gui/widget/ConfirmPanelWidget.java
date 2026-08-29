package dev.slarrties.privit.client.gui.widget;

import dev.slarrties.privit.PrivitMod;

import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;

public class ConfirmPanelWidget implements Element, Selectable, Drawable {

    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private boolean visible = false;
    private final ButtonWidget.PressAction acceptAction;
    private final ButtonWidget.PressAction cancelAction;

    private GuiButton acceptButton;
    private GuiButton cancelButton;
    private final Text confirmText;

    private static final Identifier ACCEPT_ICON = Identifier.of(PrivitMod.MOD_ID, "textures/gui/accept_changes_icon.png");
    private static final Identifier CANCEL_ICON = Identifier.of(PrivitMod.MOD_ID, "textures/gui/delete_region_icon.png");
    private static final Identifier BACKGROUND = Identifier.of(PrivitMod.MOD_ID, "textures/gui/confirm_background.png");
    private static final ButtonTextures BUTTON_BACKGROUND = new ButtonTextures(
            Identifier.of(PrivitMod.MOD_ID, "textures/gui/button_paper_common.png"),
            Identifier.of(PrivitMod.MOD_ID, "textures/gui/button_paper_pressed.png"),
            Identifier.of(PrivitMod.MOD_ID, "textures/gui/button_paper_active.png")
    );

    public ConfirmPanelWidget(int x, int y, int width, int height,
                              ButtonWidget.PressAction acceptAction,
                              ButtonWidget.PressAction cancelAction) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.confirmText = Text.translatable("privit.gui.text.confirm_changes");
        this.acceptAction = acceptAction;
        this.cancelAction = cancelAction;

        initButtons();
    }

    private void initButtons() {
        int size = 20;

        acceptButton = new GuiButton.Builder(Text.empty(), acceptAction)
                .dimensions(x + width/2 - size - 6, y + height - size - 8, size, size)
                .tooltip(Tooltip.of(Text.translatable("privit.gui.button.accept_changes")))
                .icon(ACCEPT_ICON, size, size, 0, -2)
                .setBackground(BUTTON_BACKGROUND)
                .build();

        cancelButton = new GuiButton.Builder(Text.empty(), cancelAction)
                .dimensions(x + width/2 + 6, y + height - size - 8, size, size)
                .tooltip(Tooltip.of(Text.translatable("privit.gui.button.cancel_changes")))
                .icon(CANCEL_ICON, size, size, 0, -2)
                .setBackground(BUTTON_BACKGROUND)
                .build();
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
        acceptButton.visible = visible;
        cancelButton.visible = visible;
    }

    public boolean isVisible() {
        return visible;
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!visible) return;

        context.drawTexture(BACKGROUND, x, y, 0, 0, width, height, width, height);
        context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer,
                confirmText, x + width / 2, y + 6, 0xFFFFFF);

        acceptButton.render(context, mouseX, mouseY, delta);
        cancelButton.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) return false;
        return acceptButton.mouseClicked(mouseX, mouseY, button) ||
                cancelButton.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return false;
    }

    @Override
    public void setFocused(boolean focused) {}

    @Override
    public boolean isFocused() {
        return false;
    }

    @Override
    public SelectionType getType() {
        return SelectionType.NONE;
    }

    @Override
    public void appendNarrations(NarrationMessageBuilder builder) {}
}