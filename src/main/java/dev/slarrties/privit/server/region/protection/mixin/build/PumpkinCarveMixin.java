package dev.slarrties.privit.server.region.protection.mixin.build;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.PumpkinBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule(Rule.BUILD)
@Mixin(PumpkinBlock.class)
public abstract class PumpkinCarveMixin {

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void preventCarve(ItemStack stack, BlockState state, Level level, BlockPos pos,
                              Player player, InteractionHand hand, BlockHitResult hit,
                              CallbackInfoReturnable<InteractionResult> cir) {
        if (level.isClientSide()) return;
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (!stack.is(Items.SHEARS)) return;

        if (!RegionPermissionChecker.isAllowed(serverPlayer.getUUID(), Rule.BUILD, pos, serverPlayer.level())) {
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_PLACE_BLOCK, Color.RED);
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}