package dev.slarrties.privit.client.gui.screen;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.client.hud.NotificationHudOverlay;
import dev.slarrties.privit.client.gui.RegionGuiController;
import dev.slarrties.privit.client.gui.widget.GuiButton;
import dev.slarrties.privit.client.gui.widget.ConfirmPanelWidget;
import dev.slarrties.privit.client.gui.screen.tab.ITabPanel;
import dev.slarrties.privit.client.gui.screen.tab.RegionGroupsTab;
import dev.slarrties.privit.client.gui.screen.tab.RegionPropertiesTab;
import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.gui.state.RegionGuiState;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.common.network.payload.c2s.RegionUpdateC2SPacket;
import dev.slarrties.privit.common.network.payload.c2s.RegionGuiCancelC2SPacket;

import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

public class RegionScreen extends Screen {

    public enum Tab { REGION_PROPERTIES, PLAYER_GROUPS }

    private Tab currentTab = Tab.REGION_PROPERTIES;
    private final RegionGuiController controller;
    private final Map<Tab, ITabPanel> tabs = new HashMap<>();

    private GuiButton tabPropertiesButton;
    private GuiButton tabGroupsButton;
    private ConfirmPanelWidget confirmPanel;

    private static final int BG_WIDTH = 210;
    private static final int BG_HEIGHT = 180;
    private static final Identifier BACKGROUND = Identifier.of(PrivitMod.MOD_ID, "textures/gui/region_gui_background.png");
    private static final Identifier BACKGROUND_BLUEPRINT = Identifier.of(PrivitMod.MOD_ID, "textures/gui/blueprint_background.png");
    private static final Identifier BACKGROUND_BOOK = Identifier.of(PrivitMod.MOD_ID, "textures/gui/book_background.png");
    private static final ButtonTextures WIDE_BUTTON_BACKGROUND = new ButtonTextures(
            Identifier.of(PrivitMod.MOD_ID, "textures/gui/wide_button_bg_common.png"),
            Identifier.of(PrivitMod.MOD_ID, "textures/gui/wide_button_bg_common_pressed.png"),
            Identifier.of(PrivitMod.MOD_ID, "textures/gui/wide_button_bg_common_active.png")
    );

    public RegionScreen(RegionGuiController controller) {
        super(Text.empty());
        this.controller = controller;
    }

    public RegionScreen(RegionGuiController controller, Tab initialTab) {
        super(Text.empty());
        this.controller = controller;
        this.currentTab = (initialTab != null) ? initialTab : Tab.REGION_PROPERTIES;
    }

    @Override
    protected void init() {
        if (this.controller == null) {
            PrivitMod.LOGGER.warn("[RegionScreen] State is null: closing screen");
            if (client != null && client.player != null)
                NotificationHudOverlay.showNotification(NotificationType.REGION_NOT_FOUND, Color.RED);
            close();
            return;
        }

        int backgroundX = (width - BG_WIDTH) / 2;
        int backgroundY = (height - BG_HEIGHT) / 2;
        int panelX = backgroundX + 9;
        int panelY = backgroundY + 31;
        int panelWidth = BG_WIDTH - 16;
        int panelHeight = BG_HEIGHT - 70;

        tabs.put(Tab.REGION_PROPERTIES, new RegionPropertiesTab(this.controller, textRenderer, panelX, panelY, panelWidth, panelHeight));
        tabs.put(Tab.PLAYER_GROUPS, new RegionGroupsTab(this.controller, textRenderer, panelX, panelY, panelWidth, panelHeight));
        tabs.forEach((tabType, tab) -> {
            tab.init(panelX, panelY, panelWidth, panelHeight);
            tab.getWidgets().forEach(this::addDrawableChild);
            tab.setVisible(tabType == currentTab);
        });

        int confirmHeight = 50;
        int confirmWidth = 100;
        int confirmX = backgroundX - confirmWidth - 16;
        int confirmY = backgroundY + (BG_HEIGHT - confirmHeight) / 2;

        confirmPanel = new ConfirmPanelWidget(confirmX, confirmY, confirmWidth, confirmHeight,
                button -> acceptChanges(),
                button -> cancelChanges()
        );
        addDrawableChild(confirmPanel);

        initTabButtons(backgroundX, backgroundY);
        updateTabButtonVisuals();
        updateConfirmPanelVisibility();
    }

