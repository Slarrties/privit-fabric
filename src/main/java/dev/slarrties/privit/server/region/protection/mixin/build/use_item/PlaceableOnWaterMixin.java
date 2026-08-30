package dev.slarrties.privit.server.region.protection.mixin.build.use_item;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.world.World;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.PlaceableOnWaterItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule(Rule.BUILD)
@Mixin(PlaceableOnWaterItem.class)
public abstract class PlaceableOnWaterMixin {

    @Inject(method = "useOnBlock", at = @At("HEAD"), cancellable = true)
    private void preventUseOnBlock(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        if (context.getWorld().isClient()) return;
        if (!(context.getPlayer() instanceof ServerPlayerEntity serverPlayer)) return;

        BlockPos clicked = context.getBlockPos();
        BlockPos placePos = clicked.up();

        if (!RegionPermissionChecker.isAllowed(serverPlayer.getUuid(), Rule.BUILD, placePos, serverPlayer.getServerWorld())
                && !RegionPermissionChecker.isAllowed(serverPlayer.getUuid(), Rule.BUILD, clicked, serverPlayer.getServerWorld())) {
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_BREAK_BLOCK, Color.RED);
            cir.setReturnValue(ActionResult.FAIL);
        }
    }

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void preventUse(World world, PlayerEntity user, Hand hand,
                            CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
        if (world.isClient()) return;
        if (!(user instanceof ServerPlayerEntity serverPlayer)) return;

        HitResult hit = serverPlayer.raycast(5.0, 0.0F, true);
        if (!(hit instanceof BlockHitResult blockHit)) return;

        BlockPos clicked = blockHit.getBlockPos();
        BlockPos placePos = clicked.up();

        if (!RegionPermissionChecker.isAllowed(serverPlayer.getUuid(), Rule.BUILD, placePos, serverPlayer.getServerWorld())
                && !RegionPermissionChecker.isAllowed(serverPlayer.getUuid(), Rule.BUILD, clicked, serverPlayer.getServerWorld())) {
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_BREAK_BLOCK, Color.RED);
            cir.setReturnValue(TypedActionResult.fail(serverPlayer.getStackInHand(hand)));
        }
    }
}