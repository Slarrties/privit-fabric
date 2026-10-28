package dev.slarrties.privit.server.region.protection.mixin.interact_with_trapdoors;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.tracking.redstone.handler.RedstoneReceiverHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@AssociatedRule(Rule.INTERACT_WITH_TRAPDOORS)
@Mixin(TrapDoorBlock.class)
public abstract class InteractTrapdoorMixin {

    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
    private void preventTrapdoorUse(BlockState state, Level level, BlockPos pos,
                                    Player player, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (RegionPermissionChecker.isAllowed(serverPlayer.getUUID(), Rule.INTERACT_WITH_TRAPDOORS, pos, serverPlayer.level())) return;

        PlayerNotification.trySend(serverPlayer, NotificationType.DENY_INTERACT_TRAPDOOR, Color.RED);
        serverPlayer.connection.send(new ClientboundBlockUpdatePacket(pos, state));
        cir.setReturnValue(InteractionResult.FAIL);
    }

    @Inject(method = "neighborChanged", at = @At("TAIL"))
    private void preventRedstoneTrapdoorOpen(BlockState state, Level level, BlockPos pos, Block block,
                                             @Nullable Orientation orientation, boolean movedByPiston, CallbackInfo ci) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        BlockPos sourcePos = orientation != null ? pos.relative(orientation.getFront()) : pos;
        boolean allowed = RedstoneReceiverHandler.isActionAllowed(serverLevel, pos, Rule.INTERACT_WITH_TRAPDOORS, sourcePos);
        if (allowed) return;

        BlockState closedState = state.setValue(TrapDoorBlock.OPEN, false);
        level.setBlock(pos, closedState, 2 | 4); // 2 = notify clients, 4 = skip callbacks
        serverLevel.getPlayers(p -> true).forEach(p -> p.connection.send(new ClientboundBlockUpdatePacket(pos, closedState)));
        UUID responsible = RedstoneReceiverHandler.findResponsiblePlayer(serverLevel, sourcePos);
        if (responsible != null) {
            ServerPlayer serverPlayer = serverLevel.getServer()
                    .getPlayerList()
                    .getPlayer(responsible);
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_INTERACT_TRAPDOOR, Color.RED);
        }
    }
}