    private void initTabButtons(int bgX, int bgY) {
        int tabWidth = 90;
        int tabHeight = 20;
        int tabY = bgY + 8;
        int centerX = (width / 2) + 1;

        tabPropertiesButton = new GuiButton.Builder(
                Text.translatable("privit.gui.button.properties"),
                btn -> setCurrentTab(Tab.REGION_PROPERTIES))
                .dimensions(centerX - tabWidth - 2, tabY, tabWidth, tabHeight)
                .setBackground(WIDE_BUTTON_BACKGROUND)
                .build();
        addDrawableChild(tabPropertiesButton);

        tabGroupsButton = new GuiButton.Builder(
                Text.translatable("privit.gui.button.groups"),
                btn -> setCurrentTab(Tab.PLAYER_GROUPS))
                .dimensions(centerX + 2, tabY, tabWidth, tabHeight)
                .setBackground(WIDE_BUTTON_BACKGROUND)
                .build();
        addDrawableChild(tabGroupsButton);
    }

    private void setCurrentTab(Tab newTab) {
        if (currentTab == newTab) return;
        currentTab = newTab;

        tabs.forEach((tabType, tab) -> tab.setVisible(tabType == currentTab));

        updateTabButtonVisuals();
        updateConfirmPanelVisibility();
    }

    private void updateTabButtonVisuals() {
        tabPropertiesButton.active = (currentTab != Tab.REGION_PROPERTIES);
        tabGroupsButton.active = (currentTab != Tab.PLAYER_GROUPS);
    }

    private void updateConfirmPanelVisibility() {
        boolean isCreated = !this.controller.getLocalState().isCreated();
        boolean hasChanges = this.controller.getLocalState().isChanged();
        boolean showConfirm = !isCreated && hasChanges;

        confirmPanel.setVisible(showConfirm);
    }

    public void applyUpdate() {
        tabs.forEach((tabType, tab) -> tab.updateState());
        updateConfirmPanelVisibility();
    }

    private void acceptChanges() {
        RegionGuiState stateToSend = this.controller.getLocalState().toRegionScreenState();
        ClientPlayNetworking.send(new RegionUpdateC2SPacket(stateToSend));
    }

    private void cancelChanges() {
        UUID regionId = this.controller.getLocalState().id();
        ClientPlayNetworking.send(new RegionGuiCancelC2SPacket(regionId));
    }

    public RegionGuiController getController() { return controller; }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);

        ITabPanel activeTab = tabs.get(currentTab);

        if (activeTab != null) {
            activeTab.render(context, mouseX, mouseY, delta);
        }

        super.render(context, mouseX, mouseY, delta);

        if (activeTab != null) {
            activeTab.renderPendingTooltips(context);
        }
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        int x = (width - BG_WIDTH) / 2;
        int y = (height - BG_HEIGHT) / 2;

        context.fillGradient(0, 0, width, height, 0xC0101010, 0xD0101010);
        context.drawTexture(BACKGROUND, x, y, 0, 0, BG_WIDTH, BG_HEIGHT, BG_WIDTH, BG_HEIGHT);
        context.drawTexture(currentTab == Tab.REGION_PROPERTIES ? BACKGROUND_BLUEPRINT : BACKGROUND_BOOK,
                x, y, 0, 0,BG_WIDTH, BG_HEIGHT, BG_WIDTH, BG_HEIGHT);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        ITabPanel activeTab = tabs.get(currentTab);

        if (activeTab != null && activeTab.mouseClicked(mouseX, mouseY, button))
            return true;
        if (confirmPanel.isVisible() && confirmPanel.mouseClicked(mouseX, mouseY, button))
            return true;

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        ITabPanel activeTab = tabs.get(currentTab);

        if (activeTab != null && activeTab.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void close() { super.close(); }

    @Override
    public boolean shouldPause() { return false; }
}