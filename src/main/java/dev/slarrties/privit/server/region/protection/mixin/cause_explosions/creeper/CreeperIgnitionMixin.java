package dev.slarrties.privit.server.region.protection.mixin.cause_explosions.creeper;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule(Rule.CAUSE_EXPLOSIONS)
@Mixin(Creeper.class)
public abstract class CreeperIgnitionMixin {

    @Inject(
            method = "mobInteract",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/monster/Creeper;ignite()V",
                    shift = At.Shift.BEFORE
            )
    )
    private void onPlayerIgnitedCreeper(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        Creeper creeper = (Creeper) (Object) this;
        if (creeper.level() instanceof ServerLevel serverLevel) {
            WorldRegistry.get(serverLevel).getTrackerManager()
                    .getExplosionOriginTracker()
                    .record(creeper, serverPlayer.getUUID());
        }
    }
}