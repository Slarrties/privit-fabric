package dev.slarrties.privit.server.region.protection.mixin.extinguish_fire;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

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

@AssociatedRule(Rule.EXTINGUISH_FIRE)
@Mixin(ServerPlayerInteractionManager.class)
public abstract class FireExtinguishMixin {

    @Final @Shadow protected ServerPlayerEntity player;

    @Inject(method = "tryBreakBlock", at = @At("HEAD"), cancellable = true)
    private void preventFireExtinguish(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (player.getWorld().getBlockState(pos).getBlock() != Blocks.FIRE) return;
        if (!RegionPermissionChecker.isAllowed(player, Rule.EXTINGUISH_FIRE, pos)) {
            PlayerNotification.trySend(player, NotificationType.DENY_EXTINGUISH_FIRE, Color.RED);
            player.networkHandler.sendPacket(new BlockUpdateS2CPacket(pos, player.getWorld().getBlockState(pos)));
            cir.setReturnValue(false);
        }
    }
}