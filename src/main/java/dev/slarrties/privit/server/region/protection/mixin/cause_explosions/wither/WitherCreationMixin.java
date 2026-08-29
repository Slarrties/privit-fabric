package dev.slarrties.privit.server.region.protection.mixin.cause_explosions.wither;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.tracking.protection.WitherPlacerTracker;

import net.minecraft.world.World;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.BlockState;
import net.minecraft.block.WitherSkullBlock;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.server.network.ServerPlayerEntity;

import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@AssociatedRule(Rule.CAUSE_EXPLOSIONS)
@Mixin(WitherSkullBlock.class)
public abstract class WitherCreationMixin {

    @Inject(
            method = "onPlaced(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;)V",
            at = @At("HEAD")
    )
    private void recordWitherPlacer(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack, CallbackInfo ci) {
        if (world.isClient || !(placer instanceof ServerPlayerEntity serverPlayer)) return;

        WitherPlacerTracker.recordPlacer(pos, serverPlayer);
    }

    @Inject(
            method = "onPlaced(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/entity/SkullBlockEntity;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;spawnEntity(Lnet/minecraft/entity/Entity;)Z"
            ),
            locals = LocalCapture.CAPTURE_FAILSOFT
    )
    private static void linkPlacerToWither(World world, BlockPos pos, SkullBlockEntity blockEntity, CallbackInfo ci, @Local WitherEntity witherEntity) {
        if (world.isClient || witherEntity == null) return;

        WitherPlacerTracker.applyToWither(witherEntity);
    }
}