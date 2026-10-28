package dev.slarrties.privit.server.region.protection.mixin.interact_with_animals;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.util.InventorySyncSystem;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule(Rule.INTERACT_WITH_ANIMALS)
@Mixin(BeehiveBlock.class)
public abstract class InteractBeehiveMixin {

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void preventHoneyHarvest(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                     Player player, InteractionHand hand, BlockHitResult hit,
                                     CallbackInfoReturnable<InteractionResult> cir) {
        if (level.isClientSide()) return;
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        if (!stack.is(Items.SHEARS) && !stack.is(Items.GLASS_BOTTLE)) return;
        int honeyLevel = state.getValue(BeehiveBlock.HONEY_LEVEL);
        if (honeyLevel < 5) return;

        if (!RegionPermissionChecker.isAllowed(serverPlayer.getUUID(), Rule.INTERACT_WITH_ANIMALS, pos, serverPlayer.level())) {
            ItemStack current = serverPlayer.getItemInHand(hand);
            if (stack.is(Items.GLASS_BOTTLE) && !current.is(Items.GLASS_BOTTLE)) {
                serverPlayer.setItemInHand(hand, stack.copy());
            }

            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_ANIMAL_TAME_AND_BREED, Color.RED);
            InventorySyncSystem.syncHandSlotStrong(serverPlayer, hand);
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}