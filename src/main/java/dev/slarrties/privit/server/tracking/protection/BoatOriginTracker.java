package dev.slarrties.privit.server.tracking.protection;

import dev.slarrties.privit.server.tracking.origin.AbstractEntityOriginTracker;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class BoatOriginTracker extends AbstractEntityOriginTracker {

    public BoatOriginTracker(ServerWorld world) {
        super(world);
    }

    public void record(BoatEntity boat, UUID playerUuid) {
        super.record(boat, playerUuid);
    }

    public void propagate(BoatEntity from, BoatEntity to) {
        super.propagate(from, to);
    }

    @Nullable
    public UUID getResponsiblePlayer(BoatEntity boat) {
        return super.getResponsible(boat);
    }

    public void remove(BoatEntity boat) {
        super.remove(boat);
    }

    public void clear(BoatEntity boat) {
        remove(boat);
    }
}