package dev.slarrties.privit.client.gui.widget.list;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.client.gui.widget.GuiButton;
import dev.slarrties.privit.common.util.PlayerIdentity;

import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;

import java.util.List;
import java.util.function.Consumer;

public class PlayerListWidget extends BaseScrollableListWidget<PlayerIdentity> {

    private static final Identifier DELETE_ICON = Identifier.of(PrivitMod.MOD_ID, "textures/gui/delete_region_icon.png");
    private static final ButtonTextures BUTTON_BACKGROUND = new ButtonTextures(
            Identifier.of(PrivitMod.MOD_ID, "textures/gui/button_paper_common.png"),
            Identifier.of(PrivitMod.MOD_ID, "textures/gui/button_paper_pressed.png"),
            Identifier.of(PrivitMod.MOD_ID, "textures/gui/button_paper_active.png")
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
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {}

    private class PlayerEntry extends Entry<PlayerIdentity> {

        private final ButtonWidget removeButton;

        public PlayerEntry(PlayerIdentity identity) {
            super(identity, null);

            this.removeButton = new GuiButton.Builder(
                    Text.literal("×"),
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
                removeButton.setTooltip(Tooltip.of(Text.translatable("privit.gui.groups.groups.remove_player")));
            } else {
                removeButton.setTooltip(Tooltip.of(Text.translatable("privit.gui.groups.groups.cannot_remove_player")));
            }
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight,
                           int mouseX, int mouseY, boolean hovered, boolean selected, float tickDelta) {
            String displayName = data.getDisplayName();
            int textColor = hovered ? 0xFFFFFF : 0xF5F5F5;

            context.drawTextWithShadow(
                    MinecraftClient.getInstance().textRenderer,
                    Text.literal(displayName),
                    x + 4,
                    y + (entryHeight - 8) / 2,
                    textColor
            );
            removeButton.setX(x + entryWidth - 22);
            removeButton.setY(y + (entryHeight - 18) / 2);
            updateRemoveButtonState();
            removeButton.render(context, mouseX, mouseY, tickDelta);
            context.fill(x + 6, y + entryHeight - 1, x + entryWidth - 6, y + entryHeight, 0x10FFFFFF);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return removeButton.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public void setFocused(boolean focused) {}

        @Override
        public boolean isFocused() {
            return false;
        }
    }

    private class AddPlayerEntry extends Entry<PlayerIdentity> {

        public AddPlayerEntry() { super(null, onAddClicked); }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight,
                           int mouseX, int mouseY, boolean hovered, boolean selected, float tickDelta) {

            int textColor = hovered ? 0xA8FFC0 : 0x8AE0A2;

            context.drawTextWithShadow(
                    MinecraftClient.getInstance().textRenderer,
                    Text.translatable("privit.gui.groups.players.add"),
                    x + 6,
                    y + (entryHeight - 8) / 2,
                    textColor
            );
            context.fill(x + 6, y + entryHeight - 1, x + entryWidth - 6, y + entryHeight, 0x10FFFFFF);
        }

        @Override
        public void setFocused(boolean focused) {}

        @Override
        public boolean isFocused() {
            return false;
        }
    }
}