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

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.world.World;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;

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

    @Inject(method = "onUse", at = @At("HEAD"), cancellable = true)
    private void preventManualDoorUse(BlockState state, World world, BlockPos pos,
                                      PlayerEntity player, BlockHitResult hit,
                                      CallbackInfoReturnable<ActionResult> cir) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
        if (!RegionPermissionChecker.isAllowed(serverPlayer, Rule.INTERACT_WITH_DOORS, pos)) {
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_INTERACT_DOOR, Color.RED);
            serverPlayer.networkHandler.sendPacket(new BlockUpdateS2CPacket(pos, state));
            cir.setReturnValue(ActionResult.FAIL);
        }
    }

    @Inject(method = "neighborUpdate", at = @At("TAIL"))
    private void preventRedstoneDoorOpen(BlockState state, World world, BlockPos pos,
                                         Block sourceBlock, BlockPos sourcePos,
                                         boolean notify, CallbackInfo ci) {
        if (!(world instanceof ServerWorld serverWorld)) return;

        boolean allowed = RedstoneReceiverHandler.isActionAllowed(serverWorld, pos, Rule.INTERACT_WITH_DOORS, sourcePos);
        if (allowed) return;

        BlockState closedState = state.with(DoorBlock.OPEN, false);
        world.setBlockState(pos, closedState, 2 | 4); // 2 = notify clients, 4 = skip callbacks
        serverWorld.getPlayers().forEach(p -> p.networkHandler.sendPacket(new BlockUpdateS2CPacket(pos, closedState)));

        UUID responsible = RedstoneReceiverHandler.findResponsiblePlayer(serverWorld, sourcePos);
        if (responsible != null) {
            ServerPlayerEntity serverPlayer = serverWorld.getServer()
                    .getPlayerManager()
                    .getPlayer(responsible);
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_INTERACT_DOOR, Color.RED);
        }
    }

    // it works, but there are a lot of calls from the villager
    @Inject(method = "setOpen", at = @At("HEAD"), cancellable = true)
    private void preventUnauthorizedDoorOpen(@Nullable Entity entity, World world, BlockState state,
                                             BlockPos pos, boolean open, CallbackInfo ci) {
        if (world.isClient || !(world instanceof ServerWorld serverWorld)) return;

        UUID responsible = null;

        if (entity instanceof ServerPlayerEntity player) {
            responsible = player.getUuid();
        } else if (entity instanceof MobEntity mob) {
            InfluencedEntityTracker tracker = WorldRegistry.get(serverWorld)
                    .getTrackerManager()
                    .getInfluencedEntityTracker();
            responsible = tracker.getResponsible(mob);
        }

        if (responsible == null) return;
        if (!RegionPermissionChecker.isAllowed(responsible, Rule.INTERACT_WITH_DOORS, pos, serverWorld)) {
            ci.cancel();

            if (state.get(DoorBlock.OPEN)) {
                world.setBlockState(pos, state.with(DoorBlock.OPEN, false), 10);
            }

            if (entity instanceof ServerPlayerEntity serverPlayer) {
                PlayerNotification.trySend(serverPlayer, NotificationType.DENY_INTERACT_DOOR, Color.RED);
            }
        }
    }
}