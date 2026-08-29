package dev.slarrties.privit.server.region.protection.mixin.use_sponges;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.tracking.protection.FluidOriginTracker;
import dev.slarrties.privit.server.tracking.context.SpongePlacementContext;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.world.World;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.atomic.AtomicBoolean;

/** TODO: a completely rewritten method is bad
 /** but I don't know how to solve the problem otherwise yet
 */

@AssociatedRule({Rule.USE_SPONGES, Rule.USE_FLUIDS})
@Mixin(SpongeBlock.class)
public abstract class SpongeAbsorbMixin {

    @Unique
    private static final Direction[] DIRECTIONS = Direction.values();
    @Unique
    private static final int ABSORB_RADIUS = 6;
    @Unique
    private static final int ABSORB_LIMIT = 65;

    @Inject(method = "absorbWater", at = @At("HEAD"), cancellable = true)
    private void onSpongeAbsorb(World world, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (world.isClient) {
            cir.setReturnValue(false);
            cir.cancel();
            return;
        }

        SpongePlacementContext context = SpongePlacementContext.getCurrent();
        if (context == null || !context.getPos().equals(pos)) {
            cir.setReturnValue(false);
            cir.cancel();
            return;
        }

        ServerPlayerEntity player = context.getPlayer();
        SpongePlacementContext.pop();

        boolean absorbed = performSelectiveAbsorb(world, pos, player);
        cir.setReturnValue(absorbed);
        cir.cancel();
    }

    @Unique
    private boolean performSelectiveAbsorb(World world, BlockPos spongePos, ServerPlayerEntity serverPlayer) {
        AtomicBoolean absorbedAnything = new AtomicBoolean(false);
        AtomicBoolean triedProtected = new AtomicBoolean(false);

        int absorbedCount = BlockPos.iterateRecursively(
                spongePos,
                ABSORB_RADIUS,
                ABSORB_LIMIT,
                (currentPos, queuer) -> {for (Direction direction : DIRECTIONS) queuer.accept(currentPos.offset(direction));},
                (currentPos) -> {
                    if (currentPos.equals(spongePos)) return true;

                    BlockState blockState = world.getBlockState(currentPos);
                    FluidState fluidState = world.getFluidState(currentPos);

                    if (!fluidState.isIn(FluidTags.WATER)) return false;

                    boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer, Rule.USE_SPONGES, currentPos);

                    if (!allowed) {
                        triedProtected.set(true);
                        return false;
                    }
                    if (!(world instanceof ServerWorld serverWorld)) return false; // TODO
                    FluidOriginTracker fluidOriginTracker = WorldRegistry.get(serverWorld)
                            .getTrackerManager()
                            .getFluidOriginTracker();

                    Block block = blockState.getBlock();

                    if (block instanceof FluidDrainable fluidDrainable) {
                        if (!fluidDrainable.tryDrainFluid((PlayerEntity) null, world, currentPos, blockState).isEmpty()) {
                            absorbedAnything.set(true);
                            fluidOriginTracker.remove(currentPos);
                            return true;
                        }
                    }

                    if (blockState.getBlock() instanceof FluidBlock) {
                        world.setBlockState(currentPos, Blocks.AIR.getDefaultState(), 3);
                        absorbedAnything.set(true);
                        fluidOriginTracker.remove(currentPos);
                        return true;
                    }

                    if (blockState.isOf(Blocks.KELP) || blockState.isOf(Blocks.KELP_PLANT) ||
                            blockState.isOf(Blocks.SEAGRASS) || blockState.isOf(Blocks.TALL_SEAGRASS)) {

                        BlockEntity blockEntity = blockState.hasBlockEntity() ? world.getBlockEntity(currentPos) : null;
                        Block.dropStacks(blockState, world, currentPos, blockEntity);
                        world.setBlockState(currentPos, Blocks.AIR.getDefaultState(), 3);
                        absorbedAnything.set(true);
                        fluidOriginTracker.remove(currentPos);
                        return true;
                    }

                    return false;
                }
        );

        if (triedProtected.get()) {
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_USE_SPONGE, Color.RED);
        }

        return absorbedCount > 0 || absorbedAnything.get();
    }
}