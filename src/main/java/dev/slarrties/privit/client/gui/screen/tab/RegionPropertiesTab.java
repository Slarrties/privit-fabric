package dev.slarrties.privit.client.gui.screen.tab;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.client.gui.RegionGuiController;
import dev.slarrties.privit.client.gui.widget.GuiButton;
import dev.slarrties.privit.client.gui.widget.CustomTextField;
import dev.slarrties.privit.client.gui.widget.RegionAreaWidget;
import dev.slarrties.privit.client.gui.screen.RegionColorScreen;
import dev.slarrties.privit.client.render.pipeline.RegionRenderSystem;
import dev.slarrties.privit.common.network.payload.c2s.RegionCreateC2SPacket;
import dev.slarrties.privit.common.network.payload.c2s.RegionDeleteC2SPacket;
import dev.slarrties.privit.common.network.payload.c2s.RegionGridStateC2SPacket;
import dev.slarrties.privit.common.network.payload.c2s.RegionGuiUpdateC2SPacket;

import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.input.MouseButtonEvent;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.List;
import java.util.UUID;
import java.util.Optional;
import java.util.ArrayList;

public class RegionPropertiesTab implements ITabPanel {
    private boolean isTabVisible = false;
    private final int x, y, width, height;
    private final Font font;
    private final RegionGuiController controller;

    private static final int BUTTON_SIZE = 20;
    private static final int TEXT_FIELD_WIDTH = 112;
    private static final int GAP = 4;
    private static final int ELEMENT_HEIGHT = 20;

