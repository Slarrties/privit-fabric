package dev.slarrties.privit.server.tracking.context;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

public final class EntitySpawnContext {

    private static final ThreadLocal<EntitySpawnContext> CURRENT = new ThreadLocal<>();
    private final ServerPlayerEntity player;
    private final Vec3d expectedSpawnPos;

    private EntitySpawnContext(ServerPlayerEntity player, Vec3d expectedSpawnPos) {
        this.player = player;
        this.expectedSpawnPos = expectedSpawnPos;
    }

    public static void push(ServerPlayerEntity player, Vec3d expectedSpawnPos) {
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

    public ServerPlayerEntity getPlayer() {
        return player;
    }

    public boolean isNearSpawnPosition(Entity entity) {
        if (entity == null) return false;
        return entity.getPos().squaredDistanceTo(expectedSpawnPos) < 2.0;
    }
}