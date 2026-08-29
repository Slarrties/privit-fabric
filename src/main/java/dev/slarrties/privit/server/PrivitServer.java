package dev.slarrties.privit.server;

import dev.slarrties.privit.common.network.payload.s2c.RegionGridClearS2CPacket;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.util.RegionHudService;
import dev.slarrties.privit.server.command.CommandRegistry;
import dev.slarrties.privit.server.identity.PlayerIdentityCache;
import dev.slarrties.privit.server.network.ServerPacketHandler;
import dev.slarrties.privit.server.tracking.protection.TrackerManager;
import dev.slarrties.privit.server.tracking.PlayerRegionPresenceTracker;
import dev.slarrties.privit.server.region.protection.handler.HandlerRegistry;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;

public class PrivitServer implements DedicatedServerModInitializer {

    @Override
    public void onInitializeServer() {
        CommandRegistrationCallback.EVENT.register(CommandRegistry::register);
        ServerLifecycleEvents.SERVER_STARTING.register(this::onServerStarting);
        ServerPlayConnectionEvents.DISCONNECT.register(this::onPlayerDisconnect);
    }

    private void onServerStarting(MinecraftServer server) {
        PlayerIdentityCache.init(server);
        ServerPacketHandler.register();
        HandlerRegistry.registerAll();
        PlayerRegionPresenceTracker.init();
        RegionHudService.init();
        WorldRegistry.init();

        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
            ServerPlayNetworking.send(player, new RegionGridClearS2CPacket());
        });

        ServerEntityWorldChangeEvents.AFTER_ENTITY_CHANGE_WORLD.register(
                (originalEntity, newEntity, originWorld, destinationWorld) -> {
                    if (originWorld == destinationWorld) return;

                    TrackerManager from = WorldRegistry.get(originWorld).getTrackerManager();
                    TrackerManager to = WorldRegistry.get(destinationWorld).getTrackerManager();

                    from.transferEntity(originalEntity, newEntity, to);
                }
        );
    }

    private void onPlayerDisconnect(ServerPlayNetworkHandler handler, MinecraftServer server) {
        PlayerNotification.clearForPlayer(handler.getPlayer());
    }
}