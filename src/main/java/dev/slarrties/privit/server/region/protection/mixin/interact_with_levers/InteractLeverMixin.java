package dev.slarrties.privit.server.region.protection.mixin.interact_with_levers;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.world.World;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeverBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule(Rule.INTERACT_WITH_LEVERS)
@Mixin(LeverBlock.class)
public abstract class InteractLeverMixin {

    @Inject(method = "onUse", at = @At("HEAD"), cancellable = true)
    private void preventLeverUse(BlockState state, World world, BlockPos pos,
                                 PlayerEntity player, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer, Rule.INTERACT_WITH_LEVERS, pos);

        if (!allowed) {
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_INTERACT_LEVER, Color.RED);
            serverPlayer.networkHandler.sendPacket(new BlockUpdateS2CPacket(pos, state));
            cir.setReturnValue(ActionResult.FAIL);
        }
    }
}