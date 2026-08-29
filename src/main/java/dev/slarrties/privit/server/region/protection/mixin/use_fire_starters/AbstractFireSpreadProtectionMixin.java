package dev.slarrties.privit.server.region.protection.mixin.use_fire_starters;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.tracking.protection.FireOriginTracker;

import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.BlockState;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule(Rule.USE_FIRE_STARTERS)
@Mixin(AbstractFireBlock.class)
public abstract class AbstractFireSpreadProtectionMixin {

    @Inject(
            method = "onBlockAdded",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;removeBlock(Lnet/minecraft/util/math/BlockPos;Z)Z")
    )
    private void cleanupOnInvalidPlacement(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify, CallbackInfo ci) {
        if(world instanceof ServerWorld serverWorld) {
            this.removeFromTracker(serverWorld, pos);
        }
    }

    @Inject(method = "onBreak", at = @At("HEAD"))
    private void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player, CallbackInfoReturnable<BlockState> cir) {
        if(world instanceof ServerWorld serverWorld) {
            this.removeFromTracker(serverWorld, pos);
        }
    }

    @Unique
    private void removeFromTracker(ServerWorld world, BlockPos pos) {
        FireOriginTracker fireTracker = WorldRegistry.get(world).getTrackerManager().getFireOriginTracker();
        fireTracker.remove(pos);
    }
}