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
import java.util.Map;

/**
 * 桃源树枝——会生长的活树干。
 * 主干（facing=up）从生长度 1 缓慢长到 8：前期优先向上抽高，够高后转侧芽与树冠；
 * 侧芽（水平朝向）原地增粗到 6 为止。
 * 掉落按生长度分档（见 loot_tables/blocks/peach_branch.json）：
 * 1-2 桃源树枝，3-6 粗壮桃源树枝，7-8 粗壮桃源树干。
 */
public class PeachBranchBlock extends Block implements Fertilizable {

    public static final IntProperty GROWTH = IntProperty.of("growth", 1, 8);
    public static final EnumProperty<Direction> FACING = EnumProperty.of("facing", Direction.class,
            java.util.List.of(Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST));

    private static final int MAX_TRUNK_HEIGHT = 6;
    private static final int MIN_CANOPY_HEIGHT = 4;
    private static final Direction[] HORIZONTALS = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    /** 各生长度的枝干横截面宽度（半宽），1-2 是可穿行的细枝，7-8 是满格树干 */
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
        setDefaultState(getDefaultState().with(GROWTH, 1).with(FACING, Direction.UP));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(GROWTH, FACING);
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

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        Direction facing = state.get(FACING);
        int growth = state.get(GROWTH);

        // 侧芽：原地缓慢增粗，到 6 为止，不再延伸
        if (facing != Direction.UP) {
            if (growth < 6 && random.nextInt(6) == 0) {
                world.setBlockState(pos, state.with(GROWTH, growth + 1), Block.NOTIFY_ALL);
            }
            return;
        }

        boolean top = !(world.getBlockState(pos.up()).getBlock() instanceof PeachBranchBlock);
        BlockPos above = pos.up();

        // 长到 8 即成熟：补全顶部树冠
        if (growth >= 8 && top) {
            growCanopy(world, above);
            return;
        }

        // 主干缓慢增粗
        if (growth < 8 && random.nextInt(4) == 0) {
            state = state.with(GROWTH, growth + 1);
            world.setBlockState(pos, state, Block.NOTIFY_ALL);
        }

        if (!top || !world.getBlockState(above).isAir()) {
            // 已被树冠封顶：中段偶尔仍会长出侧芽
            if (heightBelow(world, pos) >= 3 && random.nextInt(10) == 0) {
                tryGrowBud(world, pos, random);
            }
            return;
        }

        int height = heightBelow(world, pos) + 1;
        if (height < MIN_CANOPY_HEIGHT) {
            // 前期：优先向上抽高
            if (random.nextInt(2) == 0) {
                world.setBlockState(above, getDefaultState(), Block.NOTIFY_ALL);
            }
        } else if (height < MAX_TRUNK_HEIGHT) {
            int roll = random.nextInt(3);
            if (roll == 0) {
                world.setBlockState(above, getDefaultState(), Block.NOTIFY_ALL);
            } else if (roll == 1) {
                tryGrowBud(world, pos, random);
            }
        } else {
            // 到达设计高度：侧芽与树冠轮替
            if (random.nextBoolean()) {
                tryGrowBud(world, pos, random);
            } else {
                growCanopy(world, above);
            }
        }

        // 中段侧芽：主干够高时下段也可能冒芽
        if (height >= 3 && random.nextInt(12) == 0) {
            tryGrowBud(world, pos, random);
        }
    }

    private static int heightBelow(ServerWorld world, BlockPos pos) {
        int height = 0;
        BlockPos p = pos.down();
        while (height < MAX_TRUNK_HEIGHT) {
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

    // 骨粉：催一节生长度
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
