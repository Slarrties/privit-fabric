package dev.slarrties.privit.common.block;

import dev.slarrties.privit.client.network.ClientPacketSender;
import dev.slarrties.privit.common.network.payload.c2s.RegionGuiRequestC2SPacket;
import dev.slarrties.privit.common.network.payload.s2c.RegionGridStateS2CPacket;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.network.ServerPacketSender;

import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.state.StateManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

public class RegionTableBlock extends HorizontalFacingBlock {

    public RegionTableBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) {
            ClientPacketSender.send(new RegionGuiRequestC2SPacket(pos));
            return ActionResult.SUCCESS;
        }

        return ActionResult.SUCCESS;
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock()) && !world.isClient && world instanceof ServerWorld serverWorld) {
            UUID regionId = WorldRegistry.get(serverWorld)
                    .getRegionGuiSessions()
                    .closePendingAt(pos);

            if (regionId != null) {
                RegionGridStateS2CPacket packet = RegionGridStateS2CPacket.hide(regionId);

                for (PlayerEntity player : world.getPlayers()) {
                    ServerPacketSender.send((ServerPlayerEntity) player, packet);
                }
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }
}