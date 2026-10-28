package dev.slarrties.privit.server.tracking.nbt;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.server.tracking.protection.TrackerManager;

import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class TrackerNbtPersistence {

    private static final String FILE_NAME = "privit_trackers.dat";
    private final ServerLevel level;
    private final Path filePath;

    public TrackerNbtPersistence(ServerLevel level) {
        this.level = level;
        this.filePath = level.getServer()
                .getWorldPath(LevelResource.ROOT)
                .resolve(PrivitMod.MOD_ID)
                .resolve(level.dimension().identifier().toString().replace(':', '_'))
                .resolve(FILE_NAME);
        createDirectories();
    }

    public void load(TrackerManager trackerManager) {
        if (!Files.exists(filePath)) {
            PrivitMod.LOGGER.info("[NbtTrackerStorage] No tracker save file found for dimension {}", level.dimension());
            return;
        }

        try (var input = Files.newInputStream(filePath)) {
            CompoundTag root = NbtIo.readCompressed(input, NbtAccounter.unlimitedHeap());
            CompoundTag trackersTag = root.getCompoundOrEmpty("trackers");

            if (!trackersTag.isEmpty()) {
                trackerManager.loadFromNbt(trackersTag);
            }
        } catch (IOException e) {
            PrivitMod.LOGGER.error("[NbtTrackerStorage::load] Error loading trackers from NBT", e);
        }
    }

    public void save(TrackerManager trackerManager) {
        CompoundTag root = new CompoundTag();
        CompoundTag trackersData = trackerManager.saveToNbt();

        if (!trackersData.isEmpty()) {
            root.put("trackers", trackersData);
        }

        try {
            Path tempPath = filePath.resolveSibling(FILE_NAME + ".tmp");
            try (var output = Files.newOutputStream(tempPath)) {
                NbtIo.writeCompressed(root, output);
            }
            Files.move(tempPath, filePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            PrivitMod.LOGGER.info("[NbtTrackerStorage] Saved tracker data for dimension {}", level.dimension());
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