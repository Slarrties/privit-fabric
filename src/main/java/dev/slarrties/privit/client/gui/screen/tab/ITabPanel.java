package dev.slarrties.privit.client.gui.screen.tab;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;

import java.util.List;

public interface ITabPanel {

    void init(int x, int y, int width, int height);
    void render(DrawContext context, int mouseX, int mouseY, float delta);
    default void renderPendingTooltips(DrawContext context) {}
    boolean mouseClicked(double mouseX, double mouseY, int button);
    boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount);
    void setVisible(boolean visible);
    void updateUI();
    void updateVisibility();
    void updateState();
    List<ClickableWidget> getWidgets();
}