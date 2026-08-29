package dev.slarrties.privit.server.region.protection.mixin.cause_explosions.creeper;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.passive.GoatEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@AssociatedRule(Rule.CAUSE_EXPLOSIONS)
@Mixin(CreeperEntity.class)
public abstract class CreeperProvocationMixin {

    @Inject(method = "setTarget", at = @At("HEAD"))
    private void onCreeperProvoked(LivingEntity target, CallbackInfo ci) {
        CreeperEntity creeper = (CreeperEntity) (Object) this;

        if (target instanceof GoatEntity) return;
        if (target instanceof ServerPlayerEntity serverPlayer) {
            if(creeper.getWorld() instanceof ServerWorld serverWorld) {
                WorldRegistry.get(serverWorld).getTrackerManager()
                        .getExplosionOriginTracker().record(creeper, serverPlayer.getUuid());
            }
        }
    }
}