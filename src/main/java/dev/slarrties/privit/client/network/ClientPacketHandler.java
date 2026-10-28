package dev.slarrties.privit.client.network;

import dev.slarrties.privit.client.gui.RegionGuiController;
import dev.slarrties.privit.client.gui.RegionGuiLocalState;
import dev.slarrties.privit.client.gui.screen.AddPlayerScreen;
import dev.slarrties.privit.client.gui.screen.RegionScreen;
import dev.slarrties.privit.client.hud.NotificationHudOverlay;
import dev.slarrties.privit.client.hud.RegionNameHudOverlay;
import dev.slarrties.privit.client.util.ClientPlayerIdentityCache;
import dev.slarrties.privit.client.render.state.RegionRenderEntry;
import dev.slarrties.privit.client.render.pipeline.RegionRenderSystem;
import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.network.payload.s2c.*;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

import java.util.Set;
import java.util.List;
import java.util.UUID;

public final class ClientPacketHandler {

    private ClientPacketHandler() {}

    public static void register() {

        // =====================================================================
        // GUI init
        // =====================================================================

        ClientPlayNetworking.registerGlobalReceiver(RegionGuiInitS2CPacket.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft.screen instanceof RegionScreen existingScreen)
                    existingScreen.onClose();

                RegionGuiLocalState localState = RegionGuiLocalState.from(payload.state());
                ClientPlayerIdentityCache cache = ClientPlayerIdentityCache.getInstance();
                Set<UUID> allUuids = cache.collectAllUuids(localState.groups().getAll());
                cache.requestMissingNames(allUuids);
                updateRenderDataFromState(localState);

                RegionGuiController controller = new RegionGuiController(localState);
                if (payload.openGui())
                    minecraft.setScreen(new RegionScreen(controller));
            });
        });

        // =====================================================================
        // GUI update
        // =====================================================================

        ClientPlayNetworking.registerGlobalReceiver(RegionGuiUpdateS2CPacket.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                Minecraft client = Minecraft.getInstance();
                if (!(client.screen instanceof RegionScreen screen)) return;

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

        ClientPlayNetworking.registerGlobalReceiver(RegionGuiCloseS2CPacket.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                Minecraft client = Minecraft.getInstance();
                if (client.screen != null) {
                    client.setScreen(null);
                }
            });
        });

        // =====================================================================
        // Remove region grid
        // =====================================================================

        ClientPlayNetworking.registerGlobalReceiver(RegionGridStateS2CPacket.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                if (!payload.enabled()) {
                    RegionRenderSystem.disableAndRemove(payload.regionId());
                    return;
                }

                Color color = payload.color().orElse(null);
                BoundingBox draft = payload.draftBounds().orElse(null);

                if (color == null || draft == null) return;

                RegionRenderEntry entry = RegionRenderEntry.create(
                                payload.regionId(),
                                color,
                                payload.realBounds().orElse(null))
                        .withDraft(draft)
                        .withConflicts(payload.conflictBounds());
                RegionRenderSystem.updateOrMerge(entry);
                RegionRenderSystem.setGridVisible(payload.regionId(), true);
            });
        });

        // =====================================================================
        // Remove all region grids
        // =====================================================================

        ClientPlayNetworking.registerGlobalReceiver(RegionGridClearS2CPacket.TYPE, (payload, context) -> {
            context.client().execute(RegionRenderSystem::clearAll);
        });

        // =====================================================================
        // Player nicknames cache update
        // =====================================================================

        ClientPlayNetworking.registerGlobalReceiver(PlayerNamesS2CPacket.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                ClientPlayerIdentityCache.getInstance().updateFromPacket(payload.names());
                Minecraft client = Minecraft.getInstance();
                if (client.screen instanceof RegionScreen screen) {
                    RegionGuiController controller = screen.getController();
                    controller.suppressDuringRefresh(screen::applyUpdate);
                }
                if (client.screen instanceof AddPlayerScreen screen) {
                    RegionGuiController controller = screen.getController();
                    controller.suppressDuringRefresh(screen::onCacheUpdated);
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(PlayerSearchResultS2CPacket.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                ClientPlayerIdentityCache.getInstance().updateFromSearchResults(payload.results());
                Minecraft client = Minecraft.getInstance();
                if (client.screen instanceof AddPlayerScreen screen) {
                    RegionGuiController controller = screen.getController();
                    controller.suppressDuringRefresh(screen::onCacheUpdated);
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(RegionHudInfoS2CPacket.TYPE, (payload, context) -> {
            context.client().execute(() ->
                    RegionNameHudOverlay.INSTANCE.update(
                            payload.regionName().orElse(null),
                            payload.color().orElse(null)
                    )
            );
        });

        ClientPlayNetworking.registerGlobalReceiver(HudNotificationS2CPacket.TYPE, (payload, context) -> {
            context.client().execute(() ->
                    NotificationHudOverlay.showNotification(payload.notificationType(), payload.color())
            );
        });

        // =====================================================================
        // Cleanup
        // =====================================================================

        ClientPlayConnectionEvents.DISCONNECT.register((handler, ignoredClient) -> {
            RegionRenderSystem.clearAll();
            RegionNameHudOverlay.INSTANCE.update(null, null);
            ClientPlayerIdentityCache.getInstance().clear();
        });
    }

    private static void updateRenderDataFromState(RegionGuiLocalState state) {
        if (state == null) return;
        UUID regionId = state.id();
        Color color = state.color();
        BoundingBox original = state.realBounds();
        BoundingBox draft = state.draftBounds();
        List<BoundingBox> conflicts = state.conflictBounds();

        RegionRenderEntry entry = RegionRenderEntry.create(regionId, color, original)
                .withDraft(draft)
                .withConflicts(conflicts);
        RegionRenderSystem.updateOrMerge(entry);
    }
}