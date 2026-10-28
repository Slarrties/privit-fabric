package dev.slarrties.privit.server.region.protection.mixin.interact_with_boats;

import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.tracking.protection.InfluencedEntityTracker;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.damagesource.DamageSource;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VehicleEntity.class)
public abstract class VehicleDestructionMixin {

    @Inject(
            method = "hurtServer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/vehicle/VehicleEntity;destroy(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;)V"
            )
    )
    private void onKillAndDropSelf(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        recordPassengersOnDestruction(level, source);
    }

    @Inject(
            method = "hurtServer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/vehicle/VehicleEntity;discard()V"
            )
    )
    private void onDiscard(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        recordPassengersOnDestruction(level, source);
    }

    @Unique
    private void recordPassengersOnDestruction(ServerLevel level, DamageSource source) {
        if (!(source.getEntity() instanceof ServerPlayer attacker)) return;

        VehicleEntity vehicle = (VehicleEntity) (Object) this;
        InfluencedEntityTracker entityTracker = WorldRegistry.get(level)
                .getTrackerManager()
                .getInfluencedEntityTracker();

        for (Entity passenger : vehicle.getPassengers()) {
            entityTracker.record(passenger, attacker.getUUID());
        }
    }
}