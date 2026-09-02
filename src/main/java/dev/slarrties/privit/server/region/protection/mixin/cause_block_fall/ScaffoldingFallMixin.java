package dev.slarrties.privit.server.region.protection.mixin.cause_block_fall;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.tracking.context.BlockFallContext;
import dev.slarrties.privit.server.tracking.protection.BlockFallOriginTracker;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.block.BlockState;
import net.minecraft.block.ScaffoldingBlock;
import net.minecraft.server.world.ServerWorld;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@AssociatedRule(Rule.CAUSE_BLOCK_FALL)
@Mixin(ScaffoldingBlock.class)
public abstract class ScaffoldingFallMixin {

    @Inject(method = "scheduledTick", at = @At("HEAD"))
    private void markScaffoldingFall(BlockState state, ServerWorld serverWorld, BlockPos pos,
                                     Random random, CallbackInfo ci) {
        UUID uuid = resolve(serverWorld, pos);
        if (uuid == null) return;

        BlockFallOriginTracker blockFallTracker = WorldRegistry.get(serverWorld)
                .getTrackerManager()
                .getBlockFallOriginTracker();
        blockFallTracker.record(pos, uuid);
        BlockFallContext.push(uuid, pos);
    }

    @Inject(method = "scheduledTick", at = @At("RETURN"))
    private void popScaffoldingFall(BlockState state, ServerWorld world, BlockPos pos,
                                    Random random, CallbackInfo ci) {
        BlockFallContext.pop();
    }

    @Unique
    private static UUID resolve(ServerWorld serverWorld, BlockPos pos) {
        BlockFallContext ctx = BlockFallContext.getCurrent();
        if (ctx != null && ctx.getResponsible() != null) {
            return ctx.getResponsible();
        }

        return WorldRegistry.get(serverWorld)
                .getTrackerManager()
                .getBlockFallOriginTracker()
                .getResponsible(pos);
    }
}