package dev.slarrties.privit.server.region.protection.mixin.eat_chorus_fruits;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.world.World;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ChorusFruitItem;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule(Rule.EAT_CHORUS_FRUITS)
@Mixin(ChorusFruitItem.class)
public abstract class EatChorusFruitMixin {

    @Inject(method = "finishUsing", at = @At("HEAD"), cancellable = true)
    private void blockChorusTeleport(ItemStack stack, World world, LivingEntity user, CallbackInfoReturnable<ItemStack> cir) {
        if (world.isClient || !(user instanceof ServerPlayerEntity serverPlayer)) return;

        boolean blocked = false;

        for (int i = 0; i < 16; ++i) {
            double d = serverPlayer.getX() + (serverPlayer.getRandom().nextDouble() - 0.5) * 16.0;
            double e = MathHelper.clamp(serverPlayer.getY() + (serverPlayer.getRandom().nextInt(16) - 8),
                    world.getBottomY(), world.getBottomY() + ((ServerWorld)world).getLogicalHeight() - 1);
            double f = serverPlayer.getZ() + (serverPlayer.getRandom().nextDouble() - 0.5) * 16.0;
            BlockPos targetPos = BlockPos.ofFloored(d, e, f);

            if (!RegionPermissionChecker.isAllowed(serverPlayer, Rule.EAT_CHORUS_FRUITS, targetPos)) {
                blocked = true;
                break;
            }
        }

        if (blocked) {
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_EAT_CHORUS_FRUIT, Color.RED);
            cir.setReturnValue(stack);
            cir.cancel();
        }
    }
}