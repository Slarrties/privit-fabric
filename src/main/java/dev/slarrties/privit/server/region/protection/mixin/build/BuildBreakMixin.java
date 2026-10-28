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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule({Rule.BUILD, Rule.CAUSE_BLOCK_FALL})
@Mixin(ServerPlayerGameMode.class)
public abstract class BuildBreakMixin {

    @Final @Shadow
    protected ServerPlayer player;

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void preventBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        Block block = player.level().getBlockState(pos).getBlock();
        if (block == Blocks.FIRE) return;
        if (!RegionPermissionChecker.isAllowed(player.getUUID(), Rule.BUILD, pos, player.level())) {
            player.connection.send(new ClientboundBlockUpdatePacket(pos, player.level().getBlockState(pos)));
            PlayerNotification.trySend(player, NotificationType.DENY_BREAK_BLOCK, Color.RED);
            cir.setReturnValue(false);
            return;
        }

        BlockFallOriginTracker blockFallTracker = WorldRegistry.get(player.level())
                .getTrackerManager()
                .getBlockFallOriginTracker();
        blockFallTracker.record(pos, player.getUUID());
        BlockFallContext.push(player.getUUID(), pos);
    }

    @Inject(method = "destroyBlock", at = @At("RETURN"))
    private void popBlockFallContext(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        BlockFallContext.pop();
    }
}