package dev.slarrties.privit.server.tracking.protection;

import dev.slarrties.privit.server.tracking.origin.AbstractEntityOriginTracker;

import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;

import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public class InfluencedEntityTracker extends AbstractEntityOriginTracker {

    public InfluencedEntityTracker(ServerWorld world) {
        super(world);
    }

    public void record(Entity entity, UUID playerUuid) {
        super.record(entity, playerUuid);
    }

    @Nullable
    public UUID getResponsible(Entity entity) {
        return super.getResponsible(entity);
    }

    public void remove(Entity entity) {
        super.remove(entity);
    }

    public void propagate(Entity from, Entity to) {
        super.propagate(from, to);
    }
}