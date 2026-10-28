package dev.slarrties.privit.common.block;

import com.mojang.serialization.MapCodec;

import dev.slarrties.privit.common.network.payload.s2c.RegionGridStateS2CPacket;
import dev.slarrties.privit.common.network.payload.c2s.RegionGuiRequestC2SPacket;
import dev.slarrties.privit.server.world.WorldRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.UUID;

public class RegionTableBlock extends HorizontalDirectionalBlock {

    public static final MapCodec<RegionTableBlock> CODEC = simpleCodec(RegionTableBlock::new);

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    public RegionTableBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            ClientPlayNetworking.send(new RegionGuiRequestC2SPacket(pos));
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void destroy(LevelAccessor level, BlockPos pos, BlockState state) {
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            UUID regionId = WorldRegistry.get(serverLevel)
                    .getRegionGuiSessions()
                    .closePendingAt(pos);

            if (regionId != null) {
                RegionGridStateS2CPacket packet = RegionGridStateS2CPacket.hide(regionId);

                for (Player p : serverLevel.players()) {
                    ServerPlayNetworking.send((ServerPlayer) p, packet);
                }
            }
        }

        super.destroy(level, pos, state);
    }
}