package dev.slarrties.privit.client.gui.screen.tab;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.client.gui.RegionGuiController;
import dev.slarrties.privit.client.gui.widget.GuiButton;
import dev.slarrties.privit.client.gui.widget.CustomTextField;
import dev.slarrties.privit.client.gui.widget.RegionAreaWidget;
import dev.slarrties.privit.client.gui.screen.RegionColorScreen;
import dev.slarrties.privit.client.render.RegionRenderManager;
import dev.slarrties.privit.common.network.payload.c2s.RegionCreateC2SPacket;
import dev.slarrties.privit.common.network.payload.c2s.RegionDeleteC2SPacket;
import dev.slarrties.privit.common.network.payload.c2s.RegionGridStateC2SPacket;
import dev.slarrties.privit.common.network.payload.c2s.RegionGuiUpdateC2SPacket;

import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.UUID;

public class RegionPropertiesTab implements ITabPanel {

    private boolean isTabVisible = false;
    private final int x, y, width, height;
    private final TextRenderer textRenderer;
    private final RegionGuiController controller;

    private static final int BUTTON_SIZE = 20;
    private static final int TEXT_FIELD_WIDTH = 112;
    private static final int GAP = 4;
    private static final int ELEMENT_HEIGHT = 20;

    private static final ButtonTextures STANDARD_BUTTON_BACKGROUND = new ButtonTextures(
        Identifier.of(PrivitMod.MOD_ID, "textures/gui/button_bg_common.png"),
        Identifier.of(PrivitMod.MOD_ID, "textures/gui/button_bg_common_pressed.png"),
        Identifier.of(PrivitMod.MOD_ID, "textures/gui/button_bg_common_active.png")
    );

    private static final Identifier ICON_CREATE_REGION = Identifier.of(PrivitMod.MOD_ID, "textures/gui/create_region_icon.png");
    private static final Identifier ICON_DELETE_REGION = Identifier.of(PrivitMod.MOD_ID, "textures/gui/delete_region_icon.png");
    private static final Identifier ICON_SHOW_GRID = Identifier.of(PrivitMod.MOD_ID, "textures/gui/hide_grid_region_icon.png");
    private static final Identifier ICON_HIDE_GRID = Identifier.of(PrivitMod.MOD_ID, "textures/gui/show_grid_region_icon.png");

    private RegionAreaWidget areaWidget;
    private CustomTextField nameField;
    private GuiButton colorButton;
    private GuiButton gridButton;
    private GuiButton createDeleteButton;

    private final List<ClickableWidget> widgets = new ArrayList<>();

    public RegionPropertiesTab(
            RegionGuiController controller,
            TextRenderer textRenderer,
            int x, int y, int width, int height) {
        this.controller = controller;
        this.textRenderer = textRenderer;
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

        areaWidget = new RegionAreaWidget(this.controller, textRenderer, widgetX, widgetY, widgetWidth, fieldHeight);
    }

