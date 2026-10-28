package dev.slarrties.privit.server.region.protection.mixin.extinguish_fire;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.tracking.protection.CampfireOriginTracker;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule(Rule.EXTINGUISH_FIRE)
@Mixin(ShovelItem.class)
public abstract class ExtinguishFireShovelCampfireMixin {

    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void preventExtinguish(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (context.getLevel().isClientSide()) return;
        if (!(context.getPlayer() instanceof ServerPlayer serverPlayer)) return;

        BlockPos pos = context.getClickedPos();
        BlockState state = context.getLevel().getBlockState(pos);

        if (!(state.getBlock() instanceof CampfireBlock)) return;
        if (!state.getValue(CampfireBlock.LIT)) return;

        if (!RegionPermissionChecker.isAllowed(serverPlayer.getUUID(), Rule.EXTINGUISH_FIRE, pos, serverPlayer.level())) {
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_EXTINGUISH_FIRE, Color.RED);
            cir.setReturnValue(InteractionResult.FAIL);
        } else if (serverPlayer.level() instanceof ServerLevel serverLevel) {
            CampfireOriginTracker tracker = WorldRegistry.get(serverLevel)
                    .getTrackerManager()
                    .getCampfireOriginTracker();
            tracker.remove(pos);
        }
    }
}