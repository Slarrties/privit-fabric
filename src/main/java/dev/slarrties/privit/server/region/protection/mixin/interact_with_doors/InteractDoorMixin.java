package dev.slarrties.privit.server.region.protection.mixin.interact_with_doors;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.tracking.protection.InfluencedEntityTracker;
import dev.slarrties.privit.server.tracking.redstone.handler.RedstoneReceiverHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@AssociatedRule(Rule.INTERACT_WITH_DOORS)
@Mixin(DoorBlock.class)
public abstract class InteractDoorMixin {

    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
    private void preventManualDoorUse(BlockState state, Level level, BlockPos pos,
                                      Player player, BlockHitResult hit,
                                      CallbackInfoReturnable<InteractionResult> cir) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (!RegionPermissionChecker.isAllowed(serverPlayer.getUUID(), Rule.INTERACT_WITH_DOORS, pos, serverPlayer.level())) {
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_INTERACT_DOOR, Color.RED);
            serverPlayer.connection.send(new ClientboundBlockUpdatePacket(pos, state));
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(method = "neighborChanged", at = @At("TAIL"))
    private void preventRedstoneDoorOpen(BlockState state, Level level, BlockPos pos,
                                         Block block, @Nullable Orientation orientation,
                                         boolean movedByPiston, CallbackInfo ci) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        BlockPos sourcePos = orientation != null ? pos.relative(orientation.getFront()) : pos;

        boolean allowed = RedstoneReceiverHandler.isActionAllowed(serverLevel, pos, Rule.INTERACT_WITH_DOORS, sourcePos);
        if (allowed) return;

        BlockState closedState = state.setValue(DoorBlock.OPEN, false);
        level.setBlock(pos, closedState, 2 | 4); // 2 = notify clients, 4 = skip callbacks
        serverLevel.getPlayers(p -> true).forEach(p -> p.connection.send(new ClientboundBlockUpdatePacket(pos, closedState)));

        UUID responsible = RedstoneReceiverHandler.findResponsiblePlayer(serverLevel, sourcePos);
        if (responsible != null) {
            ServerPlayer serverPlayer = serverLevel.getServer()
                    .getPlayerList()
                    .getPlayer(responsible);
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_INTERACT_DOOR, Color.RED);
        }
    }

    @Inject(method = "setOpen", at = @At("HEAD"), cancellable = true)
    private void preventUnauthorizedDoorOpen(@Nullable Entity entity, Level level, BlockState state,
                                             BlockPos pos, boolean open, CallbackInfo ci) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) return;

        UUID responsible = null;
        if (entity instanceof ServerPlayer player) {
            responsible = player.getUUID();
        } else if (entity instanceof Mob mob) {
            InfluencedEntityTracker tracker = WorldRegistry.get(serverLevel)
                    .getTrackerManager()
                    .getInfluencedEntityTracker();
            responsible = tracker.getResponsible(mob);
        }

        if (responsible == null) return;

        if (!RegionPermissionChecker.isAllowed(responsible, Rule.INTERACT_WITH_DOORS, pos, serverLevel)) {
            ci.cancel();
            if (state.getValue(DoorBlock.OPEN))
                level.setBlock(pos, state.setValue(DoorBlock.OPEN, false), 10);
            if (entity instanceof ServerPlayer serverPlayer)
                PlayerNotification.trySend(serverPlayer, NotificationType.DENY_INTERACT_DOOR, Color.RED);
        }
    }
}