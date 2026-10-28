package dev.slarrties.privit.server.region.protection.mixin.build.use_item;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.tags.BlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule(Rule.BUILD)
@Mixin(PotionItem.class)
public abstract class WaterBottleMudMixin {

    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void preventMudCreation(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (context.getLevel().isClientSide()) return;
        if (!(context.getPlayer() instanceof ServerPlayer serverPlayer)) return;

        PotionContents contents = context.getItemInHand().get(DataComponents.POTION_CONTENTS);
        if (contents == null || contents.potion().isEmpty() || !contents.potion().get().is(Potions.WATER)) return;

        BlockPos pos = context.getClickedPos();
        BlockState state = context.getLevel().getBlockState(pos);
        if (!state.is(BlockTags.CONVERTABLE_TO_MUD)) return;

        if (!RegionPermissionChecker.isAllowed(serverPlayer.getUUID(), Rule.BUILD, pos, serverPlayer.level())) {
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_BREAK_BLOCK, Color.RED);
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}