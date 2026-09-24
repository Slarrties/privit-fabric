package dev.slarrties.privit.common.network.payload.c2s;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.RegionGroups;
import dev.slarrties.privit.common.network.payload.PrivitPacket;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockBox;
import net.minecraft.network.PacketByteBuf;

import java.util.List;
import java.util.UUID;
import java.util.Optional;
import java.util.ArrayList;

public record RegionGuiUpdateC2SPacket(
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
        Optional<Boolean> isCreated
) implements PrivitPacket {

    public static final Identifier ID = PrivitMod.id("region_gui_update_c2s");

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeUuid(regionId);
        buf.writeBoolean(isChanged);
        buf.writeString(editorName, 32);

        buf.writeBoolean(name.isPresent());
        name.ifPresent(n -> buf.writeString(n, 64));

        buf.writeBoolean(realBounds.isPresent());
        realBounds.ifPresent(b -> writeBox(buf, b));

        buf.writeBoolean(draftBounds.isPresent());
        draftBounds.ifPresent(b -> writeBox(buf, b));

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
    }

    public static RegionGuiUpdateC2SPacket read(PacketByteBuf buf) {
        UUID regionId = buf.readUuid();
        boolean isChanged = buf.readBoolean();
        String editorName = buf.readString(32);

        Optional<String> name = buf.readBoolean() ? Optional.of(buf.readString(64)) : Optional.empty();
        Optional<BlockBox> realBounds = buf.readBoolean() ? Optional.of(readBox(buf)) : Optional.empty();
        Optional<BlockBox> draftBounds = buf.readBoolean() ? Optional.of(readBox(buf)) : Optional.empty();
        Optional<List<BlockBox>> conflictBounds = buf.readBoolean() ? Optional.of(readBoxList(buf)) : Optional.empty();
        Optional<BlockPos> pivotPos = buf.readBoolean() ? Optional.of(buf.readBlockPos()) : Optional.empty();
        Optional<Color> color = buf.readBoolean() ? Optional.of(Color.fromCode(buf.readString(16))) : Optional.empty();
        Optional<RegionGroups> groups = buf.readBoolean() ? Optional.of(RegionGroups.readFromBuf(buf)) : Optional.empty();
        Optional<Boolean> isCreated = buf.readBoolean() ? Optional.of(buf.readBoolean()) : Optional.empty();

        return new RegionGuiUpdateC2SPacket(
                regionId, isChanged, editorName, name, realBounds, draftBounds,
                conflictBounds, pivotPos, color, groups, isCreated
        );
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

    private static void writeBoxList(PacketByteBuf buf, List<BlockBox> list) {
        buf.writeVarInt(list.size());
        for (BlockBox box : list) writeBox(buf, box);
    }

    private static List<BlockBox> readBoxList(PacketByteBuf buf) {
        int size = buf.readVarInt();
        List<BlockBox> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) list.add(readBox(buf));
        return list;
    }

    @Override
    public Identifier getId() {
        return ID;
    }
}