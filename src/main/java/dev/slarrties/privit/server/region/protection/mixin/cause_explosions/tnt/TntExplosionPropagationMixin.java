package dev.slarrties.privit.server.region.protection.mixin.cause_explosions.tnt;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.tracking.protection.ExplosionOriginTracker;

import net.minecraft.world.World;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@AssociatedRule(Rule.CAUSE_EXPLOSIONS)
@Mixin(TntEntity.class)
public abstract class TntExplosionPropagationMixin {

    @Inject(
            method = "<init>(Lnet/minecraft/world/World;DDDLnet/minecraft/entity/LivingEntity;)V",
            at = @At("TAIL")
    )
    private void propagateResponsiblePlayerOnCreation(World world, double x, double y, double z, @Nullable LivingEntity igniter, CallbackInfo ci) {
        TntEntity tnt = (TntEntity) (Object) this;

        if (igniter == null) return;
        if(world instanceof ServerWorld serverWorld) {
            ExplosionOriginTracker explosionOriginTracker = WorldRegistry.get(serverWorld).getTrackerManager().getExplosionOriginTracker();

            if (igniter instanceof ServerPlayerEntity player) {
                explosionOriginTracker.record(tnt, player.getUuid());
                return;
            }

            UUID responsible = explosionOriginTracker.getResponsiblePlayer(igniter);
            if (responsible != null) explosionOriginTracker.record(tnt, responsible);
        }
    }
}