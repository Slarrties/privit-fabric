package dev.slarrties.privit.server.region.protection.mixin.interact_with_boats;

import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.tracking.protection.BoatOriginTracker;
import dev.slarrties.privit.server.tracking.protection.MinecartOriginTracker;
import dev.slarrties.privit.server.tracking.protection.InfluencedEntityTracker;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(Entity.class)
public abstract class EntityVehicleMountingMixin {

    @Inject(method = "startRiding(Lnet/minecraft/world/entity/Entity;ZZ)Z", at = @At("RETURN"))
    private void onEntityMountsVehicle(Entity entityToRide, boolean force, boolean sendEventAndTriggers,
                                       CallbackInfoReturnable<Boolean> cir) {
        Entity passenger = (Entity) (Object) this;
        if (!cir.getReturnValue()) return;
        if (passenger.level().isClientSide()) return;
        if (!(entityToRide instanceof VehicleEntity vehicle)) return;
        if (vehicle.level() instanceof ServerLevel serverLevel) {
            UUID vehicleOwner = null;

            if (vehicle instanceof Boat boat) {
                BoatOriginTracker boatTracker = WorldRegistry.get(serverLevel)
                        .getTrackerManager()
                        .getBoatOriginTracker();
                vehicleOwner = boatTracker.getResponsible(boat);
            } else if (vehicle instanceof AbstractMinecart minecart) {
                MinecartOriginTracker minecartTracker = WorldRegistry.get(serverLevel)
                        .getTrackerManager()
                        .getMinecartOriginTracker();
                vehicleOwner = minecartTracker.getResponsible(minecart);
            }

            if (vehicleOwner != null) {
                InfluencedEntityTracker entityTracker = WorldRegistry.get(serverLevel)
                        .getTrackerManager()
                        .getInfluencedEntityTracker();
                entityTracker.record(passenger, vehicleOwner);
            }
        }
    }
}