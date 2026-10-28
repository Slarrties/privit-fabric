package dev.slarrties.privit.client.gui.widget.list;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.client.gui.widget.GuiButton;
import dev.slarrties.privit.common.util.PlayerIdentity;

import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.List;
import java.util.function.Consumer;

public class PlayerListWidget extends BaseScrollableListWidget<PlayerIdentity> {

    private static final Identifier DELETE_ICON = Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "textures/gui/delete_region_icon.png");
    private static final WidgetSprites BUTTON_BACKGROUND = new WidgetSprites(
            Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "textures/gui/button_paper_common.png"),
            Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "textures/gui/button_paper_pressed.png"),
            Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "textures/gui/button_paper_active.png")
    );
    private final Consumer<PlayerIdentity> onRemovePlayer;
    private boolean isOwnerGroup;

    public PlayerListWidget(
            int x, int y, int width, int height, int itemHeight,
            Consumer<PlayerIdentity> onRemovePlayer,
            Runnable onAddClicked,
            boolean isOwnerGroup
    ) {
        super(x, y, width, height, itemHeight, null, null, onAddClicked);
        this.onRemovePlayer = onRemovePlayer;
        this.isOwnerGroup = isOwnerGroup;
        setScrollbarPadding(0);
        setContentPadding(0);
    }

    public void refreshRemoveButtons() {
        for (Entry<PlayerIdentity> entry : entries) {
            if (entry instanceof PlayerEntry) {
                ((PlayerEntry) entry).updateRemoveButtonState();
            }
        }
    }

    public void setIsOwnerGroup(boolean isOwner) {
        this.isOwnerGroup = isOwner;
        refreshRemoveButtons();
    }

    @Override
    public void updateEntries(List<PlayerIdentity> data) {
        super.updateEntries(data);
        refreshRemoveButtons();
    }

    @Override
    protected void addDataEntries(List<PlayerIdentity> data) {
        for (PlayerIdentity identity : data) {
            entries.add(new PlayerEntry(identity));
        }
    }

    @Override
    protected Entry<PlayerIdentity> createAddEntry() {
        return new AddPlayerEntry();
    }

    @Override
    protected boolean shouldSelectFirstByDefault() {
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {}

    private class PlayerEntry extends Entry<PlayerIdentity> {
        private final GuiButton removeButton;

        public PlayerEntry(PlayerIdentity identity) {
            super(identity, null);

            this.removeButton = new GuiButton.Builder(
                    Component.literal("×"),
                    btn -> {
                        if (onRemovePlayer != null) {
                            onRemovePlayer.accept(identity);
                        }
                    }
            )
                    .dimensions(0, 0, 20, 20)
                    .icon(DELETE_ICON, 20, 20, 0, -2)
                    .setBackground(BUTTON_BACKGROUND)
                    .build();
            updateRemoveButtonState();
        }

        private void updateRemoveButtonState() {
            int realMemberCount = PlayerListWidget.this.entries.size() - 1;
            boolean canRemove = !isOwnerGroup || realMemberCount > 1;
            removeButton.active = canRemove;

            if (canRemove) {
                removeButton.setTooltip(Tooltip.create(Component.translatable("privit.gui.groups.groups.remove_player")));
            } else {
                removeButton.setTooltip(Tooltip.create(Component.translatable("privit.gui.groups.groups.cannot_remove_player")));
            }
        }

        @Override
        public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int index, int y, int x, int entryWidth, int entryHeight,
                                             int mouseX, int mouseY, boolean hovered, boolean selected, float a) {
            String displayName = data.getDisplayName();
            int textColor = hovered ? 0xFFFFFFFF : 0xFFF5F5F5;

            graphics.text(
                    Minecraft.getInstance().font,
                    Component.literal(displayName),
                    x + 4,
                    y + (entryHeight - 8) / 2,
                    textColor,
                    true
            );

            removeButton.setX(x + entryWidth - 22);
            removeButton.setY(y + (entryHeight - 18) / 2);
            updateRemoveButtonState();
            removeButton.extractRenderState(graphics, mouseX, mouseY, a);

            graphics.fill(x + 6, y + entryHeight - 1, x + entryWidth - 6, y + entryHeight, 0x10FFFFFF);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            return removeButton.mouseClicked(event, doubleClick);
        }

        @Override
        public void setFocused(boolean focused) {}
        @Override
        public boolean isFocused() { return false; }
    }

    private class AddPlayerEntry extends Entry<PlayerIdentity> {
        public AddPlayerEntry() { super(null, onAddClicked); }

        @Override
        public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int index, int y, int x, int entryWidth, int entryHeight,
                                             int mouseX, int mouseY, boolean hovered, boolean selected, float a) {
            int textColor = hovered ? 0xFFA8FFC0 : 0xFF8AE0A2;
            Minecraft minecraft = Minecraft.getInstance();

            graphics.text(
                    minecraft.font,
                    Component.translatable("privit.gui.groups.players.add"),
                    x + 6,
                    y + (entryHeight - 8) / 2,
                    textColor,
                    true
            );
            graphics.fill(x + 6, y + entryHeight - 1, x + entryWidth - 6, y + entryHeight, 0x10FFFFFF);
        }

        @Override
        public void setFocused(boolean focused) {}
        @Override
        public boolean isFocused() { return false; }
    }
}