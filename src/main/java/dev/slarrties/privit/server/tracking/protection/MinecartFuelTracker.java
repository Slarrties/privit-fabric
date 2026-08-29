package dev.slarrties.privit.server.tracking.protection;

import dev.slarrties.privit.server.tracking.origin.AbstractEntityOriginTracker;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.entity.vehicle.FurnaceMinecartEntity;

import java.util.UUID;

public final class MinecartFuelTracker extends AbstractEntityOriginTracker {

    public MinecartFuelTracker(ServerWorld world) {
        super(world);
    }

    public void recordResponsible(FurnaceMinecartEntity minecart, UUID playerUuid) {
        super.record(minecart, playerUuid);
    }

    public UUID getResponsiblePlayer(FurnaceMinecartEntity minecart) {
        return super.getResponsible(minecart);
    }

    public void remove(FurnaceMinecartEntity minecart) {
        super.remove(minecart);
    }
}