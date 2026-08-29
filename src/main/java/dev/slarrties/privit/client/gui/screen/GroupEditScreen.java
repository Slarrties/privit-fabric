package dev.slarrties.privit.client.gui.screen;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.client.gui.RegionGuiController;
import dev.slarrties.privit.client.gui.widget.GuiButton;
import dev.slarrties.privit.client.gui.widget.CustomTextField;
import dev.slarrties.privit.common.region.RegionGroups;
import dev.slarrties.privit.common.region.RegionPlayerGroup;
import dev.slarrties.privit.common.network.payload.c2s.RegionGuiUpdateC2SPacket;

import net.minecraft.text.Text;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ButtonTextures;

import java.util.Optional;

public class GroupEditScreen extends Screen {

    private static final int WIDTH = 180;
    private static final int HEIGHT = 90;
    private static final Identifier ACCEPT_ICON = Identifier.of(PrivitMod.MOD_ID, "textures/gui/accept_changes_icon.png");
    private static final Identifier CANCEL_ICON = Identifier.of(PrivitMod.MOD_ID, "textures/gui/delete_region_icon.png");
    private static final Identifier BACKGROUND = Identifier.of(PrivitMod.MOD_ID, "textures/gui/region_gui_background.png");

    private static final ButtonTextures STANDARD_BUTTON_BACKGROUND = new ButtonTextures(
            Identifier.of(PrivitMod.MOD_ID, "textures/gui/button_bg_common.png"),
            Identifier.of(PrivitMod.MOD_ID, "textures/gui/button_bg_common_pressed.png"),
            Identifier.of(PrivitMod.MOD_ID, "textures/gui/button_bg_common_active.png")
    );

    private static final ButtonTextures WIDE_BUTTON_BACKGROUND = new ButtonTextures(
            Identifier.of(PrivitMod.MOD_ID, "textures/gui/wide_button_bg_common.png"),
            Identifier.of(PrivitMod.MOD_ID, "textures/gui/wide_button_bg_common_pressed.png"),
            Identifier.of(PrivitMod.MOD_ID, "textures/gui/wide_button_bg_common_active.png")
    );

    private final RegionGuiController controller;
    private final RegionPlayerGroup originalGroup;

    private CustomTextField nameField;
    private GuiButton deleteButton;
    private GuiButton acceptButton;
    private GuiButton cancelButton;

    public GroupEditScreen(RegionGuiController controller, RegionPlayerGroup group) {
        super(Text.translatable("privit.gui.group_edit.title"));
        this.controller = controller;
        this.originalGroup = new RegionPlayerGroup(group);
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int centerY = height / 2;
        int bgX = centerX - WIDTH / 2;
        int bgY = centerY - HEIGHT / 2;

        final int SIDE_MARGIN = 10;
        final int TOP_MARGIN = 10;

        int iconSize = 20;
        int row1Y = bgY + TOP_MARGIN + 18;
        int nameFieldWidth = WIDTH - 2 * SIDE_MARGIN - 20 - 8;

        nameField = new CustomTextField(textRenderer, bgX + SIDE_MARGIN, row1Y, nameFieldWidth, 20, Text.empty());
        nameField.setMaxLength(16);
        nameField.setText(originalGroup.getName());
        nameField.setCustomBackground(
                Identifier.of(PrivitMod.MOD_ID, "textures/gui/text_field_bg.png"),
                Identifier.of(PrivitMod.MOD_ID, "textures/gui/text_field_bg_active.png")
        );
        addDrawableChild(nameField);

        deleteButton = new GuiButton.Builder(Text.empty(), btn -> deleteCurrentGroup())
                .dimensions(bgX + WIDTH - SIDE_MARGIN - 20, row1Y, 20, 20)
                .setBackground(STANDARD_BUTTON_BACKGROUND)
                .icon(Items.BARRIER.getDefaultStack())
                .tooltip(Tooltip.of(Text.translatable("privit.gui.group_edit.button.delete")))
                .build();
        addDrawableChild(deleteButton);

        int row2Y = row1Y + 20 + 12;

        acceptButton = new GuiButton.Builder(
                Text.empty(),
                btn -> renameCurrentGroup())
                .dimensions(bgX + SIDE_MARGIN, row2Y, (WIDTH - 2 * SIDE_MARGIN - 8) / 2, 20)
                .setBackground(WIDE_BUTTON_BACKGROUND)
                .icon(ACCEPT_ICON, iconSize, iconSize, 0, -2)
                .tooltip(Tooltip.of(Text.translatable("privit.gui.group_edit.button.accept")))
                .build();
        addDrawableChild(acceptButton);

        cancelButton = new GuiButton.Builder(
                Text.empty(),
                btn -> closeScreen())
                .dimensions(bgX + WIDTH - SIDE_MARGIN - (WIDTH - 2 * SIDE_MARGIN - 8) / 2, row2Y,
                        (WIDTH - 2 * SIDE_MARGIN - 8) / 2, 20)
                .setBackground(WIDE_BUTTON_BACKGROUND)
                .icon(CANCEL_ICON, iconSize, iconSize, 0, -2)
                .tooltip(Tooltip.of(Text.translatable("privit.gui.group_edit.button.cancel")))
                .build();
        addDrawableChild(cancelButton);

        nameField.setChangedListener(newText -> updateAcceptButtonState(newText.trim()));
        updateAcceptButtonState(nameField.getText().trim());
    }

