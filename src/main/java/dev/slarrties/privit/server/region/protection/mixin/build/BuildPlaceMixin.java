package dev.slarrties.privit.server.region.protection.mixin.build;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.tracking.context.BlockFallContext;
import dev.slarrties.privit.server.tracking.protection.BlockFallOriginTracker;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule({Rule.BUILD, Rule.CAUSE_BLOCK_FALL})
@Mixin(ServerPlayerGameMode.class)
public abstract class BuildPlaceMixin {

    @Final @Shadow
    protected ServerPlayer player;

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void preventPlace(ServerPlayer player, Level level, ItemStack stack,
                              InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (!(player.getItemInHand(hand).getItem() instanceof BlockItem)) return;

        BlockPos placePos = hitResult.getBlockPos().offset(hitResult.getDirection().getUnitVec3i());
        if (!RegionPermissionChecker.isAllowed(player.getUUID(), Rule.BUILD, placePos, player.level())) {
            player.connection.send(new ClientboundBlockUpdatePacket(placePos, player.level().getBlockState(placePos)));

            int slotId = hand == InteractionHand.MAIN_HAND ? player.getInventory().getSelectedSlot() + 36 : 45; // TODO
            player.connection.send(new ClientboundContainerSetSlotPacket(-2, 0, slotId, player.getItemInHand(hand)));

            PlayerNotification.trySend(player, NotificationType.DENY_PLACE_BLOCK, Color.RED);
            cir.setReturnValue(InteractionResult.FAIL);
            return;
        }

        BlockFallOriginTracker blockFallTracker = WorldRegistry.get(player.level())
                .getTrackerManager()
                .getBlockFallOriginTracker();
        blockFallTracker.record(placePos, player.getUUID());
        BlockFallContext.push(player.getUUID(), placePos);
    }

    @Inject(method = "useItemOn", at = @At("RETURN"))
    private void popBlockFallContext(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand,
                                     BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        BlockFallContext.pop();
    }
}