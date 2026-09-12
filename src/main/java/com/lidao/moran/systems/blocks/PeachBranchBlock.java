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

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 桃源树枝——种植 AI 驱动的活树干。
 *
 * 生长是「请求 → 检测 → 执行」的循环：每个未成熟部分每分钟（1200 tick）向服务端
 * 发一次生长请求；服务端先检查生长条件（光照、温度、上方无遮挡、有生长空间），
 * 连续 3 次不满足则休眠 24 分钟（28800 tick）后再恢复请求；条件满足后掷随机数，
 * 通过才真正生长。最终形态已知（桃花树结构）：先纵向抽高到目标高度，再长侧芽，
 * 顶部成熟（8）后长出树冠。
 *
 * 掉落按生长度分档（见 loot_tables/blocks/peach_branch.json）：
 * 1-2 桃源树枝，3-6 粗壮桃源树枝，7-8 粗壮桃源树干。
 */
public class PeachBranchBlock extends Block implements Fertilizable {

    public static final IntProperty GROWTH = IntProperty.of("growth", 1, 8);
    public static final EnumProperty<Direction> FACING = EnumProperty.of("facing", Direction.class,
            List.of(Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST));
    /** 连续条件不满足计数；达到 3 进入休眠 */
    public static final IntProperty FAILS = IntProperty.of("fails", 0, 3);

    /** 生长请求周期：60 秒 */
    public static final int REQUEST_INTERVAL = 1200;
    /** 休眠时长：24 分钟 */
    public static final int DORMANT_TICKS = 28800;
    private static final int MAX_FAILS = 3;
    /** 条件满足后掷骰通过的概率 */
    private static final float GROW_CHANCE = 0.5F;
    private static final int MIN_LIGHT = 9;
    private static final float MIN_TEMPERATURE = 0.3F;
    private static final float MAX_TEMPERATURE = 1.2F;
    /** 目标主干高度（桃花树已知形态） */
    private static final int TARGET_HEIGHT = 5;
    private static final int BUD_MAX_GROWTH = 6;
    private static final Direction[] HORIZONTALS = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    /** 各生长度的枝干横截面半宽，1-2 是可穿行的细枝，7-8 是满格树干 */
    private static final double[] HALF_WIDTH = {0, 2, 3, 4, 5, 6, 7, 8, 8};

    private static final VoxelShape[] TRUNK_SHAPES = new VoxelShape[9];
    private static final Map<Direction, VoxelShape[]> BUD_SHAPES = new EnumMap<>(Direction.class);

    static {
        for (int g = 1; g <= 8; g++) {
            double h = HALF_WIDTH[g];
            TRUNK_SHAPES[g] = Block.createCuboidShape(8 - h, 0, 8 - h, 8 + h, 16, 8 + h);
        }
        for (Direction d : HORIZONTALS) {
            VoxelShape[] shapes = new VoxelShape[9];
            for (int g = 1; g <= 8; g++) {
                double h = Math.min(HALF_WIDTH[g], 6);
                shapes[g] = switch (d) {
                    // 芽块紧贴其朝向反侧的面（主干所在一侧）
                    case EAST -> Block.createCuboidShape(0, 8 - h, 8 - h, 12, 8 + h, 8 + h);
                    case WEST -> Block.createCuboidShape(4, 8 - h, 8 - h, 16, 8 + h, 8 + h);
                    case SOUTH -> Block.createCuboidShape(8 - h, 8 - h, 0, 8 + h, 8 + h, 12);
                    case NORTH -> Block.createCuboidShape(8 - h, 8 - h, 4, 8 + h, 8 + h, 16);
                    default -> VoxelShapes.empty();
                };
            }
            BUD_SHAPES.put(d, shapes);
        }
    }

