package dev.slarrties.privit.server.region.protection.mixin.interact_with_levers;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule(Rule.INTERACT_WITH_LEVERS)
@Mixin(LeverBlock.class)
public abstract class InteractLeverMixin {

    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
    private void preventLeverUse(BlockState state, Level level, BlockPos pos,
                                 Player player, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer.getUUID(), Rule.INTERACT_WITH_LEVERS, pos, serverPlayer.level());
        if (!allowed) {
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_INTERACT_LEVER, Color.RED);
            serverPlayer.connection.send(new ClientboundBlockUpdatePacket(pos, state));
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}