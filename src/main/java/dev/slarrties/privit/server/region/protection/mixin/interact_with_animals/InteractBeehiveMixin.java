package dev.slarrties.privit.server.region.protection.mixin.interact_with_animals;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.util.InventorySyncSystem;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.world.World;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.block.BlockState;
import net.minecraft.block.BeehiveBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule(Rule.INTERACT_WITH_ANIMALS)
@Mixin(BeehiveBlock.class)
public abstract class InteractBeehiveMixin {

    @Inject(method = "onUse", at = @At("HEAD"), cancellable = true)
    private void preventHoneyHarvest(BlockState state, World world, BlockPos pos,
                                     PlayerEntity player, Hand hand, BlockHitResult hit,
                                     CallbackInfoReturnable<ActionResult> cir) {
        if (world.isClient) return;
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        ItemStack stack = player.getStackInHand(hand);
        if (!stack.isOf(Items.SHEARS) && !stack.isOf(Items.GLASS_BOTTLE)) return;

        int honeyLevel = state.get(BeehiveBlock.HONEY_LEVEL);
        if (honeyLevel < 5) return;

        if (!RegionPermissionChecker.isAllowed(serverPlayer, Rule.INTERACT_WITH_ANIMALS, pos)) {
            ItemStack current = serverPlayer.getStackInHand(hand);

            if (stack.isOf(Items.GLASS_BOTTLE) && !current.isOf(Items.GLASS_BOTTLE))
                serverPlayer.setStackInHand(hand, stack.copy());

            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_ANIMAL_TAME_AND_BREED, Color.RED);
            InventorySyncSystem.syncHandSlotStrong(serverPlayer, hand);
            cir.setReturnValue(ActionResult.FAIL);
        }
    }
}