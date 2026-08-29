package dev.slarrties.privit.server.tracking.redstone;

import dev.slarrties.privit.PrivitMod;

import net.minecraft.block.*;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.ArrayList;

public final class RedstoneSourceRegistry {

    private static final List<RedstoneSource> SOURCES = new ArrayList<>();

    static { registerDefaultSources(); }

    private static void registerDefaultSources() {
        register((state, world, pos) -> state.getBlock() instanceof RedstoneBlock);
        register((state, world, pos) -> state.getBlock() instanceof RedstoneTorchBlock);

        register((state, world, pos) -> state.getBlock() instanceof DaylightDetectorBlock);
        register((state, world, pos) -> state.getBlock() instanceof TargetBlock);
        register((state, world, pos) -> state.getBlock() instanceof LeverBlock);
        register((state, world, pos) -> state.getBlock() instanceof ButtonBlock);
        register((state, world, pos) -> state.getBlock() instanceof PressurePlateBlock);
        register((state, world, pos) -> state.getBlock() instanceof WeightedPressurePlateBlock);

        register((state, world, pos) -> state.getBlock() instanceof SculkSensorBlock);
        register((state, world, pos) -> state.getBlock() instanceof CalibratedSculkSensorBlock);
        register((state, world, pos) -> state.getBlock() instanceof LightningRodBlock);
        register((state, world, pos) -> state.getBlock() instanceof JukeboxBlock);

        register((state, world, pos) -> state.getBlock() instanceof RepeaterBlock);
        register((state, world, pos) -> state.getBlock() instanceof ComparatorBlock);
        register((state, world, pos) -> state.getBlock() instanceof ObserverBlock);
        register((state, world, pos) -> state.getBlock() instanceof LecternBlock);

        register((state, world, pos) -> state.getBlock() instanceof RedstoneOreBlock);

        register((state, world, pos) -> state.getBlock() instanceof ComposterBlock);
        register((state, world, pos) -> state.getBlock() instanceof DecoratedPotBlock);
        register((state, world, pos) -> state.getBlock() instanceof ChiseledBookshelfBlock);
        register((state, world, pos) -> state.getBlock() instanceof AbstractCauldronBlock);
        register((state, world, pos) -> state.getBlock() instanceof ChestBlock);
        register((state, world, pos) -> state.getBlock() instanceof TrappedChestBlock);
        register((state, world, pos) -> state.getBlock() instanceof BarrelBlock);
        register((state, world, pos) -> state.getBlock() instanceof FurnaceBlock);
        register((state, world, pos) -> state.getBlock() instanceof BlastFurnaceBlock);
    }

    public static void register(RedstoneSource source) {
        SOURCES.add(source);
    }

    public static boolean isSource(BlockState state, World world, BlockPos pos) {
        if (state == null || world == null || pos == null) {
            PrivitMod.LOGGER.error("[RedstoneSourceRegistry::isSource] Null state/world or pos");
            return false;
        }

        for (RedstoneSource source : SOURCES) {
            if (source.isSource(state, world, pos)) {
                return true;
            }
        }

        return false;
    }

    public static List<RedstoneSource> getAllSources() { return List.copyOf(SOURCES); }
}