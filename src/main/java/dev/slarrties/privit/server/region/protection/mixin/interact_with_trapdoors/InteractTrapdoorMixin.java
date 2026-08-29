package dev.slarrties.privit.server.region.protection.mixin.interact_with_trapdoors;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.tracking.redstone.handler.RedstoneReceiverHandler;

import net.minecraft.world.World;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@AssociatedRule(Rule.INTERACT_WITH_TRAPDOORS)
@Mixin(TrapdoorBlock.class)
public abstract class InteractTrapdoorMixin {

    @Inject(method = "onUse", at = @At("HEAD"), cancellable = true)
    private void preventTrapdoorUse(BlockState state, World world, BlockPos pos,
                                    PlayerEntity player, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
        if (RegionPermissionChecker.isAllowed(serverPlayer, Rule.INTERACT_WITH_TRAPDOORS, pos)) return;

        PlayerNotification.trySend(serverPlayer, NotificationType.DENY_INTERACT_TRAPDOOR, Color.RED);
        serverPlayer.networkHandler.sendPacket(new BlockUpdateS2CPacket(pos, state));
        cir.setReturnValue(ActionResult.FAIL);
    }

    @Inject(method = "neighborUpdate", at = @At("TAIL"))
    private void preventRedstoneTrapdoorOpen(BlockState state, World world, BlockPos pos, Block sourceBlock,
                                             BlockPos sourcePos, boolean notify, CallbackInfo ci) {
        if (!(world instanceof ServerWorld serverWorld)) return;

        boolean allowed = RedstoneReceiverHandler.isActionAllowed(serverWorld, pos, Rule.INTERACT_WITH_TRAPDOORS, sourcePos);
        if (allowed) return;

        BlockState closedState = state.with(TrapdoorBlock.OPEN, false);
        world.setBlockState(pos, closedState, 2 | 4); // notify clients + skip callbacks
        serverWorld.getPlayers().forEach(p -> p.networkHandler.sendPacket(new BlockUpdateS2CPacket(pos, closedState)));

        UUID responsible = RedstoneReceiverHandler.findResponsiblePlayer(serverWorld, sourcePos);
        if (responsible != null) {
            ServerPlayerEntity serverPlayer = serverWorld.getServer()
                    .getPlayerManager()
                    .getPlayer(responsible);
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_INTERACT_TRAPDOOR, Color.RED);
        }
    }
}