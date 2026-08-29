package dev.slarrties.privit.server.region.protection.mixin.interact_with_boats;

import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.tracking.protection.InfluencedEntityTracker;

import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.VehicleEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VehicleEntity.class)
public abstract class VehicleDestructionMixin {

    @Inject(
            method = "damage",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/vehicle/VehicleEntity;killAndDropSelf(Lnet/minecraft/entity/damage/DamageSource;)V"
            )
    )
    private void onKillAndDropSelf(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        recordPassengersOnDestruction(source);
    }

    @Inject(
            method = "damage",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/vehicle/VehicleEntity;discard()V"
            )
    )
    private void onDiscard(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        recordPassengersOnDestruction(source);
    }

    @Unique
    private void recordPassengersOnDestruction(DamageSource source) {
        VehicleEntity vehicle = (VehicleEntity) (Object) this;

        if (!(vehicle.getWorld() instanceof ServerWorld serverWorld)) return;
        if (!(source.getAttacker() instanceof ServerPlayerEntity attacker)) return;

        InfluencedEntityTracker entityTracker = WorldRegistry.get(serverWorld)
                .getTrackerManager()
                .getInfluencedEntityTracker();

        for (Entity passenger : vehicle.getPassengerList()) {
            entityTracker.record(passenger, attacker.getUuid());
        }
    }
}