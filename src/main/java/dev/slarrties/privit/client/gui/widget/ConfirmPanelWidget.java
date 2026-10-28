package dev.slarrties.privit.client.gui.widget;

import dev.slarrties.privit.PrivitMod;

import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;

public class ConfirmPanelWidget implements GuiEventListener, Renderable, NarratableEntry {
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private boolean visible = false;

    private final Button.OnPress acceptAction;
    private final Button.OnPress cancelAction;
    private GuiButton acceptButton;
    private GuiButton cancelButton;
    private final Component confirmText;

    private static final Identifier ACCEPT_ICON = Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "textures/gui/accept_changes_icon.png");
    private static final Identifier CANCEL_ICON = Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "textures/gui/delete_region_icon.png");
    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "textures/gui/confirm_background.png");

    private static final WidgetSprites BUTTON_BACKGROUND = new WidgetSprites(
            Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "textures/gui/button_paper_common.png"),
            Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "textures/gui/button_paper_pressed.png"),
            Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "textures/gui/button_paper_active.png")
    );

    public ConfirmPanelWidget(int x, int y, int width, int height,
                              Button.OnPress acceptAction,
                              Button.OnPress cancelAction) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.confirmText = Component.translatable("privit.gui.text.confirm_changes");
        this.acceptAction = acceptAction;
        this.cancelAction = cancelAction;
        initButtons();
    }

    private void initButtons() {
        int size = 20;

        acceptButton = new GuiButton.Builder(Component.empty(), acceptAction)
                .dimensions(x + width / 2 - size - 6, y + height - size - 8, size, size)
                .tooltip(Tooltip.create(Component.translatable("privit.gui.button.accept_changes")))
                .icon(ACCEPT_ICON, size, size, 0, -2)
                .setBackground(BUTTON_BACKGROUND)
                .build();

        cancelButton = new GuiButton.Builder(Component.empty(), cancelAction)
                .dimensions(x + width / 2 + 6, y + height - size - 8, size, size)
                .tooltip(Tooltip.create(Component.translatable("privit.gui.button.cancel_changes")))
                .icon(CANCEL_ICON, size, size, 0, -2)
                .setBackground(BUTTON_BACKGROUND)
                .build();
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
        acceptButton.visible = visible;
        cancelButton.visible = visible;
    }

    public boolean isVisible() { return visible; }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (!visible) return;

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                BACKGROUND,
                x, y,
                0, 0,
                width, height,
                width, height
        );

        graphics.centeredText(
                Minecraft.getInstance().font,
                confirmText,
                x + width / 2,
                y + 6,
                0xFFFFFFFF
        );

        acceptButton.extractRenderState(graphics, mouseX, mouseY, delta);
        cancelButton.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!visible) return false;
        return acceptButton.mouseClicked(event, doubleClick) ||
                cancelButton.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return false;
    }

    @Override
    public void setFocused(boolean focused) {}

    @Override
    public boolean isFocused() { return false; }

    @Override
    public NarrationPriority narrationPriority() { return NarratableEntry.NarrationPriority.NONE; }

    @Override
    public void updateNarration(NarrationElementOutput output) {}
}