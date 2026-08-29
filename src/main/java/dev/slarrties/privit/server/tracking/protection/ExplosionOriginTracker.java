package dev.slarrties.privit.server.tracking.protection;

import dev.slarrties.privit.server.tracking.origin.AbstractEntityOriginTracker;

import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;

import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public final class ExplosionOriginTracker extends AbstractEntityOriginTracker {

    public ExplosionOriginTracker(ServerWorld world) {
        super(world);
    }

    @Override
    public void record(Entity entity, UUID playerUuid) { super.record(entity, playerUuid); }

    @Nullable
    public UUID getResponsiblePlayer(Entity entity) { return super.getResponsible(entity); }

    public void remove(Entity entity) { super.remove(entity); }
}