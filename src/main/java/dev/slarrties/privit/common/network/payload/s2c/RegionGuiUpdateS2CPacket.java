package dev.slarrties.privit.common.network.payload.s2c;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.RegionGroups;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;
import java.util.UUID;
import java.util.Optional;
import java.util.ArrayList;

public record RegionGuiUpdateS2CPacket(
        UUID regionId,
        boolean isChanged,
        String editorName,
        Optional<String> name,
        Optional<BoundingBox> realBounds,
        Optional<BoundingBox> draftBounds,
        Optional<List<BoundingBox>> conflictBounds,
        Optional<BlockPos> pivotPos,
        Optional<Color> color,
        Optional<RegionGroups> groups,
        Optional<Boolean> isCreated,
        Optional<Boolean> isAreaLimitExceeded
) implements CustomPacketPayload {

    public static final Type<RegionGuiUpdateS2CPacket> TYPE =
            new Type<>(PrivitMod.id("region_gui_update_s2c"));

    public static final StreamCodec<FriendlyByteBuf, RegionGuiUpdateS2CPacket> CODEC =
            StreamCodec.of(
                    (buf, packet) -> packet.write(buf),
                    RegionGuiUpdateS2CPacket::read
            );

    private void write(FriendlyByteBuf buf) {
        buf.writeUUID(regionId);
        buf.writeBoolean(isChanged);
        buf.writeUtf(editorName, 32);

        buf.writeBoolean(name.isPresent());
        name.ifPresent(n -> buf.writeUtf(n, 64));

        buf.writeBoolean(realBounds.isPresent());
        realBounds.ifPresent(b -> {
            buf.writeInt(b.minX());
            buf.writeInt(b.minY());
            buf.writeInt(b.minZ());
            buf.writeInt(b.maxX());
            buf.writeInt(b.maxY());
            buf.writeInt(b.maxZ());
        });

        buf.writeBoolean(draftBounds.isPresent());
        draftBounds.ifPresent(b -> {
            buf.writeInt(b.minX());
            buf.writeInt(b.minY());
            buf.writeInt(b.minZ());
            buf.writeInt(b.maxX());
            buf.writeInt(b.maxY());
            buf.writeInt(b.maxZ());
        });

        buf.writeBoolean(conflictBounds.isPresent());
        conflictBounds.ifPresent(list -> writeBoxList(buf, list));

        buf.writeBoolean(pivotPos.isPresent());
        pivotPos.ifPresent(buf::writeBlockPos);

        buf.writeBoolean(color.isPresent());
        color.ifPresent(c -> buf.writeUtf(c.getCode(), 16));

        buf.writeBoolean(groups.isPresent());
        groups.ifPresent(g -> g.writeToBuf(buf));

        buf.writeBoolean(isCreated.isPresent());
        isCreated.ifPresent(buf::writeBoolean);

        buf.writeBoolean(isAreaLimitExceeded.isPresent());
        isAreaLimitExceeded.ifPresent(buf::writeBoolean);
    }

    private static RegionGuiUpdateS2CPacket read(FriendlyByteBuf buf) {
        UUID regionId = buf.readUUID();
        boolean isChanged = buf.readBoolean();
        String editorName = buf.readUtf(32);

        Optional<String> name = buf.readBoolean()
                ? Optional.of(buf.readUtf(64))
                : Optional.empty();

        Optional<BoundingBox> realBounds = buf.readBoolean()
                ? Optional.of(readBox(buf))
                : Optional.empty();

        Optional<BoundingBox> draftBounds = buf.readBoolean()
                ? Optional.of(readBox(buf))
                : Optional.empty();

        Optional<List<BoundingBox>> conflictBounds = buf.readBoolean()
                ? Optional.of(readBoxList(buf))
                : Optional.empty();

        Optional<BlockPos> pivotPos = buf.readBoolean()
                ? Optional.of(buf.readBlockPos())
                : Optional.empty();

        Optional<Color> color = buf.readBoolean()
                ? Optional.of(Color.fromCode(buf.readUtf(16)))
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
                regionId, isChanged, editorName, name, realBounds, draftBounds,
                conflictBounds, pivotPos, color, groups, isCreated, isAreaLimitExceeded
        );
    }

    private static BoundingBox readBox(FriendlyByteBuf buf) {
        return new BoundingBox(
                buf.readInt(), buf.readInt(), buf.readInt(),
                buf.readInt(), buf.readInt(), buf.readInt()
        );
    }

    private static void writeBoxList(FriendlyByteBuf buf, List<BoundingBox> list) {
        buf.writeVarInt(list.size());
        for (BoundingBox box : list) {
            buf.writeInt(box.minX());
            buf.writeInt(box.minY());
            buf.writeInt(box.minZ());
            buf.writeInt(box.maxX());
            buf.writeInt(box.maxY());
            buf.writeInt(box.maxZ());
        }
    }

    private static List<BoundingBox> readBoxList(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<BoundingBox> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(readBox(buf));
        }
        return list;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}