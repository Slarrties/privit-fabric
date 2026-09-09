package dev.slarrties.privit.server.region.protection.mixin.use_bone_meal;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.tracking.context.BoneMealUseContext;

import net.minecraft.world.World;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.item.ItemStack;
import net.minecraft.item.BoneMealItem;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@AssociatedRule(Rule.USE_BONE_MEAL)
@Mixin(BoneMealItem.class)
public abstract class BoneMealItemMixin {

    @Inject(method = "useOnBlock", at = @At("HEAD"))
    private void pushBoneMealContext(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        if (context.getWorld().isClient()) return;
        if (!(context.getPlayer() instanceof ServerPlayerEntity player)) return;
        BoneMealUseContext.push(player.getUuid(), context.getBlockPos());
    }

    @Inject(method = "useOnBlock", at = @At("RETURN"))
    private void popBoneMealContext(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        BoneMealUseContext.pop();
    }

    @Inject(method = "useOnFertilizable", at = @At("HEAD"), cancellable = true)
    private static void preventFertilizable(ItemStack stack, World world, BlockPos pos,
                                            CallbackInfoReturnable<Boolean> cir) {
        denyIfTargetProtected(world, pos, cir);
    }

    @Inject(method = "useOnGround", at = @At("HEAD"), cancellable = true)
    private static void preventGround(ItemStack stack, World world, BlockPos pos, Direction facing,
                                      CallbackInfoReturnable<Boolean> cir) {
        denyIfTargetProtected(world, pos, cir);
    }

    @Unique
    private static void denyIfTargetProtected(World world, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (world.isClient() || !(world instanceof ServerWorld serverWorld)) return;

        BoneMealUseContext ctx = BoneMealUseContext.getCurrent();
        if (ctx == null || ctx.getResponsible() == null) return;

        UUID uuid = ctx.getResponsible();
        if (RegionPermissionChecker.isAllowed(uuid, Rule.USE_BONE_MEAL, pos, serverWorld)) return;
        if (serverWorld.getServer().getPlayerManager().getPlayer(uuid) instanceof ServerPlayerEntity player) {
            PlayerNotification.trySend(player, NotificationType.DENY_USE_BONE_MEAL, Color.RED);
        }

        cir.setReturnValue(false);
    }
}