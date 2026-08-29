package dev.slarrties.privit.server.region.protection.mixin.cause_explosions.ghast;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.GhastEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@AssociatedRule(Rule.CAUSE_EXPLOSIONS)
@Mixin(MobEntity.class)
public abstract class GhastProvocationMixin {

    @Inject(method = "setTarget(Lnet/minecraft/entity/LivingEntity;)V", at = @At("HEAD"))
    private void onMobTargetPlayer(LivingEntity target, CallbackInfo ci) {
        MobEntity mob = (MobEntity) (Object) this;

        if (!(target instanceof ServerPlayerEntity serverPlayer)) return;
        if (!(mob.getWorld() instanceof ServerWorld serverWorld)) return;

        var trackerManager = WorldRegistry.get(serverWorld).getTrackerManager();
        trackerManager.getInfluencedEntityTracker().record(mob, serverPlayer.getUuid());

        if (mob instanceof GhastEntity) {
            trackerManager.getExplosionOriginTracker().record(mob, serverPlayer.getUuid());
        }
    }
}