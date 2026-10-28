package dev.slarrties.privit.server.tracking.redstone;

import dev.slarrties.privit.PrivitMod;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.ArrayList;

public final class RedstoneSourceRegistry {

    private static final List<RedstoneSource> SOURCES = new ArrayList<>();

    static { registerDefaultSources(); } // TODO: move to PrivitServer?

    private static void registerDefaultSources() {
//        register((state, level, pos) -> state.getBlock() instanceof RedstoneBlock); // TODO
        register((state, level, pos) -> state.getBlock() instanceof RedstoneTorchBlock);
        register((state, level, pos) -> state.getBlock() instanceof DaylightDetectorBlock);
        register((state, level, pos) -> state.getBlock() instanceof TargetBlock);
        register((state, level, pos) -> state.getBlock() instanceof LeverBlock);
        register((state, level, pos) -> state.getBlock() instanceof ButtonBlock);
        register((state, level, pos) -> state.getBlock() instanceof PressurePlateBlock);
        register((state, level, pos) -> state.getBlock() instanceof WeightedPressurePlateBlock);
        register((state, level, pos) -> state.getBlock() instanceof SculkSensorBlock);
        register((state, level, pos) -> state.getBlock() instanceof CalibratedSculkSensorBlock);
        register((state, level, pos) -> state.getBlock() instanceof LightningRodBlock);
        register((state, level, pos) -> state.getBlock() instanceof JukeboxBlock);
        register((state, level, pos) -> state.getBlock() instanceof RepeaterBlock);
        register((state, level, pos) -> state.getBlock() instanceof ComparatorBlock);
        register((state, level, pos) -> state.getBlock() instanceof ObserverBlock);
        register((state, level, pos) -> state.getBlock() instanceof LecternBlock);
        register((state, level, pos) -> state.getBlock() instanceof RedStoneOreBlock);
        register((state, level, pos) -> state.getBlock() instanceof ComposterBlock);
        register((state, level, pos) -> state.getBlock() instanceof DecoratedPotBlock);
//        register((state, level, pos) -> state.getBlock() instanceof ChiseledBookshelfBlock); // TODO
        register((state, level, pos) -> state.getBlock() instanceof AbstractCauldronBlock);
        register((state, level, pos) -> state.getBlock() instanceof ChestBlock);
        register((state, level, pos) -> state.getBlock() instanceof TrappedChestBlock);
        register((state, level, pos) -> state.getBlock() instanceof BarrelBlock);
        register((state, level, pos) -> state.getBlock() instanceof FurnaceBlock);
        register((state, level, pos) -> state.getBlock() instanceof BlastFurnaceBlock);
    }

    public static void register(RedstoneSource source) {
        SOURCES.add(source);
    }

    public static boolean isSource(BlockState state, Level level, BlockPos pos) {
        if (state == null || level == null || pos == null) {
            PrivitMod.LOGGER.error("[RedstoneSourceRegistry::isSource] Null state/level or pos");
            return false;
        }

        for (RedstoneSource source : SOURCES) {
            if (source.isSource(state, level, pos)) {
                return true;
            }
        }
        return false;
    }

    public static List<RedstoneSource> getAllSources() {
        return List.copyOf(SOURCES);
    }
}