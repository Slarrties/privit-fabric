package dev.slarrties.privit.server.region.protection.mixin.interact_with_boats;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.tracking.protection.InfluencedEntityTracker;

import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule(Rule.INTERACT_WITH_BOATS)
@Mixin(BoatEntity.class)
public abstract class VehicleDestructionMixin {

    @Inject(method = "damage", at = @At("TAIL"))
    private void onDamageTail(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            recordPassengersOnDestruction(source);
        }
    }

    @Unique
    private void recordPassengersOnDestruction(DamageSource source) {
        BoatEntity boat = (BoatEntity) (Object) this;

        if (!(boat.getWorld() instanceof ServerWorld serverWorld)) return;
        if (!(source.getAttacker() instanceof ServerPlayerEntity attacker)) return;

        InfluencedEntityTracker entityTracker = WorldRegistry.get(serverWorld)
                .getTrackerManager()
                .getInfluencedEntityTracker();

        for (Entity passenger : boat.getPassengerList()) {
            entityTracker.record(passenger, attacker.getUuid());
        }
    }
}