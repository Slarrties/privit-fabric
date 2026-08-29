package dev.slarrties.privit.server.tracking.protection;

import dev.slarrties.privit.server.tracking.origin.AbstractEntityOriginTracker;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public final class LightningOriginTracker extends AbstractEntityOriginTracker {

    public LightningOriginTracker(ServerWorld world) {
        super(world);
    }

    public void record(Entity lightningOrSource, UUID playerUuid) {
        super.record(lightningOrSource, playerUuid);
    }

    @Nullable
    public UUID getResponsible(LightningEntity lightning) {
        return super.getResponsible(lightning);
    }

    public void remove(Entity entity) {
        super.remove(entity);
    }
}