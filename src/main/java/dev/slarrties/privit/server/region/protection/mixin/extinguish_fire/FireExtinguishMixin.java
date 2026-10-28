package dev.slarrties.privit.server.region.protection.mixin.extinguish_fire;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.core.BlockPos;
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

@AssociatedRule(Rule.EXTINGUISH_FIRE)
@Mixin(ServerPlayerGameMode.class)
public abstract class FireExtinguishMixin {

    @Final @Shadow
    protected ServerPlayer player;

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void preventFireExtinguish(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (player.level().getBlockState(pos).getBlock() != Blocks.FIRE) return;
        if (!RegionPermissionChecker.isAllowed(player.getUUID(), Rule.EXTINGUISH_FIRE, pos, player.level())) {
            PlayerNotification.trySend(player, NotificationType.DENY_EXTINGUISH_FIRE, Color.RED);
            player.connection.send(new ClientboundBlockUpdatePacket(pos, player.level().getBlockState(pos)));
            cir.setReturnValue(false);
        }
    }
}