    // =====================================================================
    //
    // =====================================================================

    private void deleteCurrentGroup() {
        RegionGroups currentGroups = controller.getLocalState().groups();

        try {
            RegionGroups updatedGroups = currentGroups.removeGroup(originalGroup.getName());
            sendGroupsUpdate(updatedGroups);
        } catch (UnsupportedOperationException e) {
            // Special groups cannot be deleted (owner / visitors)
        }

        closeScreen();
    }

    private void renameCurrentGroup() {
        String newName = nameField.getText().trim();

        if (newName.isEmpty() || newName.equalsIgnoreCase(originalGroup.getName())) {
            closeScreen();
            return;
        }

        RegionGroups currentGroups = controller.getLocalState().groups();

        try {
            RegionGroups updatedGroups = currentGroups.renameGroup(originalGroup.getName(), newName);
            sendGroupsUpdate(updatedGroups);
        } catch (IllegalArgumentException | UnsupportedOperationException e) {
            // Name is already taken or an attempt to rename the special group.
        }

        closeScreen();
    }

    private void sendGroupsUpdate(RegionGroups updatedGroups) {
        RegionGuiUpdateC2SPacket packet = new RegionGuiUpdateC2SPacket(
                controller.getLocalState().id(),
                true,
                MinecraftClient.getInstance().player.getName().getString(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of(updatedGroups),
                Optional.empty()
        );

        controller.sendUpdate(packet);
    }

    private void closeScreen() {
        client.setScreen(new RegionScreen(controller, RegionScreen.Tab.PLAYER_GROUPS));
    }

    private void updateAcceptButtonState(String newName) {
        if (newName.isEmpty()) {
            acceptButton.active = false;
            acceptButton.setTooltip(Tooltip.of(Text.translatable("privit.gui.group_edit.button.accept.empty")));
            return;
        }

        if (RegionGroups.isNameForbidden(newName)) {
            acceptButton.active = false;
            acceptButton.setTooltip(Tooltip.of(
                    Text.translatable("privit.gui.group_edit.button.accept.forbidden", newName)
            ));
            return;
        }

        RegionGroups currentGroups = controller.getLocalState().groups();

        boolean isDuplicate = currentGroups.findByName(newName)
                .map(g -> !g.getName().equalsIgnoreCase(originalGroup.getName()))
                .orElse(false);

        if (isDuplicate) {
            acceptButton.active = false;
            acceptButton.setTooltip(Tooltip.of(
                    Text.translatable("privit.gui.group_edit.button.accept.duplicate", newName)
            ));
        } else {
            acceptButton.active = true;
            acceptButton.setTooltip(Tooltip.of(Text.translatable("privit.gui.group_edit.button.accept", newName)));
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        int centerX = width / 2;
        int bgY = (height - HEIGHT) / 2;

        context.drawCenteredTextWithShadow(textRenderer,
                this.title,
                centerX, bgY + 10, 0xFFFFFF);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        int x = (width - WIDTH) / 2;
        int y = (height - HEIGHT) / 2;
        context.fillGradient(0, 0, width, height, 0xC0101010, 0xD0101010);
        context.drawTexture(BACKGROUND, x, y, 0, 0, WIDTH, HEIGHT, WIDTH, HEIGHT);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}