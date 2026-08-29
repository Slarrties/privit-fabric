package dev.slarrties.privit.server.tracking.origin;

import java.util.UUID;
import net.minecraft.entity.Entity;
import org.jetbrains.annotations.Nullable;

public interface EntityOriginTracker extends OriginTracker {

    void record(Entity entity, UUID playerUuid);
    void propagate(Entity from, Entity to);
    void remove(Entity entity);
    @Nullable UUID getResponsible(Entity entity);

}