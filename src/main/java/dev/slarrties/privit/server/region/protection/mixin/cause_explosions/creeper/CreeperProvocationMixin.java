package dev.slarrties.privit.server.region.protection.mixin.cause_explosions.creeper;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@AssociatedRule(Rule.CAUSE_EXPLOSIONS)
@Mixin(Creeper.class)
public abstract class CreeperProvocationMixin {

    @Inject(method = "setTarget", at = @At("HEAD"))
    private void onCreeperProvoked(LivingEntity target, CallbackInfo ci) {
        Creeper creeper = (Creeper) (Object) this;

        if (target instanceof ServerPlayer serverPlayer) {
            if (creeper.level() instanceof ServerLevel serverLevel) {
                WorldRegistry.get(serverLevel).getTrackerManager()
                        .getExplosionOriginTracker()
                        .record(creeper, serverPlayer.getUUID());
            }
        }
    }
}