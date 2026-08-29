package dev.slarrties.privit.server.region.protection.mixin.cause_explosions.ghast;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.tracking.protection.ExplosionOriginTracker;

import net.minecraft.world.World;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.GhastEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.server.world.ServerWorld;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@AssociatedRule(Rule.CAUSE_EXPLOSIONS)
@Mixin(FireballEntity.class)
public abstract class GhastFireballPropagationMixin {

    @Inject(
            method = "<init>(Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/util/math/Vec3d;I)V",
            at = @At("TAIL")
    )
    private void propagateResponsibleFromGhast(World world, LivingEntity owner, Vec3d velocity, int explosionPower, CallbackInfo ci) {
        FireballEntity fireball = (FireballEntity) (Object) this;

        if (owner instanceof GhastEntity ghast) {
            if(ghast.getWorld() instanceof ServerWorld serverWorld) {
                ExplosionOriginTracker explosionTracker = WorldRegistry.get(serverWorld)
                        .getTrackerManager()
                        .getExplosionOriginTracker();
                UUID responsible = explosionTracker.getResponsiblePlayer(ghast);

                if (responsible != null) {
                    explosionTracker.record(fireball, responsible);
                }
            }
        }
    }
}