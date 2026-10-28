package dev.slarrties.privit.server.region.protection.mixin.cause_explosions.tnt;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.tracking.protection.ExplosionOriginTracker;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@AssociatedRule(Rule.CAUSE_EXPLOSIONS)
@Mixin(PrimedTnt.class)
public abstract class TntExplosionPropagationMixin {

    @Inject(
            method = "<init>(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/entity/LivingEntity;)V",
            at = @At("TAIL")
    )
    private void propagateResponsiblePlayerOnCreation(Level level, double x, double y, double z, @Nullable LivingEntity igniter, CallbackInfo ci) {
        PrimedTnt tnt = (PrimedTnt) (Object) this;
        if (igniter == null) return;

        if (level instanceof ServerLevel serverLevel) {
            ExplosionOriginTracker explosionOriginTracker = WorldRegistry.get(serverLevel)
                    .getTrackerManager()
                    .getExplosionOriginTracker();

            if (igniter instanceof ServerPlayer player) {
                explosionOriginTracker.record(tnt, player.getUUID());
                return;
            }

            UUID responsible = explosionOriginTracker.getResponsible(igniter);
            if (responsible != null) {
                explosionOriginTracker.record(tnt, responsible);
            }
        }
    }
}