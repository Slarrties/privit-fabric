package dev.slarrties.privit.server.region.nbt;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.RegionGroups;
import dev.slarrties.privit.server.region.Region;
import dev.slarrties.privit.server.region.RegionManager;

import net.minecraft.nbt.*;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.WorldSavePath;
import net.minecraft.server.world.ServerWorld;

import java.util.UUID;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public final class RegionNbtPersistence {

    private static final String FILE_NAME = "privit_regions.dat";
    private final ServerWorld world;
    private final Path filePath;

    public RegionNbtPersistence(ServerWorld world) {
        this.world = world;
        this.filePath = world.getServer()
                .getSavePath(WorldSavePath.ROOT)
                .resolve(PrivitMod.MOD_ID)
                .resolve(world.getRegistryKey().getValue().getPath())
                .resolve(FILE_NAME);
        createDirectories();
    }

    public void load(RegionManager manager) {
        if (!Files.exists(filePath)) {
            PrivitMod.LOGGER.info("[NbtRegionStorage] No regions save file found for dimension {}",
                    world.getRegistryKey().getValue());
            return;
        }

        try (var input = Files.newInputStream(filePath)) {
            NbtCompound root = NbtIo.readCompressed(input, NbtSizeTracker.ofUnlimitedBytes());
            NbtList list = root.getList("regions", NbtElement.COMPOUND_TYPE);

            int loaded = 0;
            for (int i = 0; i < list.size(); i++) {
                NbtCompound tag = list.getCompound(i);
                Region region = fromNbt(tag);

                if (region != null) {
                    manager.loadRegion(region);
                    loaded++;
                }
            }

            PrivitMod.LOGGER.info("[NbtRegionStorage] Loaded {} regions in dimension {}",
                    loaded, world.getRegistryKey().getValue());

        } catch (IOException e) {
            PrivitMod.LOGGER.error("[NbtRegionStorage::load] Error loading regions from NBT", e);
        }
    }

    public void save(RegionManager manager) {
        NbtList list = new NbtList();
        manager.getAll().forEach(region -> list.add(toNbt(region)));

        NbtCompound root = new NbtCompound();
        root.put("regions", list);

        try {
            Path tempPath = filePath.resolveSibling(FILE_NAME + ".tmp");

            try (var output = Files.newOutputStream(tempPath)) {
                NbtIo.writeCompressed(root, output);
            }

            Files.move(tempPath, filePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            PrivitMod.LOGGER.info("[NbtRegionStorage] Saved {} regions for dimension {}",
                    list.size(), world.getRegistryKey().getValue());

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

    private static NbtCompound toNbt(Region region) {
        NbtCompound tag = new NbtCompound();

        tag.putUuid("id", region.id());
        tag.putString("name", region.name());
        tag.putString("colorCode", region.color().getCode());

        NbtCompound bounds = new NbtCompound();
        bounds.putInt("minX", region.bounds().getMinX());
        bounds.putInt("minY", region.bounds().getMinY());
        bounds.putInt("minZ", region.bounds().getMinZ());
        bounds.putInt("maxX", region.bounds().getMaxX());
        bounds.putInt("maxY", region.bounds().getMaxY());
        bounds.putInt("maxZ", region.bounds().getMaxZ());
        tag.put("bounds", bounds);

        tag.putLong("pivotPos", region.pivotPos().asLong());
        tag.put("groups", region.groups().toNbt());

        return tag;
    }

    private static Region fromNbt(NbtCompound tag) {
        try {
            UUID id = tag.getUuid("id");
            String name = tag.getString("name");

            String colorCodeStr = tag.contains("colorCode", NbtElement.STRING_TYPE)
                    ? tag.getString("colorCode")
                    : "§f";
            Color color = Color.fromCode(colorCodeStr);

            NbtCompound boundsTag = tag.getCompound("bounds");
            BlockBox bounds = new BlockBox(
                    boundsTag.getInt("minX"), boundsTag.getInt("minY"), boundsTag.getInt("minZ"),
                    boundsTag.getInt("maxX"), boundsTag.getInt("maxY"), boundsTag.getInt("maxZ")
            );

            BlockPos pivotPos = BlockPos.fromLong(tag.getLong("pivotPos"));
            NbtCompound groupsTag = tag.getCompound("groups");
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