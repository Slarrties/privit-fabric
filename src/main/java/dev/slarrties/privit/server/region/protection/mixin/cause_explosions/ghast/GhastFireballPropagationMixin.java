package dev.slarrties.privit.server.region.protection.mixin.cause_explosions.ghast;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.tracking.protection.ExplosionOriginTracker;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.projectile.hurtingprojectile.Fireball;
import net.minecraft.server.level.ServerLevel;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@AssociatedRule(Rule.CAUSE_EXPLOSIONS)
@Mixin(Fireball.class)
public abstract class GhastFireballPropagationMixin {

    @Inject(
            method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/level/Level;)V",
            at = @At("TAIL")
    )
    private void propagateResponsibleFromGhast(EntityType<?> type, LivingEntity mob, Vec3 direction, Level level, CallbackInfo ci) {
        Fireball fireball = (Fireball) (Object) this;
        if (mob instanceof Ghast ghast) {
            if (ghast.level() instanceof ServerLevel serverLevel) {
                ExplosionOriginTracker explosionTracker = WorldRegistry.get(serverLevel)
                        .getTrackerManager()
                        .getExplosionOriginTracker();
                UUID responsible = explosionTracker.getResponsible(ghast);

                if (responsible != null) {
                    explosionTracker.record(fireball, responsible);
                }
            }
        }
    }
}