package dev.slarrties.privit.common.network.payload.s2c;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.RegionGroups;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockBox;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

import java.util.List;
import java.util.UUID;
import java.util.Optional;
import java.util.ArrayList;

public record RegionGuiUpdateS2CPacket(
        UUID regionId,
        boolean isChanged,
        String editorName,
        Optional<String> name,
        Optional<BlockBox> realBounds,
        Optional<BlockBox> draftBounds,
        Optional<List<BlockBox>> conflictBounds,
        Optional<BlockPos> pivotPos,
        Optional<Color> color,
        Optional<RegionGroups> groups,
        Optional<Boolean> isCreated,
        Optional<Boolean> isAreaLimitExceeded
) implements CustomPayload {

    public static final Id<RegionGuiUpdateS2CPacket> ID = new Id<>(PrivitMod.id("region_gui_update_s2c"));
    public static final PacketCodec<PacketByteBuf, RegionGuiUpdateS2CPacket> CODEC =
            PacketCodec.of(RegionGuiUpdateS2CPacket::write, RegionGuiUpdateS2CPacket::read);

    private void write(PacketByteBuf buf) {
        buf.writeUuid(regionId);
        buf.writeBoolean(isChanged);
        buf.writeString(editorName, 32);
        buf.writeBoolean(name.isPresent());
        name.ifPresent(n -> buf.writeString(n, 64));

        buf.writeBoolean(realBounds.isPresent());
        realBounds.ifPresent(b -> {
            buf.writeInt(b.getMinX());
            buf.writeInt(b.getMinY());
            buf.writeInt(b.getMinZ());
            buf.writeInt(b.getMaxX());
            buf.writeInt(b.getMaxY());
            buf.writeInt(b.getMaxZ());
        });

        buf.writeBoolean(draftBounds.isPresent());
        draftBounds.ifPresent(b -> {
            buf.writeInt(b.getMinX());
            buf.writeInt(b.getMinY());
            buf.writeInt(b.getMinZ());
            buf.writeInt(b.getMaxX());
            buf.writeInt(b.getMaxY());
            buf.writeInt(b.getMaxZ());
        });

        buf.writeBoolean(conflictBounds.isPresent());
        conflictBounds.ifPresent(list -> writeBoxList(buf, list));

        buf.writeBoolean(pivotPos.isPresent());
        pivotPos.ifPresent(buf::writeBlockPos);

        buf.writeBoolean(color.isPresent());
        color.ifPresent(c -> buf.writeString(c.getCode(), 16));

        buf.writeBoolean(groups.isPresent());
        groups.ifPresent(g -> g.writeToBuf(buf));

        buf.writeBoolean(isCreated.isPresent());
        isCreated.ifPresent(buf::writeBoolean);

        buf.writeBoolean(isAreaLimitExceeded.isPresent());
        isAreaLimitExceeded.ifPresent(buf::writeBoolean);
    }

    private static RegionGuiUpdateS2CPacket read(PacketByteBuf buf) {
        UUID regionId = buf.readUuid();
        boolean isChanged = buf.readBoolean();
        String editorName = buf.readString(32);
        Optional<String> name = buf.readBoolean() ?
                Optional.of(buf.readString(64)) :
                Optional.empty();

        Optional<BlockBox> realBounds = buf.readBoolean()
                ? Optional.of(BlockBox.create(
                new BlockPos(buf.readInt(), buf.readInt(), buf.readInt()),
                new BlockPos(buf.readInt(), buf.readInt(), buf.readInt())))
                : Optional.empty();

        Optional<BlockBox> draftBounds = buf.readBoolean()
                ? Optional.of(BlockBox.create(
                new BlockPos(buf.readInt(), buf.readInt(), buf.readInt()),
                new BlockPos(buf.readInt(), buf.readInt(), buf.readInt())))
                : Optional.empty();

        Optional<List<BlockBox>> conflictBounds = buf.readBoolean()
                ? Optional.of(readBoxList(buf))
                : Optional.empty();

        Optional<BlockPos> pivotPos = buf.readBoolean()
                ? Optional.of(buf.readBlockPos())
                : Optional.empty();

        Optional<Color> color = buf.readBoolean()
                ? Optional.of(Color.fromCode(buf.readString(16)))
                : Optional.empty();

        Optional<RegionGroups> groups = buf.readBoolean()
                ? Optional.of(RegionGroups.readFromBuf(buf))
                : Optional.empty();

        Optional<Boolean> isCreated = buf.readBoolean()
                ? Optional.of(buf.readBoolean())
                : Optional.empty();

        Optional<Boolean> isAreaLimitExceeded = buf.readBoolean()
                ? Optional.of(buf.readBoolean())
                : Optional.empty();

        return new RegionGuiUpdateS2CPacket(
                regionId, isChanged, editorName, name, realBounds, draftBounds, conflictBounds, pivotPos, color, groups, isCreated, isAreaLimitExceeded
        );
    }

    private static void writeBox(PacketByteBuf buf, BlockBox box) {
        buf.writeInt(box.getMinX());
        buf.writeInt(box.getMinY());
        buf.writeInt(box.getMinZ());
        buf.writeInt(box.getMaxX());
        buf.writeInt(box.getMaxY());
        buf.writeInt(box.getMaxZ());
    }

    private static Optional<BlockBox> readOptionalBox(PacketByteBuf buf) {
        return buf.readBoolean()
                ? Optional.of(BlockBox.create(
                        new BlockPos(buf.readInt(), buf.readInt(), buf.readInt()),
                        new BlockPos(buf.readInt(), buf.readInt(), buf.readInt())
                    ))
                : Optional.empty();
    }

    private static void writeBoxList(PacketByteBuf buf, List<BlockBox> list) {
        buf.writeVarInt(list.size());
        for (BlockBox box : list) {
            writeBox(buf, box);
        }
    }

    private static List<BlockBox> readBoxList(PacketByteBuf buf) {
        int size = buf.readVarInt();
        List<BlockBox> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(BlockBox.create(
                    new BlockPos(buf.readInt(), buf.readInt(), buf.readInt()),
                    new BlockPos(buf.readInt(), buf.readInt(), buf.readInt())
            ));
        }
        return list;
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}