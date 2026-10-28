package dev.slarrties.privit.common.network.payload.s2c;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.common.region.Color;

import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.Optional;
import java.util.ArrayList;

public record RegionGridStateS2CPacket(
        UUID regionId,
        boolean enabled,
        Optional<Color> color,
        Optional<BoundingBox> realBounds,
        Optional<BoundingBox> draftBounds,
        List<BoundingBox> conflictBounds
) implements CustomPacketPayload {

    public static final Type<RegionGridStateS2CPacket> TYPE = new Type<>(PrivitMod.id("region_grid_state_s2c"));

    public static final StreamCodec<FriendlyByteBuf, RegionGridStateS2CPacket> CODEC = StreamCodec.of(
            (FriendlyByteBuf buf, RegionGridStateS2CPacket packet) -> packet.write(buf),
            RegionGridStateS2CPacket::read
    );

    public static RegionGridStateS2CPacket show(
            UUID regionId,
            Color color,
            @Nullable BoundingBox realBounds,
            BoundingBox draftBounds,
            List<BoundingBox> conflictBounds
    ) {
        return new RegionGridStateS2CPacket(
                regionId,
                true,
                Optional.of(color),
                Optional.ofNullable(realBounds),
                Optional.of(draftBounds),
                conflictBounds == null ? List.of() : List.copyOf(conflictBounds)
        );
    }

    public static RegionGridStateS2CPacket hide(UUID regionId) {
        return new RegionGridStateS2CPacket(
                regionId,
                false,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                List.of()
        );
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeUUID(regionId);
        buf.writeBoolean(enabled);
        if (!enabled) return;

        buf.writeUtf(color.orElse(Color.getDefault()).getCode(), 16);
        writeOptionalBox(buf, realBounds);
        writeBox(buf, draftBounds.orElseThrow());
        buf.writeVarInt(conflictBounds.size());
        for (BoundingBox box : conflictBounds) {
            writeBox(buf, box);
        }
    }

    private static RegionGridStateS2CPacket read(FriendlyByteBuf buf) {
        UUID regionId = buf.readUUID();
        boolean enabled = buf.readBoolean();

        if (!enabled) {
            return hide(regionId);
        }

        Color color = Color.fromCode(buf.readUtf(16));
        Optional<BoundingBox> realBounds = readOptionalBox(buf);
        BoundingBox draftBounds = readBox(buf);
        int conflictCount = buf.readVarInt();
        List<BoundingBox> conflicts = new ArrayList<>(conflictCount);
        for (int i = 0; i < conflictCount; i++) {
            conflicts.add(readBox(buf));
        }

        return new RegionGridStateS2CPacket(
                regionId,
                true,
                Optional.of(color),
                realBounds,
                Optional.of(draftBounds),
                List.copyOf(conflicts)
        );
    }

    private static void writeOptionalBox(FriendlyByteBuf buf, Optional<BoundingBox> box) {
        buf.writeBoolean(box.isPresent());
        box.ifPresent((BoundingBox value) -> writeBox(buf, value));
    }

    private static Optional<BoundingBox> readOptionalBox(FriendlyByteBuf buf) {
        if (!buf.readBoolean()) return Optional.empty();
        return Optional.of(readBox(buf));
    }

    private static void writeBox(FriendlyByteBuf buf, BoundingBox box) {
        buf.writeInt(box.minX());
        buf.writeInt(box.minY());
        buf.writeInt(box.minZ());
        buf.writeInt(box.maxX());
        buf.writeInt(box.maxY());
        buf.writeInt(box.maxZ());
    }

    private static BoundingBox readBox(FriendlyByteBuf buf) {
        return new BoundingBox(
                buf.readInt(), buf.readInt(), buf.readInt(),
                buf.readInt(), buf.readInt(), buf.readInt()
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}