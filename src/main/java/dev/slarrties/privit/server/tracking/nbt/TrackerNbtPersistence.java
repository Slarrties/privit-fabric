package dev.slarrties.privit.server.tracking.nbt;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.server.tracking.protection.TrackerManager;

import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.util.WorldSavePath;
import net.minecraft.server.world.ServerWorld;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class TrackerNbtPersistence {

    private static final String FILE_NAME = "privit_trackers.dat";
    private final ServerWorld world;
    private final Path filePath;

    public TrackerNbtPersistence(ServerWorld world) {
        this.world = world;
        this.filePath = world.getServer()
                .getSavePath(WorldSavePath.ROOT)
                .resolve(PrivitMod.MOD_ID)
                .resolve(world.getRegistryKey().getValue().getPath())
                .resolve(FILE_NAME);
        createDirectories();
    }

    public void load(TrackerManager trackerManager) {
        if (!Files.exists(filePath)) {
            PrivitMod.LOGGER.info("[NbtTrackerStorage] No tracker save file found for dimension {}", world.getRegistryKey().getValue());
            return;
        }

        try (var input = Files.newInputStream(filePath)) {
            NbtCompound root = NbtIo.readCompressed(input, NbtSizeTracker.ofUnlimitedBytes());

            if (root.contains("trackers", net.minecraft.nbt.NbtElement.COMPOUND_TYPE)) {
                trackerManager.loadFromNbt(root.getCompound("trackers"));
//                PrivitMod.LOGGER.info("[NbtTrackerStorage] Loaded tracker data for dimension {}",
//                        world.getRegistryKey().getValue());
            }
        } catch (IOException e) {
            PrivitMod.LOGGER.error("[NbtTrackerStorage::load] Error loading trackers from NBT", e);
        }
    }

    public void save(TrackerManager trackerManager) {
        NbtCompound root = new NbtCompound();
        NbtCompound trackersData = trackerManager.saveToNbt();

        if (!trackersData.isEmpty()) {
            root.put("trackers", trackersData);
        }

        try {
            Path tempPath = filePath.resolveSibling(FILE_NAME + ".tmp");

            try (var output = Files.newOutputStream(tempPath)) {
                NbtIo.writeCompressed(root, output);
            }

            Files.move(tempPath, filePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            PrivitMod.LOGGER.info("[NbtTrackerStorage] Saved tracker data for dimension {}", world.getRegistryKey().getValue());
        } catch (IOException e) {
            PrivitMod.LOGGER.error("[NbtTrackerStorage::save] Error saving trackers to NBT", e);
        }
    }

    private void createDirectories() {
        try {
            Files.createDirectories(filePath.getParent());
        } catch (IOException e) {
            PrivitMod.LOGGER.error("[NbtTrackerStorage::createDirectories] Error creating directories", e);
        }
    }
}