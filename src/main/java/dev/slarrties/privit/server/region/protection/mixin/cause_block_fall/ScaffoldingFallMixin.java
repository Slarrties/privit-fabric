package dev.slarrties.privit.server.region.protection.mixin.cause_block_fall;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.tracking.context.BlockFallContext;
import dev.slarrties.privit.server.tracking.protection.BlockFallOriginTracker;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.ScaffoldingBlock;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@AssociatedRule(Rule.CAUSE_BLOCK_FALL)
@Mixin(ScaffoldingBlock.class)
public abstract class ScaffoldingFallMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void markScaffoldingFall(BlockState state, ServerLevel level, BlockPos pos,
                                     RandomSource random, CallbackInfo ci) {
        UUID uuid = resolve(level, pos);
        if (uuid == null) return;

        BlockFallOriginTracker blockFallTracker = WorldRegistry.get(level)
                .getTrackerManager()
                .getBlockFallOriginTracker();
        blockFallTracker.record(pos, uuid);
        BlockFallContext.push(uuid, pos);
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void popScaffoldingFall(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        BlockFallContext.pop();
    }

    @Unique
    private static UUID resolve(ServerLevel level, BlockPos pos) {
        BlockFallContext ctx = BlockFallContext.getCurrent();
        if (ctx != null && ctx.getResponsible() != null) return ctx.getResponsible();

        return WorldRegistry.get(level)
                .getTrackerManager()
                .getBlockFallOriginTracker()
                .getResponsible(pos);
    }
}