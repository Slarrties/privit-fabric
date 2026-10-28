package dev.slarrties.privit.server.region.protection.mixin.cause_explosions.wither;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.tracking.protection.ExplosionOriginTracker;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.projectile.hurtingprojectile.WitherSkull;
import net.minecraft.server.level.ServerLevel;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@AssociatedRule(Rule.CAUSE_EXPLOSIONS)
@Mixin(WitherSkull.class)
public abstract class WitherSkullPropagationMixin {

    @Inject(
            method = "<init>(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/phys/Vec3;)V",
            at = @At("TAIL")
    )
    private void propagateFromWither(Level level, LivingEntity mob, Vec3 direction, CallbackInfo ci) {
        WitherSkull skull = (WitherSkull) (Object) this;
        if (skull.getOwner() instanceof WitherBoss wither && wither.level() instanceof ServerLevel serverLevel) {
            ExplosionOriginTracker explosionOriginTracker = WorldRegistry.get(serverLevel)
                    .getTrackerManager()
                    .getExplosionOriginTracker();
            UUID responsible = explosionOriginTracker.getResponsible(wither);
            if (responsible != null) explosionOriginTracker.record(skull, responsible);
        }
    }
}