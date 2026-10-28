package dev.slarrties.privit.server.region.protection.mixin.use_bone_meal;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.tracking.context.BoneMealUseContext;

import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@AssociatedRule(Rule.USE_BONE_MEAL)
@Mixin(BoneMealItem.class)
public abstract class BoneMealItemMixin {

    @Inject(method = "useOn", at = @At("HEAD"))
    private void pushBoneMealContext(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (context.getLevel().isClientSide()) return;
        if (!(context.getPlayer() instanceof ServerPlayer player)) return;
        BoneMealUseContext.push(player.getUUID(), context.getClickedPos());
    }

    @Inject(method = "useOn", at = @At("RETURN"))
    private void popBoneMealContext(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        BoneMealUseContext.pop();
    }

    @Inject(method = "growCrop", at = @At("HEAD"), cancellable = true)
    private static void preventFertilizable(ItemStack stack, Level level, BlockPos pos,
                                            CallbackInfoReturnable<Boolean> cir) {
        denyIfTargetProtected(level, pos, cir);
    }

    @Inject(method = "growWaterPlant", at = @At("HEAD"), cancellable = true)
    private static void preventGround(ItemStack stack, Level level, BlockPos pos, Direction facing,
                                      CallbackInfoReturnable<Boolean> cir) {
        denyIfTargetProtected(level, pos, cir);
    }

    @Unique
    private static void denyIfTargetProtected(Level level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) return;

        BoneMealUseContext ctx = BoneMealUseContext.getCurrent();
        if (ctx == null || ctx.getResponsible() == null) return;

        UUID uuid = ctx.getResponsible();
        if (RegionPermissionChecker.isAllowed(uuid, Rule.USE_BONE_MEAL, pos, serverLevel)) return;
        if (serverLevel.getServer().getPlayerList().getPlayer(uuid) instanceof ServerPlayer player) {
            PlayerNotification.trySend(player, NotificationType.DENY_USE_BONE_MEAL, Color.RED);
        }

        cir.setReturnValue(false);
    }
}