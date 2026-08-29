package dev.slarrties.privit.server.region.protection.mixin.cause_explosions.wither;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.tracking.protection.ExplosionOriginTracker;

import net.minecraft.world.World;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.server.world.ServerWorld;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@AssociatedRule(Rule.CAUSE_EXPLOSIONS)
@Mixin(WitherSkullEntity.class)
public abstract class WitherSkullPropagationMixin {

    @Inject(
            method = "<init>(Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/util/math/Vec3d;)V",
            at = @At("TAIL")
    )
    private void propagateFromWither(World world, LivingEntity owner, Vec3d velocity, CallbackInfo ci) {
        WitherSkullEntity skull = (WitherSkullEntity) (Object) this;

        if (owner instanceof WitherEntity wither && owner.getWorld() instanceof ServerWorld serverWorld) {
            ExplosionOriginTracker explosionOriginTracker = WorldRegistry.get(serverWorld)
                    .getTrackerManager()
                    .getExplosionOriginTracker();
            UUID responsible = explosionOriginTracker.getResponsiblePlayer(wither);

            if (responsible != null) {
                explosionOriginTracker.record(skull, responsible);
            }
        }
    }
}