package dev.slarrties.privit.server.region.protection.mixin.use_fire_starters;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.tracking.protection.FireOriginTracker;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule(Rule.USE_FIRE_STARTERS)
@Mixin(BaseFireBlock.class)
public abstract class AbstractFireSpreadProtectionMixin {

    @Inject(
            method = "onPlace",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z")
    )
    private void cleanupOnInvalidPlacement(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean notify, CallbackInfo ci) {
        if (level instanceof ServerLevel serverLevel) {
            this.removeFromTracker(serverLevel, pos);
        }
    }

    @Inject(method = "playerWillDestroy", at = @At("HEAD"))
    private void onBreak(Level level, BlockPos pos, BlockState state, Player player, CallbackInfoReturnable<BlockState> cir) {
        if (level instanceof ServerLevel serverLevel) {
            this.removeFromTracker(serverLevel, pos);
        }
    }

    @Unique
    private void removeFromTracker(ServerLevel level, BlockPos pos) {
        FireOriginTracker fireTracker = WorldRegistry.get(level).getTrackerManager().getFireOriginTracker();
        fireTracker.remove(pos);
    }
}