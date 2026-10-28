package dev.slarrties.privit.server.region.protection.mixin.build;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.tracking.protection.BlockFallOriginTracker;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ScaffoldingBlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule({Rule.BUILD, Rule.CAUSE_BLOCK_FALL})
@Mixin(ScaffoldingBlockItem.class)
public abstract class ScaffoldingPlaceMixin {

    @Inject(method = "updatePlacementContext", at = @At("RETURN"), cancellable = true)
    private void preventUnauthorizedScaffoldingPlace(BlockPlaceContext context, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;
        if (!(context.getPlayer() instanceof ServerPlayer player)) return;

        BlockPos placePos = context.getClickedPos();
        if (!RegionPermissionChecker.isAllowed(player.getUUID(), Rule.BUILD, placePos, player.level())) {
            PlayerNotification.trySend(player, NotificationType.DENY_PLACE_BLOCK, Color.RED);
            cir.setReturnValue(false);
            return;
        }

        BlockFallOriginTracker blockFallTracker = WorldRegistry.get(player.level())
                .getTrackerManager()
                .getBlockFallOriginTracker();
        blockFallTracker.record(placePos, player.getUUID());
    }
}