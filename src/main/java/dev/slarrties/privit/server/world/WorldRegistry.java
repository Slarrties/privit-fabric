package dev.slarrties.privit.server.world;

import net.minecraft.server.level.ServerLevel;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;

import java.util.Map;
import java.util.WeakHashMap;

public final class WorldRegistry {

    private static final Map<ServerLevel, DimensionContext> dimensions = new WeakHashMap<>();

    private WorldRegistry() {}

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(s -> {
            for (ServerLevel world : s.getAllLevels()) {
                get(world).onServerTick();
            }
        });

        ServerLevelEvents.LOAD.register((s, world) -> get(world).onWorldLoad());
        ServerLevelEvents.UNLOAD.register((s, world) -> get(world).onWorldUnload());
    }

    public static DimensionContext get(ServerLevel world) {
        return dimensions.computeIfAbsent(world, DimensionContext::new);
    }
}