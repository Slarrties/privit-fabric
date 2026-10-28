package dev.slarrties.privit.server.region.nbt;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.RegionGroups;
import dev.slarrties.privit.server.region.Region;
import dev.slarrties.privit.server.region.RegionManager;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.UUID;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public final class RegionNbtPersistence {

    private static final String FILE_NAME = "privit_regions.dat";
    private final ServerLevel level;
    private final Path filePath;

    public RegionNbtPersistence(ServerLevel level) {
        this.level = level;
        this.filePath = level.getServer()
                .getWorldPath(LevelResource.ROOT)
                .resolve(PrivitMod.MOD_ID)
                .resolve(level.dimension().identifier().toString().replace(':', '_'))
                .resolve(FILE_NAME);
        createDirectories();
    }

    public void load(RegionManager manager) {
        if (!Files.exists(filePath)) {
            PrivitMod.LOGGER.info("[NbtRegionStorage] No regions save file found for dimension {}", level.dimension());
            return;
        }

        try (var input = Files.newInputStream(filePath)) {
            CompoundTag root = NbtIo.readCompressed(input, NbtAccounter.unlimitedHeap());
            ListTag list = root.getListOrEmpty("regions");
            int loaded = 0;

            for (int i = 0; i < list.size(); i++) {
                CompoundTag tag = list.getCompound(i).orElseThrow();
                Region region = fromNbt(tag);

                if (region != null) {
                    manager.loadRegion(region);
                    loaded++;
                }
            }

            PrivitMod.LOGGER.info("[NbtRegionStorage] Loaded {} regions in dimension {}", loaded, level.dimension());
        } catch (IOException e) {
            PrivitMod.LOGGER.error("[NbtRegionStorage::load] Error loading regions from NBT", e);
        }
    }

    public void save(RegionManager manager) {
        ListTag list = new ListTag();
        manager.getAll().forEach(region -> list.add(toNbt(region)));
        CompoundTag root = new CompoundTag();

        root.put("regions", list);

        try {
            Path tempPath = filePath.resolveSibling(FILE_NAME + ".tmp");

            try (var output = Files.newOutputStream(tempPath)) {
                NbtIo.writeCompressed(root, output);
            }

            Files.move(tempPath, filePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            PrivitMod.LOGGER.info("[NbtRegionStorage] Saved {} regions for dimension {}", list.size(), level.dimension());
        } catch (IOException e) {
            PrivitMod.LOGGER.error("[NbtRegionStorage] Error saving regions to NBT", e);
        }
    }

    private void createDirectories() {
        try {
            Files.createDirectories(filePath.getParent());
        } catch (IOException e) {
            PrivitMod.LOGGER.error("[NbtRegionStorage] Error creating directories", e);
        }
    }

    private static CompoundTag toNbt(Region region) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", region.id().toString());
        tag.putString("name", region.name());
        tag.putString("colorCode", region.color().getCode());

        CompoundTag bounds = new CompoundTag();
        bounds.putInt("minX", region.bounds().minX());
        bounds.putInt("minY", region.bounds().minY());
        bounds.putInt("minZ", region.bounds().minZ());
        bounds.putInt("maxX", region.bounds().maxX());
        bounds.putInt("maxY", region.bounds().maxY());
        bounds.putInt("maxZ", region.bounds().maxZ());
        tag.put("bounds", bounds);

        tag.putLong("pivotPos", region.pivotPos().asLong());
        tag.put("groups", region.groups().toNbt());
        return tag;
    }

    private static Region fromNbt(CompoundTag tag) {
        try {
            UUID id = UUID.fromString(tag.getStringOr("id", ""));
            String name = tag.getStringOr("name", "");
            String colorCodeStr = tag.getStringOr("colorCode", "§f");
            Color color = Color.fromCode(colorCodeStr);
            CompoundTag boundsTag = tag.getCompoundOrEmpty("bounds");
            BoundingBox bounds = new BoundingBox(
                    boundsTag.getIntOr("minX", 0), boundsTag.getIntOr("minY", 0), boundsTag.getIntOr("minZ", 0),
                    boundsTag.getIntOr("maxX", 0), boundsTag.getIntOr("maxY", 0), boundsTag.getIntOr("maxZ", 0)
            );
            BlockPos pivotPos = BlockPos.of(tag.getLongOr("pivotPos", 0L));
            CompoundTag groupsTag = tag.getCompoundOrEmpty("groups");
            RegionGroups groups = RegionGroups.fromNbt(groupsTag);

            return new Region.Builder(pivotPos, name, id)
                    .id(id)
                    .name(name)
                    .bounds(bounds)
                    .pivotPos(pivotPos)
                    .color(color)
                    .groups(groups)
                    .build();
        } catch (Exception e) {
            PrivitMod.LOGGER.warn("[NbtRegionStorage] Region NBT data is corrupted — skipping", e);
            return null;
        }
    }
}