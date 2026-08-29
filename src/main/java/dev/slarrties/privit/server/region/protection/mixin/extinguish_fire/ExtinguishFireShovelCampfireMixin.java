package dev.slarrties.privit.server.region.protection.mixin.extinguish_fire;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.tracking.protection.CampfireOriginTracker;

import net.minecraft.block.BlockState;
import net.minecraft.block.CampfireBlock;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule(Rule.EXTINGUISH_FIRE)
@Mixin(ShovelItem.class)
public abstract class ExtinguishFireShovelCampfireMixin {

    @Inject(method = "useOnBlock", at = @At("HEAD"), cancellable = true)
    private void preventExtinguish(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        if (context.getWorld().isClient()) return;
        if (!(context.getPlayer() instanceof ServerPlayerEntity serverPlayer)) return;

        BlockPos pos = context.getBlockPos();
        BlockState state = context.getWorld().getBlockState(pos);

        if (!(state.getBlock() instanceof CampfireBlock)) return;
        if (!state.get(CampfireBlock.LIT)) return;
        if (!RegionPermissionChecker.isAllowed(serverPlayer, Rule.EXTINGUISH_FIRE, pos)) {
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_EXTINGUISH_FIRE, Color.RED);
            cir.setReturnValue(ActionResult.FAIL);
        } else if (serverPlayer.getWorld() instanceof ServerWorld serverWorld) {
            CampfireOriginTracker tracker = WorldRegistry.get(serverWorld)
                    .getTrackerManager()
                    .getCampfireOriginTracker();
            tracker.remove(pos);
        }
    }
}