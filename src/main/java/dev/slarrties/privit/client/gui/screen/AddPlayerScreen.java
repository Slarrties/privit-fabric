package dev.slarrties.privit.client.gui.screen;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.client.gui.RegionGuiController;
import dev.slarrties.privit.client.gui.widget.GuiButton;
import dev.slarrties.privit.client.gui.widget.CustomTextField;
import dev.slarrties.privit.client.gui.widget.list.PlayerSearchList;
import dev.slarrties.privit.client.util.ClientPlayerIdentityCache;
import dev.slarrties.privit.common.util.PlayerIdentity;
import dev.slarrties.privit.common.region.RegionGroups;
import dev.slarrties.privit.common.region.RegionPlayerGroup;
import dev.slarrties.privit.common.network.payload.c2s.RegionGuiUpdateC2SPacket;
import dev.slarrties.privit.common.network.payload.c2s.SearchPlayersRequestC2SPacket;

import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

public class AddPlayerScreen extends Screen {

    private static final Identifier ADD_ICON = Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "textures/gui/create_region_icon.png");
    private static final Identifier CANCEL_ICON = Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "textures/gui/delete_region_icon.png");
    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "textures/gui/region_gui_background.png");

    private static final WidgetSprites WIDE_BUTTON_BACKGROUND = new WidgetSprites(
            Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "textures/gui/wide_button_bg_common.png"),
            Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "textures/gui/wide_button_bg_common_pressed.png"),
            Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "textures/gui/wide_button_bg_common_active.png")
    );

    private static final int BG_WIDTH = 210;
    private static final int BG_HEIGHT = 180;
    private static final int BUTTON_SIZE = 20;

    private final RegionGuiController controller;
    private final RegionPlayerGroup group;
    private final Runnable onSuccess;
    private final ClientPlayerIdentityCache identityCache = ClientPlayerIdentityCache.getInstance();

    private CustomTextField searchField;
    private PlayerSearchList playerList;
    private PlayerIdentity selectedPlayer = null;

    public AddPlayerScreen(RegionGuiController controller, RegionPlayerGroup group, Runnable onSuccess) {
        super(Component.empty());
        this.controller = controller;
        this.group = group;
        this.onSuccess = onSuccess;
    }

    @Override
    protected void init() {
        int bgX = (width - BG_WIDTH) / 2;
        int bgY = (height - BG_HEIGHT) / 2;
        int fieldWidth = BG_WIDTH - 20;

        searchField = new CustomTextField(this.font, bgX + 10, bgY + 10, fieldWidth, 20, Component.literal("Поиск игрока..."));
        searchField.setMaxLength(32);
        searchField.setCustomBackground(
                Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "textures/gui/text_field_bg.png"),
                Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "textures/gui/text_field_bg_active.png")
        );
        searchField.setResponder(this::onSearchChanged);
        addRenderableWidget(searchField);

        int listX = bgX + 10;
        int listY = bgY + 40;
        int listWidth = BG_WIDTH - 20;
        int listHeight = BG_HEIGHT - 80;

        playerList = new PlayerSearchList(listX, listY, listWidth, listHeight, this.font, identity -> selectedPlayer = identity);
        addRenderableWidget(playerList);
        refreshPlayerList("");

        int btnY = bgY + BG_HEIGHT - 35;
        int btnW = 80;
        int centerX = width / 2;

        GuiButton addButton = new GuiButton.Builder(Component.empty(), btn -> tryAddPlayer())
                .dimensions(centerX - btnW - 5, btnY, btnW, 20)
                .icon(ADD_ICON, BUTTON_SIZE, BUTTON_SIZE, 0, -2)
                .setBackground(WIDE_BUTTON_BACKGROUND)
                .tooltip(Tooltip.create(Component.translatable("privit.gui.groups.add_player.add")))
                .build();
        addRenderableWidget(addButton);

        GuiButton cancelButton = new GuiButton.Builder(Component.empty(), btn -> this.onClose())
                .dimensions(centerX + 5, btnY, btnW, 20)
                .icon(CANCEL_ICON, BUTTON_SIZE, BUTTON_SIZE, 0, -2)
                .setBackground(WIDE_BUTTON_BACKGROUND)
                .tooltip(Tooltip.create(Component.translatable("privit.gui.groups.add_player.cancel")))
                .build();
        addRenderableWidget(cancelButton);

        setFocused(searchField);
    }

    private void refreshPlayerList(String query) {
        List<PlayerIdentity> knownPlayers = new ArrayList<>(identityCache.getAllKnownIdentities());
        playerList.updateList(knownPlayers, query);
    }

    private void onSearchChanged(String text) {
        String trimmed = text.trim();
        refreshPlayerList(trimmed);
        if (!trimmed.isEmpty() && trimmed.length() >= 2) {
            ClientPlayNetworking.send(new SearchPlayersRequestC2SPacket(trimmed, 50));
        }
    }

    public void onCacheUpdated() {
        String query = searchField != null ? searchField.getValue().trim() : "";
        refreshPlayerList(query);
    }

    public RegionGuiController getController() { return controller; }

    private void tryAddPlayer() {
        if (selectedPlayer == null) return;
        if (group.getMembers().contains(selectedPlayer.uuid())) return;

        RegionGroups currentGroups = controller.getLocalState().groups();
        currentGroups.findByName(group.getName()).ifPresentOrElse(
                oldGroup -> {
                    try {
                        RegionPlayerGroup updatedGroup = new RegionPlayerGroup(oldGroup);
                        updatedGroup.addMember(selectedPlayer.uuid());
                        RegionGroups newGroups = currentGroups.withUpdatedGroup(
                                group.getName(),
                                updatedGroup
                        );
                        RegionGuiUpdateC2SPacket packet = new RegionGuiUpdateC2SPacket(
                                controller.getLocalState().id(),
                                true,
                                Minecraft.getInstance().player.getName().getString(),
                                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                                Optional.empty(), Optional.of(newGroups), Optional.empty()
                        );
                        controller.sendUpdate(packet);
                        if (onSuccess != null) onSuccess.run();
                        this.onClose();
                    } catch (UnsupportedOperationException e) {
                        PrivitMod.LOGGER.error("[AddPlayerScreen::tryAddPlayer] cannot be added in this group");
                    } catch (IllegalArgumentException e) {
                        PrivitMod.LOGGER.error("[AddPlayerScreen::tryAddPlayer] this player is on other group already");
                    }
                },
                () -> PrivitMod.LOGGER.error("[AddPlayerScreen::tryAddPlayer] group not found")
        );
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(new RegionScreen(controller, RegionScreen.Tab.PLAYER_GROUPS));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        this.extractBackground(graphics, mouseX, mouseY, delta);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int x = (width - BG_WIDTH) / 2;
        int y = (height - BG_HEIGHT) / 2;

        graphics.fillGradient(0, 0, width, height, 0xC0101010, 0xD0101010);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                BACKGROUND,
                x, y, 0, 0,
                BG_WIDTH, BG_HEIGHT,
                BG_WIDTH, BG_HEIGHT
        );
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (playerList.mouseClicked(event, doubleClick)) {
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (playerList.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) return true;

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
}