package com.lidao.moran.systems.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Fertilizable;
import net.minecraft.block.ShapeContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

import java.util.List;

/**
 * 桃花花苞——生殖生长的起点，演绎「花蕾 → 开花 → 展叶」的物候。
 * 复用桃源树枝的请求-检测-休眠 AI；三阶段完全成熟后化为桃花树叶。
 * 顶部花苞（facing=up）成熟时额外生成小树冠。
 */
public class PeachFlowerBudBlock extends Block implements Fertilizable {

    public static final EnumProperty<Direction> FACING = EnumProperty.of("facing", Direction.class,
            List.of(Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST));
    public static final IntProperty STAGE = IntProperty.of("stage", 1, 3);
    public static final IntProperty FAILS = IntProperty.of("fails", 0, 3);

    private static final int REQUEST_INTERVAL = PeachBranchBlock.REQUEST_INTERVAL;
    private static final int DORMANT_TICKS = PeachBranchBlock.DORMANT_TICKS;
    private static final int MAX_FAILS = 3;
    private static final float GROW_CHANCE = 0.5F;
    private static final int MIN_LIGHT = 9;
    private static final float MIN_TEMPERATURE = 0.3F;
    private static final float MAX_TEMPERATURE = 1.2F;
    private static final Direction[] HORIZONTALS = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    public PeachFlowerBudBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(FACING, Direction.UP).with(STAGE, 1).with(FAILS, 0));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, STAGE, FAILS);
    }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean moved) {
        if (!world.isClient && world.getBlockState(pos).getBlock() instanceof PeachFlowerBudBlock) {
            world.scheduleBlockTick(pos, this, REQUEST_INTERVAL);
        }
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        int fails = state.get(FAILS);
        if (fails >= MAX_FAILS) {
            world.setBlockState(pos, state.with(FAILS, 0), Block.NOTIFY_ALL);
            world.scheduleBlockTick(pos, this, REQUEST_INTERVAL);
            return;
        }

        if (world.getLightLevel(pos.up()) < MIN_LIGHT) {
            fail(state, world, pos, fails);
            return;
        }
        float temperature = world.getBiome(pos).value().getTemperature();
        if (temperature < MIN_TEMPERATURE || temperature > MAX_TEMPERATURE) {
            fail(state, world, pos, fails);
            return;
        }

        if (random.nextFloat() < GROW_CHANCE) {
            int stage = state.get(STAGE);
            if (stage >= 3) {
                mature(state, world, pos, random);
                return;
            }
            world.setBlockState(pos, state.with(STAGE, stage + 1), Block.NOTIFY_ALL);
        }
        world.scheduleBlockTick(pos, this, REQUEST_INTERVAL);
    }

    private void fail(BlockState state, ServerWorld world, BlockPos pos, int fails) {
        int next = fails + 1;
        world.setBlockState(pos, state.with(FAILS, next), Block.NOTIFY_ALL);
        world.scheduleBlockTick(pos, this, next >= MAX_FAILS ? DORMANT_TICKS : REQUEST_INTERVAL);
    }

    /** 完全成熟：化为桃花树叶；顶部花苞额外生成小树冠 */
    private void mature(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        boolean top = state.get(FACING) == Direction.UP;
        world.setBlockState(pos, BlockSystem.PEACH_BLOSSOM_LEAVES.getDefaultState(), Block.NOTIFY_ALL);
        if (top) {
            for (Direction d : HORIZONTALS) {
                placeLeafIfAir(world, pos.offset(d));
            }
            for (Direction d : HORIZONTALS) {
                // 四角以半数概率补齐，形成自然圆冠
                if (random.nextBoolean()) {
                    placeLeafIfAir(world, pos.offset(d).up());
                }
            }
        }
    }

    private void placeLeafIfAir(ServerWorld world, BlockPos pos) {
        if (world.getBlockState(pos).isAir()) {
            world.setBlockState(pos, BlockSystem.PEACH_BLOSSOM_LEAVES.getDefaultState(), Block.NOTIFY_ALL);
        }
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.fullCube();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.empty();
    }

    // 骨粉：直接催一阶段
    @Override
    public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state, boolean isClient) {
        return true;
    }

    @Override
    public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
        int stage = state.get(STAGE);
        if (stage >= 3) {
            mature(state, world, pos, random);
        } else {
            world.setBlockState(pos, state.with(STAGE, stage + 1), Block.NOTIFY_ALL);
        }
    }
}
