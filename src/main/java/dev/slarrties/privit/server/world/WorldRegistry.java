package dev.slarrties.privit.server.world;

import net.minecraft.server.world.ServerWorld;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;

import java.util.Map;
import java.util.WeakHashMap;

public final class WorldRegistry {

    private static final Map<ServerWorld, DimensionContext> dimensions = new WeakHashMap<>();

    private WorldRegistry() {}

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(s -> {
            for (ServerWorld world : s.getWorlds()) {
                get(world).onServerTick();
            }
        });

        ServerWorldEvents.LOAD.register((s, world) -> get(world).onWorldLoad());
        ServerWorldEvents.UNLOAD.register((s, world) -> get(world).onWorldUnload());
    }

    public static DimensionContext get(ServerWorld world) {
        return dimensions.computeIfAbsent(world, DimensionContext::new);
    }
}