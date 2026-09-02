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

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule({Rule.BUILD, Rule.CAUSE_BLOCK_FALL})
@Mixin(ServerPlayerInteractionManager.class)
public abstract class BuildBreakMixin {

    @Final @Shadow protected ServerPlayerEntity player;

    @Inject(method = "tryBreakBlock", at = @At("HEAD"), cancellable = true)
    private void preventBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        Block block = player.getWorld().getBlockState(pos).getBlock();

        if (block == Blocks.FIRE) return;
        if (!RegionPermissionChecker.isAllowed(player.getUuid(), Rule.BUILD, pos, player.getServerWorld())) {
            player.networkHandler.sendPacket(new BlockUpdateS2CPacket(pos, player.getWorld().getBlockState(pos)));
            PlayerNotification.trySend(player, NotificationType.DENY_BREAK_BLOCK, Color.RED);
            cir.setReturnValue(false);
            return;
        }

        BlockFallOriginTracker blockFallTracker = WorldRegistry.get(player.getServerWorld())
                .getTrackerManager()
                .getBlockFallOriginTracker();
        blockFallTracker.record(pos, player.getUuid());
        BlockFallContext.push(player.getUuid(), pos);
    }

    @Inject(method = "tryBreakBlock", at = @At("RETURN"))
    private void popBlockFallContext(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        BlockFallContext.pop();
    }
}