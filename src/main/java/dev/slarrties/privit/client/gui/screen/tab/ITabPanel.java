package dev.slarrties.privit.client.gui.screen.tab;

import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;

import java.util.List;

public interface ITabPanel {
    void init(int x, int y, int width, int height);
    void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta);
    default void renderPendingTooltips(GuiGraphicsExtractor graphics) {}
    boolean mouseClicked(MouseButtonEvent event, boolean doubleClick);
    boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount);
    void setVisible(boolean visible);
    void updateUI();
    void updateVisibility();
    void updateState();
    List<AbstractWidget> getWidgets();
}