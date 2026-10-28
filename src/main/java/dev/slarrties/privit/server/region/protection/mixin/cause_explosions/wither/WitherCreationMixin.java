package dev.slarrties.privit.server.region.protection.mixin.cause_explosions.wither;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.tracking.protection.WitherPlacerTracker;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.WitherSkullBlock;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.server.level.ServerPlayer;

import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@AssociatedRule(Rule.CAUSE_EXPLOSIONS)
@Mixin(WitherSkullBlock.class)
public abstract class WitherCreationMixin {

    @Inject(method = "setPlacedBy", at = @At("HEAD"))
    private void recordWitherPlacer(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack, CallbackInfo ci) {
        if (level.isClientSide() || !(placer instanceof ServerPlayer serverPlayer)) return;
        WitherPlacerTracker.recordPlacer(pos, serverPlayer);
    }

    @Inject(
            method = "checkSpawn(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/SkullBlockEntity;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"
            ),
            locals = LocalCapture.CAPTURE_FAILSOFT
    )
    private static void linkPlacerToWither(Level level, BlockPos pos, SkullBlockEntity blockEntity, CallbackInfo ci, @Local(name = "witherBoss") WitherBoss wither) {
        if (level.isClientSide() || wither == null) return;
        WitherPlacerTracker.applyToWither(wither);
    }
}