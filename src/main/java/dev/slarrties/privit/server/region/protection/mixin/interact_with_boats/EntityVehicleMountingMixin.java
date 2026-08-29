package dev.slarrties.privit.server.region.protection.mixin.interact_with_boats;

import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.tracking.protection.BoatOriginTracker;
import dev.slarrties.privit.server.tracking.protection.MinecartOriginTracker;
import dev.slarrties.privit.server.tracking.protection.InfluencedEntityTracker;

import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.entity.vehicle.VehicleEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.server.world.ServerWorld;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

// shouldn't it be in this package?
@Mixin(Entity.class)
public abstract class EntityVehicleMountingMixin {

    @Inject(method = "startRiding(Lnet/minecraft/entity/Entity;Z)Z", at = @At("RETURN"))
    private void onEntityMountsVehicle(Entity vehicle, boolean force, CallbackInfoReturnable<Boolean> cir) {
        Entity passenger = (Entity) (Object) this;

        if (!cir.getReturnValue()) return;
        if (!(vehicle instanceof VehicleEntity)) return;
        if (passenger.getWorld().isClient) return;

        if (vehicle.getWorld() instanceof ServerWorld serverWorld) {
            UUID vehicleOwner = null;

            if (vehicle instanceof BoatEntity boat) {
                BoatOriginTracker boatTracker = WorldRegistry.get(serverWorld)
                        .getTrackerManager()
                        .getBoatOriginTracker();
                vehicleOwner = boatTracker.getResponsiblePlayer(boat);
            }

            else if (vehicle instanceof AbstractMinecartEntity minecart) {
                MinecartOriginTracker minecartTracker = WorldRegistry.get(serverWorld)
                        .getTrackerManager()
                        .getMinecartOriginTracker();
                vehicleOwner = minecartTracker.getResponsiblePlayer(minecart);
            }

            if (vehicleOwner != null) {
                InfluencedEntityTracker entityTracker = WorldRegistry.get(serverWorld)
                        .getTrackerManager()
                        .getInfluencedEntityTracker();

                entityTracker.record(passenger, vehicleOwner);
            }
        }
    }
}