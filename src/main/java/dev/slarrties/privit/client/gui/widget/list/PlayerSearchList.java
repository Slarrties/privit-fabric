package dev.slarrties.privit.client.gui.widget.list;

import dev.slarrties.privit.client.util.ClientPlayerIdentityCache;
import dev.slarrties.privit.common.util.PlayerIdentity;

import net.minecraft.text.Text;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;

import java.util.*;
import java.util.function.Consumer;

public class PlayerSearchList extends BaseScrollableListWidget<PlayerIdentity> {

    private final TextRenderer textRenderer;
    private final Consumer<PlayerIdentity> onPlayerSelected;

    private String currentQuery = "";
    private final Set<UUID> onlineUuids = new HashSet<>();

    private final ClientPlayerIdentityCache identityCache = ClientPlayerIdentityCache.getInstance();

    public PlayerSearchList(
            int x, int y, int width, int height,
            TextRenderer textRenderer,
            Consumer<PlayerIdentity> onPlayerSelected
    ) {
        super(x, y, width, height, 22, null, null, null);
        this.textRenderer = textRenderer;
        this.onPlayerSelected = onPlayerSelected;

        setScrollbarPosition(ScrollbarPosition.RIGHT);
        setScrollbarWidth(6);
        setScrollbarPadding(0);
        setContentPadding(2);
    }

    public void updateList(List<PlayerIdentity> availablePlayers, String query) {
        this.currentQuery = query.trim().toLowerCase(Locale.ROOT);

        List<PlayerIdentity> filtered;

        if (currentQuery.isEmpty()) {
            filtered = new ArrayList<>(availablePlayers);
        } else {
            filtered = availablePlayers.stream()
                    .filter(identity -> identity.getDisplayName().toLowerCase(Locale.ROOT)
                            .contains(currentQuery))
                    .sorted((a, b) -> {
                        String nameA = a.getDisplayName().toLowerCase(Locale.ROOT);
                        String nameB = b.getDisplayName().toLowerCase(Locale.ROOT);
                        int idxA = nameA.indexOf(currentQuery);
                        int idxB = nameB.indexOf(currentQuery);
                        return Integer.compare(idxA, idxB);
                    })
                    .toList();
        }

        updateEntries(filtered);
    }

    public void updateList(String query) {
        List<PlayerIdentity> allKnown = new ArrayList<>(identityCache.getAllKnownIdentities());
        updateList(allKnown, query);
    }

    @Override
    protected void addDataEntries(List<PlayerIdentity> data) {
        for (PlayerIdentity identity : data) {
            entries.add(new PlayerEntry(identity));
        }
    }

    @Override
    protected Entry<PlayerIdentity> createAddEntry() {
        return null;
    }

    @Override
    protected boolean shouldSelectFirstByDefault() {
        return false;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        // Можно добавить позже
    }

    // =====================================================================
    //
    // =====================================================================

    private class PlayerEntry extends Entry<PlayerIdentity> {

        public PlayerEntry(PlayerIdentity identity) {
            super(identity, () -> onPlayerSelected.accept(identity));
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight,
                           int mouseX, int mouseY, boolean hovered, boolean selected, float tickDelta) {

            PlayerIdentity identity = data;

            if (hovered || selected) {
                context.fill(x, y, x + entryWidth, y + entryHeight,
                        hovered ? 0x44AAAAAA : 0x88FFFFFF);
            }

            String displayName = identity.getDisplayName();

            context.drawTextWithShadow(textRenderer,
                    Text.literal(displayName),
                    x + 4, y + (entryHeight - 8) / 2,
                    selected ? 0xFFFFAA : 0xFFFFFF);

            boolean isOnline = onlineUuids.contains(identity.uuid());
            if (isOnline) {
                int iconX = x + entryWidth - 24;
                int iconY = y + (entryHeight - 16) / 2;
                context.fill(iconX, iconY, iconX + 16, iconY + 16, 0xFF00FF00);
            }

//            if (!identity.isKnown()) {}
        }

        @Override
        public void setFocused(boolean focused) {
            // не используется
        }

        @Override
        public boolean isFocused() {
            return false;
        }
    }

    public void updateOnlinePlayers(Set<UUID> onlinePlayers) {
        this.onlineUuids.clear();
        this.onlineUuids.addAll(onlinePlayers);
    }
}