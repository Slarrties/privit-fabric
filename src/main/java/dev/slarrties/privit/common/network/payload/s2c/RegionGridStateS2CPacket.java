package dev.slarrties.privit.common.network.payload.s2c;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.network.payload.PrivitPacket;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.network.PacketByteBuf;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.Optional;
import java.util.ArrayList;

public record RegionGridStateS2CPacket(
        UUID regionId,
        boolean enabled,
        Optional<Color> color,
        Optional<BlockBox> realBounds,
        Optional<BlockBox> draftBounds,
        List<BlockBox> conflictBounds
) implements PrivitPacket {

    public static final Identifier ID = PrivitMod.id("region_grid_state_s2c");

    public static RegionGridStateS2CPacket show(
            UUID regionId, Color color, @Nullable BlockBox realBounds,
            BlockBox draftBounds, List<BlockBox> conflictBounds
    ) {
        return new RegionGridStateS2CPacket(
                regionId, true, Optional.of(color), Optional.ofNullable(realBounds),
                Optional.of(draftBounds), conflictBounds == null ? List.of() : List.copyOf(conflictBounds)
        );
    }

    public static RegionGridStateS2CPacket hide(UUID regionId) {
        return new RegionGridStateS2CPacket(regionId, false, Optional.empty(), Optional.empty(), Optional.empty(), List.of());
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeUuid(regionId);
        buf.writeBoolean(enabled);
        if (!enabled) return;

        buf.writeString(color.orElse(Color.getDefault()).getCode(), 16);
        writeOptionalBox(buf, realBounds);
        writeBox(buf, draftBounds.orElseThrow());

        buf.writeVarInt(conflictBounds.size());
        for (BlockBox box : conflictBounds) {
            writeBox(buf, box);
        }
    }

    public static RegionGridStateS2CPacket read(PacketByteBuf buf) {
        UUID regionId = buf.readUuid();
        boolean enabled = buf.readBoolean();
        if (!enabled) return hide(regionId);

        Color color = Color.fromCode(buf.readString(16));
        Optional<BlockBox> realBounds = readOptionalBox(buf);
        BlockBox draftBounds = readBox(buf);

        int conflictCount = buf.readVarInt();
        List<BlockBox> conflicts = new ArrayList<>(conflictCount);

        for (int i = 0; i < conflictCount; i++)
            conflicts.add(readBox(buf));

        return new RegionGridStateS2CPacket(
                regionId, true, Optional.of(color), realBounds, Optional.of(draftBounds), List.copyOf(conflicts)
        );
    }

    private static void writeOptionalBox(PacketByteBuf buf, Optional<BlockBox> box) {
        buf.writeBoolean(box.isPresent());
        box.ifPresent(value -> writeBox(buf, value));
    }

    private static Optional<BlockBox> readOptionalBox(PacketByteBuf buf) {
        if (!buf.readBoolean()) return Optional.empty();
        return Optional.of(readBox(buf));
    }

    private static void writeBox(PacketByteBuf buf, BlockBox box) {
        buf.writeInt(box.getMinX()); buf.writeInt(box.getMinY()); buf.writeInt(box.getMinZ());
        buf.writeInt(box.getMaxX()); buf.writeInt(box.getMaxY()); buf.writeInt(box.getMaxZ());
    }

    private static BlockBox readBox(PacketByteBuf buf) {
        return BlockBox.create(
                new BlockPos(buf.readInt(), buf.readInt(), buf.readInt()),
                new BlockPos(buf.readInt(), buf.readInt(), buf.readInt())
        );
    }

    @Override
    public Identifier getId() {
        return ID;
    }
}