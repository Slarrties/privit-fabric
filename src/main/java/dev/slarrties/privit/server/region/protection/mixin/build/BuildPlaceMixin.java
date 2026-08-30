package dev.slarrties.privit.server.region.protection.mixin.build;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.world.World;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule(Rule.BUILD)
@Mixin(ServerPlayerInteractionManager.class)
public abstract class BuildPlaceMixin {

    @Final @Shadow protected ServerPlayerEntity player;

    @Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
    private void preventPlace(ServerPlayerEntity player, World world, ItemStack stack,
                              Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
        if (!(player.getStackInHand(hand).getItem() instanceof BlockItem)) return;

        BlockPos placePos = hitResult.getBlockPos().offset(hitResult.getSide());

        if (!RegionPermissionChecker.isAllowed(player.getUuid(), Rule.BUILD, placePos, player.getServerWorld())) {
            cir.setReturnValue(ActionResult.FAIL);
            player.networkHandler.sendPacket(new BlockUpdateS2CPacket(placePos, player.getWorld().getBlockState(placePos)));
            player.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(-2, 0,
                    hand == Hand.MAIN_HAND ? player.getInventory().selectedSlot : 40, player.getStackInHand(hand)));
            PlayerNotification.trySend(player, NotificationType.DENY_PLACE_BLOCK, Color.RED);
        }
    }
}