    private static final WidgetSprites STANDARD_BUTTON_BACKGROUND = new WidgetSprites(
            Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "textures/gui/button_bg_common.png"),
            Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "textures/gui/button_bg_common_pressed.png"),
            Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "textures/gui/button_bg_common_active.png")
    );

    private static final Identifier ICON_CREATE_REGION = Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "textures/gui/create_region_icon.png");
    private static final Identifier ICON_DELETE_REGION = Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "textures/gui/delete_region_icon.png");
    private static final Identifier ICON_SHOW_GRID = Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "textures/gui/hide_grid_region_icon.png");
    private static final Identifier ICON_HIDE_GRID = Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "textures/gui/show_grid_region_icon.png");

    private RegionAreaWidget areaWidget;
    private CustomTextField nameField;
    private GuiButton colorButton;
    private GuiButton gridButton;
    private GuiButton createDeleteButton;

    private final List<AbstractWidget> widgets = new ArrayList<>();

    public RegionPropertiesTab(
            RegionGuiController controller,
            Font font,
            int x, int y, int width, int height) {
        this.controller = controller;
        this.font = font;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Override
    public void init(int x, int y, int width, int height) {
        initAreaWidget();
        initPropertiesPanel();
        widgets.addAll(areaWidget.getButtons());
        widgets.addAll(areaWidget.getFields());
        widgets.add(nameField);
        widgets.add(colorButton);
        widgets.add(gridButton);
        widgets.add(createDeleteButton);
        updateUI();
    }

    private void initAreaWidget() {
        int widgetX = x + 10;
        int widgetY = y + 20;
        int widgetWidth = width - 20;
        int fieldHeight = 20;
        areaWidget = new RegionAreaWidget(this.controller, font, widgetX, widgetY, widgetWidth, fieldHeight);
    }

    private void initPropertiesPanel() {
        int areaBottom = (y + 95) + 12;
        int mainPanelY = areaBottom + 8;
        int totalButtonsWidth = 3 * BUTTON_SIZE + 2 * GAP;
        int totalWidth = TEXT_FIELD_WIDTH + GAP + totalButtonsWidth;
        int startX = x + (width - totalWidth) / 2;
        int buttonX = startX + TEXT_FIELD_WIDTH + GAP;

        nameField = new CustomTextField(font, startX, mainPanelY, TEXT_FIELD_WIDTH, ELEMENT_HEIGHT, Component.empty());
        nameField.setMaxLength(24);
        nameField.setValue(this.controller.getLocalState().name());
        nameField.setCustomBackground(
                Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "textures/gui/text_field_bg.png"),
                Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "textures/gui/text_field_bg_active.png")
        );

        nameField.setResponder(newText -> {
            if (newText.equals(this.controller.getLocalState().name()) || newText.isBlank()) return;

            RegionGuiUpdateC2SPacket packet = new RegionGuiUpdateC2SPacket(
                    this.controller.getLocalState().id(),
                    true,
                    Minecraft.getInstance().player.getName().getString(),
                    Optional.of(newText), Optional.empty(),
                    Optional.empty(), Optional.empty(), Optional.empty(),
                    Optional.empty(), Optional.empty(), Optional.empty()
            );

            this.controller.sendUpdate(packet);
        });

        colorButton = new GuiButton.Builder(Component.empty(), button -> {
            Minecraft.getInstance().setScreen(new RegionColorScreen(this.controller));
        })
                .dimensions(buttonX, mainPanelY, BUTTON_SIZE, BUTTON_SIZE)
                .icon(getColorPreviewIcon(), BUTTON_SIZE, BUTTON_SIZE, 0, -2)
                .tooltip(Tooltip.create(Component.translatable("privit.gui.properties.button.change_color")))
                .setBackground(STANDARD_BUTTON_BACKGROUND)
                .build();
        buttonX += BUTTON_SIZE + GAP;

        gridButton = new GuiButton.Builder(Component.empty(), button -> {
            UUID regionId = this.controller.getLocalState().id();
            boolean current = RegionRenderSystem.isGridVisible(regionId);
            boolean newVisible = !current;
            RegionRenderSystem.setGridVisible(regionId, newVisible);
            ClientPlayNetworking.send(new RegionGridStateC2SPacket(regionId, newVisible));
            updateGridButton();
        })
                .dimensions(buttonX, mainPanelY, BUTTON_SIZE, BUTTON_SIZE)
                .icon(getGridIcon(), BUTTON_SIZE, BUTTON_SIZE, 0, 0)
                .iconPadding(0)
                .size(BUTTON_SIZE, BUTTON_SIZE)
                .tooltip(Tooltip.create(getGridTooltip()))
                .setBackground(STANDARD_BUTTON_BACKGROUND)
                .build();
        buttonX += BUTTON_SIZE + GAP;

        createDeleteButton = new GuiButton.Builder(Component.empty(), button -> {
            UUID regionId = this.controller.getLocalState().id();

            if (!this.controller.getLocalState().isCreated()) {
                ClientPlayNetworking.send(new RegionCreateC2SPacket(this.controller.getLocalState().toRegionScreenState()));
            } else {
                ClientPlayNetworking.send(new RegionDeleteC2SPacket(regionId));
                ClientPlayNetworking.send(new RegionGridStateC2SPacket(regionId, false));
                RegionRenderSystem.disableAndRemove(regionId);
            }

            if (Minecraft.getInstance().screen != null) {
                Minecraft.getInstance().setScreen(null);
            }
        })
                .dimensions(buttonX, mainPanelY, BUTTON_SIZE, BUTTON_SIZE)
                .icon(ICON_CREATE_REGION, BUTTON_SIZE, BUTTON_SIZE, 0, 0)
                .iconPadding(0)
                .size(BUTTON_SIZE, BUTTON_SIZE)
                .tooltip(Tooltip.create(Component.translatable("privit.gui.properties.button.create_region")))
                .setBackground(STANDARD_BUTTON_BACKGROUND)
                .build();
    }

    @Override
    public List<AbstractWidget> getWidgets() {
        return widgets;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {}

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return false;
    }

    @Override
    public void setVisible(boolean visible) {
        this.isTabVisible = visible;
        updateVisibility();
    }

    @Override
    public void updateVisibility() {
        for (AbstractWidget w : widgets) {
            w.visible = isTabVisible;
        }
    }

    @Override
    public void updateState() {
        nameField.setValue(this.controller.getLocalState().name());
        areaWidget.updateState();
        updateUI();
    }

    @Override
    public void updateUI() {
        boolean isCreation = !this.controller.getLocalState().isCreated();
        createDeleteButton.setIcon(isCreation ?
                        ICON_CREATE_REGION :
                        ICON_DELETE_REGION,
                BUTTON_SIZE, BUTTON_SIZE, 0, 0);
        createDeleteButton.setTooltip(Tooltip.create(isCreation ?
                Component.translatable("privit.gui.properties.button.create_region") :
                Component.translatable("privit.gui.properties.button.delete_region")));
        colorButton.setIcon(getColorPreviewIcon(), BUTTON_SIZE, BUTTON_SIZE, 0, -2);
        updateGridButton();
    }

    private Identifier getColorPreviewIcon() {
        String iconPath = switch (this.controller.getLocalState().color()) {
            case BLACK -> "textures/gui/icon_black.png";
            case DARK_GRAY -> "textures/gui/icon_dark_gray.png";
            case DARK_BLUE -> "textures/gui/icon_dark_blue.png";
            case DARK_GREEN -> "textures/gui/icon_dark_green.png";
            case DARK_AQUA -> "textures/gui/icon_dark_aqua.png";
            case DARK_RED -> "textures/gui/icon_dark_red.png";
            case DARK_PURPLE -> "textures/gui/icon_dark_purple.png";
            case GOLD -> "textures/gui/icon_gold.png";
            case GRAY -> "textures/gui/icon_gray.png";
            case BLUE -> "textures/gui/icon_blue.png";
            case GREEN -> "textures/gui/icon_green.png";
            case AQUA -> "textures/gui/icon_aqua.png";
            case RED -> "textures/gui/icon_red.png";
            case LIGHT_PURPLE -> "textures/gui/icon_purple.png";
            case YELLOW -> "textures/gui/icon_yellow.png";
            case WHITE -> "textures/gui/icon_white.png";
        };
        return Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, iconPath);
    }

    private void updateGridButton() {
        gridButton.setIcon(getGridIcon(), BUTTON_SIZE, BUTTON_SIZE, 0, 0);
        gridButton.setTooltip(Tooltip.create(getGridTooltip()));
    }

    private Identifier getGridIcon() {
        UUID regionId = this.controller.getLocalState().id();
        return RegionRenderSystem.isGridVisible(regionId)
                ? ICON_SHOW_GRID
                : ICON_HIDE_GRID;
    }

    private Component getGridTooltip() {
        UUID regionId = this.controller.getLocalState().id();
        return RegionRenderSystem.isGridVisible(regionId)
                ? Component.translatable("privit.gui.properties.button.hide_grid")
                : Component.translatable("privit.gui.properties.button.show_grid");
    }
}