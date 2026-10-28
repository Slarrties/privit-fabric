package dev.slarrties.privit.server.region.protection.mixin.use_spawn_eggs;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.tracking.context.EntitySpawnContext;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.server.level.ServerPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule(Rule.USE_SPAWN_EGGS)
@Mixin(SpawnEggItem.class)
public abstract class SpawnEggUseOnBlockMixin {

    @Inject(
            method = "useOn(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void preventSpawnEggOnProtectedBlock(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (!(context.getPlayer() instanceof ServerPlayer serverPlayer)) return;

        BlockPos pos = context.getClickedPos();
        boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer.getUUID(), Rule.USE_SPAWN_EGGS, pos, serverPlayer.level());
        if (!allowed) {
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_USE_SPAWN_EGG, Color.RED);
            cir.setReturnValue(InteractionResult.FAIL);
            return;
        }

        EntitySpawnContext.push(serverPlayer, Vec3.atLowerCornerOf(pos.relative(context.getClickedFace())));
    }

    @Inject(
            method = "useOn(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;",
            at = @At("RETURN")
    )
    private void popSpawnEggContext(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        EntitySpawnContext.pop();
    }
}