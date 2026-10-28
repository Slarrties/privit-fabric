package dev.slarrties.privit.server.region.protection.mixin.interact_with_minecarts;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.MinecartFurnace;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule(Rule.INTERACT_WITH_MINECARTS)
@Mixin(MinecartFurnace.class)
public abstract class MinecartFuelingMixin {

    @Inject(method = "interact", at = @At("HEAD"))
    private void recordFueler(Player player, InteractionHand hand, Vec3 location, CallbackInfoReturnable<InteractionResult> cir) {
        Entity minecart = (Entity) (Object) this;

        if (minecart instanceof MinecartFurnace && minecart.level() instanceof ServerLevel serverLevel) {
            WorldRegistry.get(serverLevel)
                    .getTrackerManager()
                    .getMinecartFuelTracker()
                    .record(minecart, player.getUUID());
        }
    }
}