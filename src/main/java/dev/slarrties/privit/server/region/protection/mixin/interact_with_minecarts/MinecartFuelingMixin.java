package dev.slarrties.privit.server.region.protection.mixin.interact_with_minecarts;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;

import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.FurnaceMinecartEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule(Rule.INTERACT_WITH_MINECARTS)
@Mixin(FurnaceMinecartEntity.class)
public abstract class MinecartFuelingMixin {

    @Inject(method = "interact", at = @At("HEAD"))
    private void recordFueler(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        Entity minecart = (Entity) (Object) this;

        if (minecart instanceof FurnaceMinecartEntity && minecart.getWorld() instanceof ServerWorld serverWorld) {
            WorldRegistry.get(serverWorld)
                    .getTrackerManager()
                    .getMinecartFuelTracker()
                    .record(minecart, player.getUuid());
        }
    }
}