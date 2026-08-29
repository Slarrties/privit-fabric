package dev.slarrties.privit.server.region.protection.mixin.use_spawn_eggs;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.tracking.context.EntitySpawnContext;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule(Rule.USE_SPAWN_EGGS)
@Mixin(SpawnEggItem.class)
public abstract class SpawnEggUseOnBlockMixin {

    @Inject(
            method = "useOnBlock(Lnet/minecraft/item/ItemUsageContext;)Lnet/minecraft/util/ActionResult;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void preventSpawnEggOnProtectedBlock(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        if (!(context.getPlayer() instanceof ServerPlayerEntity serverPlayer)) return;

        BlockPos pos = context.getBlockPos();
        boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer, Rule.USE_SPAWN_EGGS, pos);

        if (!allowed) {
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_USE_SPAWN_EGG, Color.RED);
            cir.setReturnValue(ActionResult.FAIL);
            return;
        }

        EntitySpawnContext.push(serverPlayer, Vec3d.of(pos.offset(context.getSide())));
    }

    @Inject(
            method = "useOnBlock(Lnet/minecraft/item/ItemUsageContext;)Lnet/minecraft/util/ActionResult;",
            at = @At("RETURN")
    )
    private void popSpawnEggContext(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        EntitySpawnContext.pop();
    }
}