package dev.slarrties.privit.server.region.protection.mixin.cause_explosions.creeper;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;

import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule(Rule.CAUSE_EXPLOSIONS)
@Mixin(CreeperEntity.class)
public abstract class CreeperIgnitionMixin {

    @Inject(
            method = "interactMob",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/mob/CreeperEntity;ignite()V",
                    shift = At.Shift.BEFORE
            )
    )
    private void onPlayerIgnitedCreeper(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        CreeperEntity creeper = (CreeperEntity) (Object) this;
        if(creeper.getWorld() instanceof ServerWorld serverWorld) {
            WorldRegistry.get(serverWorld).getTrackerManager()
                    .getExplosionOriginTracker().record(creeper, serverPlayer.getUuid());
        }
    }
}