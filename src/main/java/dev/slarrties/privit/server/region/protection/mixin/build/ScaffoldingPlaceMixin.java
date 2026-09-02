package dev.slarrties.privit.server.region.protection.mixin.build;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.tracking.protection.BlockFallOriginTracker;

import net.minecraft.util.math.BlockPos;
import net.minecraft.item.ScaffoldingItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule({Rule.BUILD, Rule.CAUSE_BLOCK_FALL})
@Mixin(ScaffoldingItem.class)
public abstract class ScaffoldingPlaceMixin {

    @Inject(method = "getPlacementContext", at = @At("RETURN"), cancellable = true)
    private void preventUnauthorizedScaffoldingPlace(ItemPlacementContext context,
                                                     CallbackInfoReturnable<ItemPlacementContext> cir) {
        ItemPlacementContext resolved = cir.getReturnValue();
        if (resolved == null) return;
        if (!(resolved.getPlayer() instanceof ServerPlayerEntity player)) return;

        BlockPos placePos = resolved.getBlockPos();

        if (!RegionPermissionChecker.isAllowed(player.getUuid(), Rule.BUILD, placePos, player.getServerWorld())) {
            PlayerNotification.trySend(player, NotificationType.DENY_PLACE_BLOCK, Color.RED);
            cir.setReturnValue(null);
            return;
        }

        BlockFallOriginTracker blockFallTracker = WorldRegistry.get(player.getServerWorld())
                .getTrackerManager()
                .getBlockFallOriginTracker();
        blockFallTracker.record(placePos, player.getUuid());
    }
}