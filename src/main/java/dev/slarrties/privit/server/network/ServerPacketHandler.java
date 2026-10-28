package dev.slarrties.privit.server.network;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.gui.state.RegionGuiState;
import dev.slarrties.privit.common.network.payload.c2s.*;
import dev.slarrties.privit.common.network.payload.s2c.*;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.Region;
import dev.slarrties.privit.server.region.RegionManager;
import dev.slarrties.privit.server.region.gui.RegionGuiMapping;
import dev.slarrties.privit.server.region.gui.RegionGuiSession;
import dev.slarrties.privit.server.region.gui.RegionGuiSessions;
import dev.slarrties.privit.server.identity.PlayerIdentityCache;
import dev.slarrties.privit.server.tracking.PlayerRegionPresenceTracker;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import java.util.*;

public final class ServerPacketHandler {

    public static void register() {

        // =====================================================================
        // Create region
        // =====================================================================

        ServerPlayNetworking.registerGlobalReceiver(RegionCreateC2SPacket.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            ServerLevel level = player.level();
            RegionManager regionManager = WorldRegistry.get(level).getRegionManager();
            RegionGuiSessions sessions = WorldRegistry.get(level).getRegionGuiSessions();

            Region newRegion = RegionGuiMapping.toRegion(payload.state());
            RegionGuiSession session = sessions.find(newRegion.id());

            if (session == null || !session.isOwner(player.getUUID())) {
                ServerPlayNetworking.send(player, new HudNotificationS2CPacket(NotificationType.DENY_MANAGE, Color.RED));
                return;
            }

            RegionManager.OpResult result = regionManager.tryCreate(newRegion, player);
            if (!result.isSuccess()) {
                if (session != null) session.notifyCommitRejected(result, player);
                ServerPlayNetworking.send(player, new HudNotificationS2CPacket(result.type(), Color.RED));
                return;
            }

            PlayerRegionPresenceTracker.refreshPlayersHud(level, newRegion.bounds());
            PlayerNotification.trySend(player, NotificationType.REGION_CREATED, Color.GREEN);
            WorldRegistry.get(level).getGridSubscriptions().publish(newRegion, level.players());
            sessions.close(newRegion.id());
        });

        // =====================================================================
        // Update region
        // =====================================================================