    private void initPropertiesPanel() {
        int areaBottom = (y + 95) + 12;
        int mainPanelY = areaBottom + 8;
        int totalButtonsWidth = 3 * BUTTON_SIZE + 2 * GAP;
        int totalWidth = TEXT_FIELD_WIDTH + GAP + totalButtonsWidth;
        int startX = x + (width - totalWidth) / 2;
        int buttonX = startX + TEXT_FIELD_WIDTH + GAP;

        nameField = new CustomTextField(textRenderer, startX, mainPanelY, TEXT_FIELD_WIDTH, ELEMENT_HEIGHT, Text.empty());
        nameField.setMaxLength(24);
        nameField.setText(this.controller.getLocalState().name());
        nameField.setCustomBackground(
                Identifier.of(PrivitMod.MOD_ID, "textures/gui/text_field_bg.png"),
                Identifier.of(PrivitMod.MOD_ID, "textures/gui/text_field_bg_active.png")
        );
        nameField.setChangedListener(newText -> {
            if (newText.equals(this.controller.getLocalState().name()) || newText.isBlank()) return;

            RegionGuiUpdateC2SPacket packet = new RegionGuiUpdateC2SPacket(
                    this.controller.getLocalState().id(),
                    true,
                    MinecraftClient.getInstance().player.getName().getString(),
                    Optional.of(newText), Optional.empty(),
                    Optional.empty(), Optional.empty(), Optional.empty(),
                    Optional.empty(), Optional.empty(), Optional.empty()
            );
            this.controller.sendUpdate(packet);
        });

        colorButton = new GuiButton.Builder(Text.empty(), button -> {
            MinecraftClient.getInstance().setScreen(new RegionColorScreen(this.controller));
        })
                .dimensions(buttonX, mainPanelY, BUTTON_SIZE, BUTTON_SIZE)
                .icon(getColorPreviewIcon(), BUTTON_SIZE, BUTTON_SIZE, 0, -2)
                .tooltip(Tooltip.of(Text.translatable("privit.gui.properties.button.change_color")))
                .setBackground(STANDARD_BUTTON_BACKGROUND)
                .build();

        buttonX += BUTTON_SIZE + GAP;

        gridButton = new GuiButton.Builder(Text.empty(), button -> {
            UUID regionId = this.controller.getLocalState().id();
            boolean current = RegionRenderManager.isGridVisible(regionId);
            boolean newVisible = !current;

            RegionRenderManager.setGridVisible(regionId, newVisible);
            ClientPlayNetworking.send(new RegionGridStateC2SPacket(regionId, newVisible));
            updateGridButton();
        })
                .dimensions(buttonX, mainPanelY, BUTTON_SIZE, BUTTON_SIZE)
                .icon(getGridIcon(), BUTTON_SIZE, BUTTON_SIZE, 0, 0)
                .iconPadding(0)
                .size(BUTTON_SIZE, BUTTON_SIZE)
                .tooltip(Tooltip.of(getGridTooltip()))
                .setBackground(STANDARD_BUTTON_BACKGROUND)
                .build();

        buttonX += BUTTON_SIZE + GAP;

        createDeleteButton = new GuiButton.Builder(Text.empty(), button -> {
            UUID regionId = this.controller.getLocalState().id();

            if (!this.controller.getLocalState().isCreated()) {
                ClientPlayNetworking.send(new RegionCreateC2SPacket(this.controller.getLocalState().toRegionScreenState()));
            } else {
                ClientPlayNetworking.send(new RegionDeleteC2SPacket(regionId));
                ClientPlayNetworking.send(new RegionGridStateC2SPacket(regionId, false));
                RegionRenderManager.disableAndRemove(regionId);
            }

            if (MinecraftClient.getInstance().currentScreen != null)
                MinecraftClient.getInstance().currentScreen.close();
        })
                .dimensions(buttonX, mainPanelY, BUTTON_SIZE, BUTTON_SIZE)
                .icon(ICON_CREATE_REGION, BUTTON_SIZE, BUTTON_SIZE, 0, 0)
                .iconPadding(0)
                .size(BUTTON_SIZE, BUTTON_SIZE)
                .tooltip(Tooltip.of(Text.translatable("privit.gui.properties.button.create_region")))
                .setBackground(STANDARD_BUTTON_BACKGROUND)
                .build();
    }

    @Override
    public List<ClickableWidget> getWidgets() {
        return widgets;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {}

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
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
        for (ClickableWidget w : widgets) {
            w.visible = isTabVisible;
        }
    }

    @Override
    public void updateState() {
        nameField.setText(this.controller.getLocalState().name());
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

        createDeleteButton.setTooltip(Tooltip.of(isCreation ?
                Text.translatable("privit.gui.properties.button.create_region") :
                Text.translatable("privit.gui.properties.button.delete_region")));

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
        return Identifier.of(PrivitMod.MOD_ID, iconPath);
    }

    private void updateGridButton() {
        gridButton.setIcon(getGridIcon(), BUTTON_SIZE, BUTTON_SIZE, 0, 0);
        gridButton.setTooltip(Tooltip.of(getGridTooltip()));
    }

    private Identifier getGridIcon() {
        UUID regionId = this.controller.getLocalState().id();

        return RegionRenderManager.isGridVisible(regionId)
                ? ICON_SHOW_GRID
                : ICON_HIDE_GRID;
    }

    private Text getGridTooltip() {
        UUID regionId = this.controller.getLocalState().id();

        return RegionRenderManager.isGridVisible(regionId)
                ? Text.translatable("privit.gui.properties.button.hide_grid")
                : Text.translatable("privit.gui.properties.button.show_grid");
    }
}