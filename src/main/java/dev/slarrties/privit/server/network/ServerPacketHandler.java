package dev.slarrties.privit.server.network;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.gui.state.RegionGuiState;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.common.network.payload.c2s.*;
import dev.slarrties.privit.common.network.payload.s2c.*;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.Region;
import dev.slarrties.privit.server.region.RegionManager;
import dev.slarrties.privit.server.region.gui.RegionGuiMapping;
import dev.slarrties.privit.server.region.gui.RegionGuiSession;
import dev.slarrties.privit.server.region.gui.RegionGuiSessions;
import dev.slarrties.privit.server.identity.PlayerIdentityCache;
import dev.slarrties.privit.server.tracking.PlayerRegionPresenceTracker;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import java.util.*;

public final class ServerPacketHandler {

    public static void register() {

        // =====================================================================
        // Create region
        // =====================================================================

        ServerPlayNetworking.registerGlobalReceiver(RegionCreateC2SPacket.ID, (server, player, handler, buf, responseSender) -> {
            RegionCreateC2SPacket payload = RegionCreateC2SPacket.read(buf);

            ServerWorld world = player.getServerWorld();
            RegionManager regionManager = WorldRegistry.get(world).getRegionManager();
            RegionGuiSessions sessions = WorldRegistry.get(world).getRegionGuiSessions();

            Region newRegion = RegionGuiMapping.toRegion(payload.state());
            RegionGuiSession session = sessions.find(newRegion.id());

            if (session == null || !session.isOwner(player.getUuid())) {
                ServerPacketSender.send(player, new HudNotificationS2CPacket(NotificationType.DENY_MANAGE, Color.RED));
                return;
            }

            RegionManager.OpResult result = regionManager.tryCreate(newRegion, player);
            if (!result.isSuccess()) {
                if (session != null) session.notifyCommitRejected(result, player);
                ServerPacketSender.send(player, new HudNotificationS2CPacket(result.type(), Color.RED));
                return;
            }

            PlayerRegionPresenceTracker.refreshPlayersHud(world, newRegion.bounds());
            PlayerNotification.trySend(player, NotificationType.REGION_CREATED, Color.GREEN);
            WorldRegistry.get(world).getGridSubscriptions().publish(newRegion, world.getPlayers());
            sessions.close(newRegion.id());
        });

        // =====================================================================
        // Update region
        // =====================================================================

        ServerPlayNetworking.registerGlobalReceiver(RegionUpdateC2SPacket.ID, (server, player, handler, buf, responseSender) -> {
            RegionUpdateC2SPacket payload = RegionUpdateC2SPacket.read(buf);

            ServerWorld world = player.getServerWorld();
            RegionManager regionManager = WorldRegistry.get(world).getRegionManager();
            RegionGuiSessions sessions = WorldRegistry.get(world).getRegionGuiSessions();

            Region incoming = RegionGuiMapping.toRegion(payload.state());
            Optional<Region> oldRegionOpt = regionManager.getById(incoming.id());

            if (oldRegionOpt.isEmpty()) {
                ServerPacketSender.send(player, new HudNotificationS2CPacket(NotificationType.REGION_NOT_FOUND, Color.RED));
                return;
            }

            RegionManager.OpResult result = regionManager.tryUpdate(oldRegionOpt.get(), incoming, player);
            if (!result.isSuccess()) {
                RegionGuiSession session = sessions.find(incoming.id());
                if (session != null) session.notifyCommitRejected(result, player);
                ServerPacketSender.send(player, new HudNotificationS2CPacket(result.type(), Color.RED));
                return;
            }

            PlayerRegionPresenceTracker.refreshPlayersHud(world, incoming.bounds());
            RegionGuiSession session = sessions.find(incoming.id());
            if (session != null) session.replaceCommitted(incoming);
            ServerPacketSender.send(player, new HudNotificationS2CPacket(NotificationType.REGION_UPDATED, Color.GREEN));
        });

        // =====================================================================
        // Delete region
        // =====================================================================

        ServerPlayNetworking.registerGlobalReceiver(RegionDeleteC2SPacket.ID, (server, player, handler, buf, responseSender) -> {
            RegionDeleteC2SPacket payload = RegionDeleteC2SPacket.read(buf);
            ServerWorld world = player.getServerWorld();
            RegionManager regionManager = WorldRegistry.get(world).getRegionManager();
            UUID regionId = payload.regionId();
            Optional<Region> regionOpt = regionManager.getById(regionId);

            if (regionOpt.isEmpty()) {
                ServerPacketSender.send(player, new HudNotificationS2CPacket(NotificationType.REGION_NOT_FOUND, Color.RED));
                return;
            }

            Region region = regionOpt.get();
            RegionManager.OpResult result = regionManager.tryDelete(regionId, player);
            if (!result.isSuccess()) {
                ServerPacketSender.send(player, new HudNotificationS2CPacket(result.type(), Color.RED));
                return;
            }

            PlayerRegionPresenceTracker.refreshPlayersHud(world, region.bounds());
            ServerPacketSender.send(player, new HudNotificationS2CPacket(NotificationType.REGION_DELETED, Color.GREEN));
            WorldRegistry.get(world).getRegionGuiSessions().close(regionId);
            WorldRegistry.get(world).getGridSubscriptions().hide(regionId, world.getPlayers());
        });

        // =====================================================================
        // GUI open request
        // =====================================================================

        ServerPlayNetworking.registerGlobalReceiver(RegionGuiRequestC2SPacket.ID, (server, player, handler, buf, responseSender) -> {
            RegionGuiRequestC2SPacket payload = RegionGuiRequestC2SPacket.read(buf);
            RegionGuiSessions sessions = WorldRegistry.get(player.getServerWorld()).getRegionGuiSessions();
            var openResult = sessions.open(player, payload.tablePos());

            if (openResult instanceof RegionGuiSessions.OpenResult.Opened opened) {
                ServerPacketSender.send(player, new RegionGuiInitS2CPacket(opened.state(), true));
            } else if (openResult instanceof RegionGuiSessions.OpenResult.Denied denied) {
                ServerPacketSender.send(player, new HudNotificationS2CPacket(denied.type(), Color.RED));
            }
        });

        // =====================================================================
        // GUI state sync
        // =====================================================================

        ServerPlayNetworking.registerGlobalReceiver(RegionGuiUpdateC2SPacket.ID, (server, player, handler, buf, responseSender) -> {
            RegionGuiUpdateC2SPacket payload = RegionGuiUpdateC2SPacket.read(buf);
            ServerPlayerEntity playerEntity = player;
            RegionGuiSessions sessions = WorldRegistry.get(playerEntity.getServerWorld()).getRegionGuiSessions();
            RegionGuiSession session = sessions.find(payload.regionId());

            if (session == null) {
                PrivitMod.LOGGER.warn("[Server] RegionGuiUpdate for missing session {}", payload.regionId());
                return;
            }

            var deltaResult = session.applyDelta(playerEntity, payload);

            if (deltaResult instanceof RegionGuiSession.DeltaResult.Applied) {
                // Do nothing
            } else if (deltaResult instanceof RegionGuiSession.DeltaResult.Denied denied) {
                ServerPacketSender.send(playerEntity, snapshotPacket(session.state(), playerEntity.getName().getString()));
                ServerPacketSender.send(playerEntity, new HudNotificationS2CPacket(denied.type(), Color.RED));
            } else if (deltaResult instanceof RegionGuiSession.DeltaResult.Locked locked) {
                ServerPacketSender.send(playerEntity, snapshotPacket(locked.state(), locked.editorName()));
            }
        });

        // =====================================================================
        // Cancel GUI changes
        // =====================================================================

        ServerPlayNetworking.registerGlobalReceiver(RegionGuiCancelC2SPacket.ID, (server, player, handler, buf, responseSender) -> {
            RegionGuiCancelC2SPacket payload = RegionGuiCancelC2SPacket.read(buf);

            RegionGuiSession session = WorldRegistry.get(player.getServerWorld())
                    .getRegionGuiSessions()
                    .find(payload.regionId());

            if (session == null) {
                PrivitMod.LOGGER.warn("[Server] Cancel for missing session {}", payload.regionId());
                return;
            }

            var cancelResult = session.cancel(player);

            if (cancelResult instanceof RegionGuiSession.CancelResult.Restored) {
                // Do nothing
            } else if (cancelResult instanceof RegionGuiSession.CancelResult.NoRegionToRevert) {
                // Do nothing
            } else if (cancelResult instanceof RegionGuiSession.CancelResult.Denied denied) {
                ServerPacketSender.send(player, new HudNotificationS2CPacket(denied.type(), Color.RED));
            }
        });

        // =====================================================================
        // Grid state sync
        // =====================================================================

        ServerPlayNetworking.registerGlobalReceiver(RegionGridStateC2SPacket.ID, (server, player, handler, buf, responseSender) -> {
            RegionGridStateC2SPacket payload = RegionGridStateC2SPacket.read(buf);

            ServerWorld world = player.getServerWorld();
            UUID regionId = payload.regionId();
            var grids = WorldRegistry.get(world).getGridSubscriptions();
            var session = WorldRegistry.get(world).getRegionGuiSessions().find(regionId);

            if (payload.enabled()) {
                grids.subscribe(regionId, player);
                if (session != null) {
                    var state = session.state();
                    ServerPacketSender.send(player, RegionGridStateS2CPacket.show(
                            state.getId(),
                            state.getColor(),
                            state.getRealBounds(),
                            state.getDraftBounds(),
                            state.getConflictBounds()
                    ));
                } else {
                    WorldRegistry.get(world).getRegionManager().getById(regionId).ifPresent(region ->
                            ServerPacketSender.send(player, RegionGridStateS2CPacket.show(
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
                ServerPacketSender.send(player, RegionGridStateS2CPacket.hide(regionId));
            }
        });

        // =====================================================================
        // Player list (UUID + nickname)
        // =====================================================================

        ServerPlayNetworking.registerGlobalReceiver(RequestPlayerNamesC2SPacket.ID, (server, player, handler, buf, responseSender) -> {
            RequestPlayerNamesC2SPacket payload = RequestPlayerNamesC2SPacket.read(buf);

            Map<UUID, String> result = new HashMap<>();
            for (UUID uuid : payload.uuids()) {
                String name = PlayerIdentityCache.getNameByUuid(uuid);
                if (name != null) {
                    result.put(uuid, name);
                }
            }
            ServerPacketSender.send(player, new PlayerNamesS2CPacket(result));
        });

        // =====================================================================
        // AddPlayerList search result
        // =====================================================================

        ServerPlayNetworking.registerGlobalReceiver(SearchPlayersRequestC2SPacket.ID, (server, player, handler, buf, responseSender) -> {
            SearchPlayersRequestC2SPacket payload = SearchPlayersRequestC2SPacket.read(buf);

            String query = payload.query().trim();
            if (query.isEmpty() || query.length() < 2) {
                ServerPacketSender.send(player, new PlayerSearchResultS2CPacket(Map.of()));
                return;
            }

            int limit = Math.min(payload.limit(), 100);
            Map<UUID, String> results = PlayerIdentityCache.searchPlayers(query, limit, player);
            ServerPacketSender.send(player, new PlayerSearchResultS2CPacket(results));
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