    public PeachBranchBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(GROWTH, 1).with(FACING, Direction.UP).with(FAILS, 0));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(GROWTH, FACING, FAILS);
    }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean moved) {
        // 新枝条入世（玩家种植/母株长出），登记第一次生长请求
        if (!world.isClient && world.getBlockState(pos).getBlock() instanceof PeachBranchBlock) {
            world.scheduleBlockTick(pos, this, REQUEST_INTERVAL);
        }
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        int fails = state.get(FAILS);

        // 休眠期满：醒来，重置计数，进入下一轮请求循环
        if (fails >= MAX_FAILS) {
            world.setBlockState(pos, state.with(FAILS, 0), Block.NOTIFY_ALL);
            world.scheduleBlockTick(pos, this, REQUEST_INTERVAL);
            return;
        }

        if (canGrowAt(state, world, pos)) {
            // 条件满足：掷随机数决定本次是否生长；骰子失败不计入休眠次数
            if (random.nextFloat() < GROW_CHANCE) {
                growPart(state, world, pos, random);
                if (!(world.getBlockState(pos).getBlock() instanceof PeachBranchBlock)) {
                    return; // 生长过程中方块被替换的极端情况
                }
                if (world.getBlockState(pos).get(FAILS) != 0) {
                    world.setBlockState(pos, world.getBlockState(pos).with(FAILS, 0), Block.NOTIFY_ALL);
                }
            }
            world.scheduleBlockTick(pos, this, REQUEST_INTERVAL);
        } else {
            int next = fails + 1;
            world.setBlockState(pos, state.with(FAILS, next), Block.NOTIFY_ALL);
            // 连续 3 次条件不满足：休眠 24 分钟
            world.scheduleBlockTick(pos, this, next >= MAX_FAILS ? DORMANT_TICKS : REQUEST_INTERVAL);
        }
    }

    /**
     * 生长条件检测：光照充足、温度适宜；主干顶端若还需向上抽高，则要求上方无遮挡。
     * 桃花树最终形态已知，检测因此可以简单明确。
     */
    private boolean canGrowAt(BlockState state, ServerWorld world, BlockPos pos) {
        if (world.getLightLevel(pos.up()) < MIN_LIGHT) {
            return false;
        }
        float temperature = world.getBiome(pos).value().getTemperature();
        if (temperature < MIN_TEMPERATURE || temperature > MAX_TEMPERATURE) {
            return false;
        }
        if (state.get(FACING) == Direction.UP && state.get(GROWTH) < 8) {
            boolean top = !(world.getBlockState(pos.up()).getBlock() instanceof PeachBranchBlock);
            if (top && heightBelow(world, pos) + 1 < TARGET_HEIGHT && !world.getBlockState(pos.up()).isAir()) {
                return false; // 还需向上但上方被遮挡
            }
        }
        return true;
    }

    /** 执行一次生长：自身成熟度 +1，并按已知形态推进结构（抽高 / 侧芽 / 树冠） */
    private void growPart(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        int growth = state.get(GROWTH);

        if (state.get(FACING) != Direction.UP) {
            // 侧芽：原地增粗，到 6 为止
            if (growth < BUD_MAX_GROWTH) {
                world.setBlockState(pos, state.with(GROWTH, growth + 1), Block.NOTIFY_ALL);
            }
            return;
        }

        if (growth < 8) {
            world.setBlockState(pos, state.with(GROWTH, growth + 1), Block.NOTIFY_ALL);
        }

        boolean top = !(world.getBlockState(pos.up()).getBlock() instanceof PeachBranchBlock);
        BlockPos above = pos.up();
        if (!top || !world.getBlockState(above).isAir()) {
            return;
        }

        int height = heightBelow(world, pos) + 1;
        if (height < TARGET_HEIGHT) {
            // 前期优先向上：长出新节（新节入世自动登记生长请求）
            world.setBlockState(above, getDefaultState(), Block.NOTIFY_ALL);
        } else if (growth >= 6 && random.nextBoolean()) {
            tryGrowBud(world, pos, random);
        } else if (growth >= 8) {
            growCanopy(world, above);
        }
    }

    private static int heightBelow(ServerWorld world, BlockPos pos) {
        int height = 0;
        BlockPos p = pos.down();
        while (height < TARGET_HEIGHT) {
            BlockState s = world.getBlockState(p);
            if (!(s.getBlock() instanceof PeachBranchBlock) || s.get(FACING) != Direction.UP) {
                break;
            }
            height++;
            p = p.down();
        }
        return height;
    }

    private void tryGrowBud(ServerWorld world, BlockPos pos, Random random) {
        Direction d = HORIZONTALS[random.nextInt(HORIZONTALS.length)];
        BlockPos p = pos.offset(d);
        if (world.getBlockState(p).isAir()) {
            world.setBlockState(p, getDefaultState().with(FACING, d), Block.NOTIFY_ALL);
        }
    }

    private static void growCanopy(ServerWorld world, BlockPos above) {
        if (world.getBlockState(above).isAir()) {
            world.setBlockState(above, BlockSystem.PEACH_BLOSSOM_LEAVES.getDefaultState(), Block.NOTIFY_ALL);
        }
        for (Direction d : HORIZONTALS) {
            BlockPos p = above.offset(d);
            if (world.getBlockState(p).isAir()) {
                world.setBlockState(p, BlockSystem.PEACH_BLOSSOM_LEAVES.getDefaultState(), Block.NOTIFY_ALL);
            }
        }
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return state.get(FACING) == Direction.UP
                ? TRUNK_SHAPES[state.get(GROWTH)]
                : BUD_SHAPES.get(state.get(FACING))[state.get(GROWTH)];
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        // 细枝可穿行，侧芽不挡路，粗壮主干才实心
        if (state.get(FACING) != Direction.UP || state.get(GROWTH) < 3) {
            return VoxelShapes.empty();
        }
        return TRUNK_SHAPES[state.get(GROWTH)];
    }

    // 骨粉：直接催一节成熟度（绕过请求与随机数）
    @Override
    public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state, boolean isClient) {
        return state.get(GROWTH) < 8;
    }

    @Override
    public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
        int growth = state.get(GROWTH);
        if (growth < 8) {
            world.setBlockState(pos, state.with(GROWTH, growth + 1), Block.NOTIFY_ALL);
        }
    }
}
