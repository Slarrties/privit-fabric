package dev.slarrties.privit.server.tracking.context;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;

import org.jetbrains.annotations.Nullable;

public final class EntitySpawnContext {
    private static final ThreadLocal<EntitySpawnContext> CURRENT = new ThreadLocal<>();
    private final ServerPlayer player;
    private final Vec3 expectedSpawnPos;

    private EntitySpawnContext(ServerPlayer player, Vec3 expectedSpawnPos) {
        this.player = player;
        this.expectedSpawnPos = expectedSpawnPos;
    }

    public static void push(ServerPlayer player, Vec3 expectedSpawnPos) {
        if (player == null || expectedSpawnPos == null) return;
        CURRENT.set(new EntitySpawnContext(player, expectedSpawnPos));
    }

    public static void pop() {
        CURRENT.remove();
    }

    @Nullable
    public static EntitySpawnContext getCurrent() {
        return CURRENT.get();
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public boolean isNearSpawnPosition(Entity entity) {
        if (entity == null) return false;
        return entity.position().distanceToSqr(expectedSpawnPos) < 2.0;
    }
}