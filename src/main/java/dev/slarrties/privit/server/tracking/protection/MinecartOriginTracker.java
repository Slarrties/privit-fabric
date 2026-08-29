package dev.slarrties.privit.server.tracking.protection;

import dev.slarrties.privit.server.tracking.origin.AbstractEntityOriginTracker;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;

import java.util.UUID;

public final class MinecartOriginTracker extends AbstractEntityOriginTracker {

    public MinecartOriginTracker(ServerWorld world) {
        super(world);
    }

    public void record(AbstractMinecartEntity minecart, UUID playerUuid) {
        super.record(minecart, playerUuid);
    }

    public void propagate(AbstractMinecartEntity from, AbstractMinecartEntity to) {
        super.propagate(from, to);
    }

    public UUID getResponsiblePlayer(AbstractMinecartEntity minecart) {
        return super.getResponsible(minecart);
    }

    public void remove(AbstractMinecartEntity minecart) {
        super.remove(minecart);
    }
}