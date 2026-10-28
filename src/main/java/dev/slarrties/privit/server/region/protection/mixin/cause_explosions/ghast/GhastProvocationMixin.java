package dev.slarrties.privit.server.region.protection.mixin.cause_explosions.ghast;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@AssociatedRule(Rule.CAUSE_EXPLOSIONS)
@Mixin(Mob.class)
public abstract class GhastProvocationMixin {

    @Inject(method = "setTarget(Lnet/minecraft/world/entity/LivingEntity;)V", at = @At("HEAD"))
    private void onMobTargetPlayer(LivingEntity target, CallbackInfo ci) {
        Mob mob = (Mob) (Object) this;
        if (!(target instanceof ServerPlayer serverPlayer)) return;
        if (!(mob.level() instanceof ServerLevel serverLevel)) return;

        var trackerManager = WorldRegistry.get(serverLevel).getTrackerManager();
        trackerManager.getInfluencedEntityTracker().record(mob, serverPlayer.getUUID());

        if (mob instanceof Ghast) {
            trackerManager.getExplosionOriginTracker().record(mob, serverPlayer.getUUID());
        }
    }
}