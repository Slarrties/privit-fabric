package dev.slarrties.privit.client.network;

import dev.slarrties.privit.client.gui.RegionGuiController;
import dev.slarrties.privit.client.gui.RegionGuiLocalState;
import dev.slarrties.privit.client.gui.screen.AddPlayerScreen;
import dev.slarrties.privit.client.gui.screen.RegionScreen;
import dev.slarrties.privit.client.hud.NotificationHudOverlay;
import dev.slarrties.privit.client.hud.RegionNameHudOverlay;
import dev.slarrties.privit.client.render.RegionRenderCache;
import dev.slarrties.privit.client.render.RegionRenderEntry;
import dev.slarrties.privit.client.render.RegionRenderManager;
import dev.slarrties.privit.client.util.ClientPlayerIdentityCache;
import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.network.payload.s2c.*;

import net.minecraft.util.math.BlockBox;
import net.minecraft.client.MinecraftClient;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class ClientPacketHandler {

    private ClientPacketHandler() {}

    public static void register() {

        // =====================================================================
        // GUI init
        // =====================================================================

        ClientPlayNetworking.registerGlobalReceiver(RegionGuiInitS2CPacket.ID, (client, handler, buf, responseSender) -> {
            RegionGuiInitS2CPacket payload = RegionGuiInitS2CPacket.read(buf);

            client.execute(() -> {
                if (MinecraftClient.getInstance().currentScreen instanceof RegionScreen existingScreen)
                    existingScreen.close();

                RegionGuiLocalState localState = RegionGuiLocalState.from(payload.state());
                ClientPlayerIdentityCache cache = ClientPlayerIdentityCache.getInstance();
                Set<UUID> allUuids = cache.collectAllUuids(localState.groups().getAll());
                cache.requestMissingNames(allUuids);
                updateRenderDataFromState(localState);

                RegionGuiController controller = new RegionGuiController(localState);
                if (payload.openGui())
                    MinecraftClient.getInstance().setScreen(new RegionScreen(controller));
            });
        });

        // =====================================================================
        // GUI update
        // =====================================================================

        ClientPlayNetworking.registerGlobalReceiver(RegionGuiUpdateS2CPacket.ID, (client, handler, buf, responseSender) -> {
            RegionGuiUpdateS2CPacket payload = RegionGuiUpdateS2CPacket.read(buf);

            client.execute(() -> {
                MinecraftClient mcClient = MinecraftClient.getInstance();
                if (!(mcClient.currentScreen instanceof RegionScreen screen)) return;

                RegionGuiController controller = screen.getController();
                RegionGuiLocalState oldState = controller.getLocalState();
                RegionGuiLocalState updated = oldState.withUpdate(payload);

                ClientPlayerIdentityCache cache = ClientPlayerIdentityCache.getInstance();
                Set<UUID> allUuids = cache.collectAllUuids(updated.groups().getAll());
                updateRenderDataFromState(updated);
                cache.requestMissingNames(allUuids);

                controller.applyServerUpdate(updated, screen::applyUpdate);
            });
        });

        // =====================================================================
        // GUI close
        // =====================================================================

        ClientPlayNetworking.registerGlobalReceiver(RegionGuiCloseS2CPacket.ID, (client, handler, buf, responseSender) -> {
            RegionGuiCloseS2CPacket payload = RegionGuiCloseS2CPacket.read(buf);

            client.execute(() -> {
                MinecraftClient mcClient = MinecraftClient.getInstance();
                if (mcClient.currentScreen != null) {
                    mcClient.currentScreen.close();
                }
            });
        });

        // =====================================================================
        // Remove region grid
        // =====================================================================

        ClientPlayNetworking.registerGlobalReceiver(RegionGridStateS2CPacket.ID, (client, handler, buf, responseSender) -> {
            RegionGridStateS2CPacket payload = RegionGridStateS2CPacket.read(buf);

            client.execute(() -> {
                if (!payload.enabled()) {
                    RegionRenderManager.disableAndRemove(payload.regionId());
                    return;
                }

                Color color = payload.color().orElse(null);
                BlockBox draft = payload.draftBounds().orElse(null);
                if (color == null || draft == null) return;

                RegionRenderEntry entry = RegionRenderEntry.create(
                                payload.regionId(),
                                color,
                                payload.realBounds().orElse(null))
                        .withDraft(draft)
                        .withConflicts(payload.conflictBounds());

                RegionRenderCache.getInstance().updateOrMerge(entry);
                RegionRenderManager.setGridVisible(payload.regionId(), true);
            });
        });

        // =====================================================================
        // Remove all region grids
        // =====================================================================

        ClientPlayNetworking.registerGlobalReceiver(RegionGridClearS2CPacket.ID, (client, handler, buf, responseSender) -> {
            RegionGridClearS2CPacket payload = RegionGridClearS2CPacket.read(buf);
            client.execute(RegionRenderManager::clearAll);
        });

        // =====================================================================
        // Player nicknames cache update
        // =====================================================================

        ClientPlayNetworking.registerGlobalReceiver(PlayerNamesS2CPacket.ID, (client, handler, buf, responseSender) -> {
            PlayerNamesS2CPacket payload = PlayerNamesS2CPacket.read(buf);

            client.execute(() -> {
                ClientPlayerIdentityCache.getInstance().updateFromPacket(payload.names());
                MinecraftClient mcClient = MinecraftClient.getInstance();

                if (mcClient.currentScreen instanceof RegionScreen screen) {
                    RegionGuiController controller = screen.getController();
                    controller.suppressDuringRefresh(screen::applyUpdate);
                }

                if (mcClient.currentScreen instanceof AddPlayerScreen screen) {
                    RegionGuiController controller = screen.getController();
                    controller.suppressDuringRefresh(screen::onCacheUpdated);
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(PlayerSearchResultS2CPacket.ID, (client, handler, buf, responseSender) -> {
            PlayerSearchResultS2CPacket payload = PlayerSearchResultS2CPacket.read(buf);

            client.execute(() -> {
                ClientPlayerIdentityCache.getInstance().updateFromSearchResults(payload.results());
                MinecraftClient mcClient = MinecraftClient.getInstance();

                if (mcClient.currentScreen instanceof AddPlayerScreen screen) {
                    RegionGuiController controller = screen.getController();
                    controller.suppressDuringRefresh(screen::onCacheUpdated);
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(RegionHudInfoS2CPacket.ID, (client, handler, buf, responseSender) -> {
            RegionHudInfoS2CPacket payload = RegionHudInfoS2CPacket.read(buf);

            client.execute(() ->
                    RegionNameHudOverlay.INSTANCE.update(
                            payload.regionName().orElse(null),
                            payload.color().orElse(null)
                    )
            );
        });

        ClientPlayNetworking.registerGlobalReceiver(HudNotificationS2CPacket.ID, (client, handler, buf, responseSender) -> {
            HudNotificationS2CPacket payload = HudNotificationS2CPacket.read(buf);

            client.execute(() ->
                    NotificationHudOverlay.showNotification(payload.type(), payload.color())
            );
        });

        // =====================================================================
        // Cleanup
        // =====================================================================

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            RegionRenderManager.clearAll();
            RegionNameHudOverlay.INSTANCE.update(null, null);
            ClientPlayerIdentityCache.getInstance().clear();
        });
    }

    private static void updateRenderDataFromState(RegionGuiLocalState state) {
        if (state == null) return;
        UUID regionId = state.id();
        Color color = state.color();
        BlockBox original = state.realBounds();
        BlockBox draft = state.draftBounds();
        List<BlockBox> conflicts = state.conflictBounds();

        RegionRenderEntry entry = RegionRenderEntry.create(regionId, color, original)
                .withDraft(draft)
                .withConflicts(conflicts);
        RegionRenderCache.getInstance().updateOrMerge(entry);
    }
}