        ServerPlayNetworking.registerGlobalReceiver(RegionUpdateC2SPacket.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            ServerLevel level = player.level();
            RegionManager regionManager = WorldRegistry.get(level).getRegionManager();
            RegionGuiSessions sessions = WorldRegistry.get(level).getRegionGuiSessions();

            Region incoming = RegionGuiMapping.toRegion(payload.state());
            Optional<Region> oldRegionOpt = regionManager.getById(incoming.id());

            if (oldRegionOpt.isEmpty()) {
                ServerPlayNetworking.send(player, new HudNotificationS2CPacket(NotificationType.REGION_NOT_FOUND, Color.RED));
                return;
            }

            RegionManager.OpResult result = regionManager.tryUpdate(oldRegionOpt.get(), incoming, player);
            if (!result.isSuccess()) {
                RegionGuiSession session = sessions.find(incoming.id());
                if (session != null) session.notifyCommitRejected(result, player);
                ServerPlayNetworking.send(player, new HudNotificationS2CPacket(result.type(), Color.RED));
                return;
            }

            PlayerRegionPresenceTracker.refreshPlayersHud(level, incoming.bounds());
            RegionGuiSession session = sessions.find(incoming.id());
            if (session != null) session.replaceCommitted(incoming);
            ServerPlayNetworking.send(player, new HudNotificationS2CPacket(NotificationType.REGION_UPDATED, Color.GREEN));
        });

        // =====================================================================
        // Delete region
        // =====================================================================

        ServerPlayNetworking.registerGlobalReceiver(RegionDeleteC2SPacket.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            ServerLevel level = player.level();
            RegionManager regionManager = WorldRegistry.get(level).getRegionManager();

            UUID regionId = payload.regionId();
            Optional<Region> regionOpt = regionManager.getById(regionId);

            if (regionOpt.isEmpty()) {
                ServerPlayNetworking.send(player, new HudNotificationS2CPacket(NotificationType.REGION_NOT_FOUND, Color.RED));
                return;
            }

            Region region = regionOpt.get();
            RegionManager.OpResult result = regionManager.tryDelete(regionId, player);

            if (!result.isSuccess()) {
                ServerPlayNetworking.send(player, new HudNotificationS2CPacket(result.type(), Color.RED));
                return;
            }

            PlayerRegionPresenceTracker.refreshPlayersHud(level, region.bounds());
            ServerPlayNetworking.send(player, new HudNotificationS2CPacket(NotificationType.REGION_DELETED, Color.GREEN));
            WorldRegistry.get(level).getRegionGuiSessions().close(regionId);
            WorldRegistry.get(level).getGridSubscriptions().hide(regionId, level.players());
        });

        // =====================================================================
        // GUI open request
        // =====================================================================

        ServerPlayNetworking.registerGlobalReceiver(RegionGuiRequestC2SPacket.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            RegionGuiSessions sessions = WorldRegistry.get(player.level()).getRegionGuiSessions();

            switch (sessions.open(player, payload.tablePos())) {
                case RegionGuiSessions.OpenResult.Opened(var state) ->
                        ServerPlayNetworking.send(player, new RegionGuiInitS2CPacket(state, true));
                case RegionGuiSessions.OpenResult.Denied(var type) ->
                        ServerPlayNetworking.send(player, new HudNotificationS2CPacket(type, Color.RED));
            }
        });

        // =====================================================================
        // GUI state sync
        // =====================================================================

        ServerPlayNetworking.registerGlobalReceiver(RegionGuiUpdateC2SPacket.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            RegionGuiSessions sessions = WorldRegistry.get(player.level()).getRegionGuiSessions();
            RegionGuiSession session = sessions.find(payload.regionId());

            if (session == null) {
                PrivitMod.LOGGER.warn("[Server] RegionGuiUpdate for missing session {}", payload.regionId());
                return;
            }

            switch (session.applyDelta(player, payload)) {
                case RegionGuiSession.DeltaResult.Applied() -> {}
                case RegionGuiSession.DeltaResult.Denied(var type) -> {
                    ServerPlayNetworking.send(player, snapshotPacket(session.state(), player.getName().getString()));
                    ServerPlayNetworking.send(player, new HudNotificationS2CPacket(type, Color.RED));
                }
                case RegionGuiSession.DeltaResult.Locked(var editorName, var state) -> {
                    ServerPlayNetworking.send(player, snapshotPacket(state, editorName));
                    // TODO:
                    // ServerPlayNetworking.send(player, new HudNotificationS2CPacket(
                    //         NotificationType.REGION_GUI_LOCKED,
                    //         Color.YELLOW
                    // ));
                }
            }
        });

        // =====================================================================
        // Cancel GUI changes
        // =====================================================================

        ServerPlayNetworking.registerGlobalReceiver(RegionGuiCancelC2SPacket.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            RegionGuiSession session = WorldRegistry.get(player.level())
                    .getRegionGuiSessions()
                    .find(payload.regionId());

            if (session == null) {
                PrivitMod.LOGGER.warn("[Server] Cancel for missing session {}", payload.regionId());
                return;
            }

            switch (session.cancel(player)) {
                case RegionGuiSession.CancelResult.Restored(var state) -> {}
                case RegionGuiSession.CancelResult.NoRegionToRevert() -> {}
                case RegionGuiSession.CancelResult.Denied(var type) ->
                        ServerPlayNetworking.send(player, new HudNotificationS2CPacket(type, Color.RED));
            }
        });

        // =====================================================================
        // Grid state sync
        // =====================================================================

        ServerPlayNetworking.registerGlobalReceiver(RegionGridStateC2SPacket.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            ServerLevel level = player.level();
            UUID regionId = payload.regionId();

            var grids = WorldRegistry.get(level).getGridSubscriptions();
            var session = WorldRegistry.get(level).getRegionGuiSessions().find(regionId);

            if (payload.enabled()) {
                grids.subscribe(regionId, player);
                if (session != null) {
                    var state = session.state();
                    ServerPlayNetworking.send(player, RegionGridStateS2CPacket.show(
                            state.getId(),
                            state.getColor(),
                            state.getRealBounds(),
                            state.getDraftBounds(),
                            state.getConflictBounds()
                    ));
                } else {
                    WorldRegistry.get(level).getRegionManager().getById(regionId).ifPresent(region ->
                            ServerPlayNetworking.send(player, RegionGridStateS2CPacket.show(
                                    region.id(),
                                    region.color(),
                                    region.bounds(),
                                    region.bounds(),
                                    List.of()
                            ))
                    );
                }
            } else {
                grids.unsubscribe(regionId, player);
                ServerPlayNetworking.send(player, RegionGridStateS2CPacket.hide(regionId));
            }
        });

        // =====================================================================
        // Player list (UUID + nickname)
        // =====================================================================

        ServerPlayNetworking.registerGlobalReceiver(RequestPlayerNamesC2SPacket.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            Map<UUID, String> result = new HashMap<>();

            for (UUID uuid : payload.uuids()) {
                String name = PlayerIdentityCache.getNameByUuid(uuid);
                if (name != null) {
                    result.put(uuid, name);
                }
            }
            ServerPlayNetworking.send(player, new PlayerNamesS2CPacket(result));
        });

        // =====================================================================
        // AddPlayerList search result
        // =====================================================================

        // TODO: protection against too frequent requests?
        ServerPlayNetworking.registerGlobalReceiver(SearchPlayersRequestC2SPacket.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            String query = payload.query().trim();

            if (query.isEmpty() || query.length() < 2) {
                ServerPlayNetworking.send(player, new PlayerSearchResultS2CPacket(Map.of()));
                return;
            }

            int limit = Math.min(payload.limit(), 100);
            Map<UUID, String> results = PlayerIdentityCache.searchPlayers(query, limit);
            ServerPlayNetworking.send(player, new PlayerSearchResultS2CPacket(results));
        });
    }

    // TODO: remove the crutch
    private static RegionGuiUpdateS2CPacket snapshotPacket(RegionGuiState state, String editorName) {
        return new RegionGuiUpdateS2CPacket(
                state.getId(),
                state.isChanged(),
                editorName,
                Optional.of(state.getName()),
                Optional.ofNullable(state.getRealBounds()),
                Optional.ofNullable(state.getDraftBounds()),
                Optional.of(state.getConflictBounds()),
                Optional.of(state.getPivotPos()),
                Optional.of(state.getColor()),
                Optional.of(state.getGroups()),
                Optional.of(state.isCreated()),
                Optional.of(state.isAreaLimitExceeded())
        );
    }
}