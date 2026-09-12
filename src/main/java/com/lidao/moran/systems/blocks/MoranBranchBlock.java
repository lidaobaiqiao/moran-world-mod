package com.lidao.moran.systems.blocks;

import com.lidao.moran.systems.trees.TreeSpecies;
import com.lidao.moran.systems.trees.Trees;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Fertilizable;
import net.minecraft.block.ShapeContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
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
 * 通用活树枝干方块——生长引擎的树干/侧枝部分，所有树种共用。
 * 行为由 {@link TreeSpecies} 档案驱动：数值参数决定节奏与形态，
 * 策略钩子决定萌芽分布、延伸方向与开花方式（详见 TreeSpecies 类注释）。
 *
 * 掉落按生长度分档（见各树种枝干方块的战利品表）：
 * 1-2 桃源树枝，3-6 粗壮桃源树枝，7-8 粗壮桃源树干。
 */
public class MoranBranchBlock extends Block implements Fertilizable {

    public static final IntProperty GROWTH = IntProperty.of("growth", 1, 8);
    public static final EnumProperty<Direction> FACING = EnumProperty.of("facing", Direction.class,
            List.of(Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST));
    public static final IntProperty FAILS = IntProperty.of("fails", 0, TreeSpecies.MAX_FAILS);
    public static final BooleanProperty DORMANT = BooleanProperty.of("dormant");

    private final TreeSpecies species;

    /** 各生长度的枝干横截面半宽，1-2 是可穿行的细枝，7-8 是满格树干 */
    private static final double[] HALF_WIDTH = {0, 2, 3, 4, 5, 6, 7, 8, 8};
    private static final VoxelShape[] TRUNK_SHAPES = new VoxelShape[9];
    private static final Map<Direction, VoxelShape[]> BUD_SHAPES = new EnumMap<>(Direction.class);

    static {
        for (int g = 1; g <= 8; g++) {
            double h = HALF_WIDTH[g];
            TRUNK_SHAPES[g] = Block.createCuboidShape(8 - h, 0, 8 - h, 8 + h, 16, 8 + h);
        }
        for (Direction d : TreeSpecies.HORIZONTALS) {
            VoxelShape[] shapes = new VoxelShape[9];
            for (int g = 1; g <= 8; g++) {
                double h = Math.min(HALF_WIDTH[g], 6);
                shapes[g] = switch (d) {
                    // 芽块紧贴其朝向反侧的面（母体所在一侧）
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

    public MoranBranchBlock(TreeSpecies species, Settings settings) {
        super(settings);
        this.species = species;
        setDefaultState(getDefaultState()
                .with(GROWTH, 1)
                .with(FACING, Direction.UP)
                .with(FAILS, 0)
                .with(DORMANT, false));
    }

    public TreeSpecies species() {
        return species;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(GROWTH, FACING, FAILS, DORMANT);
    }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean moved) {
        // 新枝条入世（玩家种植/母株长出），登记第一次生长请求
        if (!world.isClient && world.getBlockState(pos).getBlock() instanceof MoranBranchBlock) {
            world.scheduleBlockTick(pos, this, TreeSpecies.REQUEST_INTERVAL);
        }
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        // 休眠侧芽不参与请求循环，由主干到顶时统一唤醒
        if (state.get(DORMANT)) {
            return;
        }

        int fails = state.get(FAILS);
        if (fails >= TreeSpecies.MAX_FAILS) {
            // 休眠期满：醒来，重置计数，进入下一轮请求循环
            world.setBlockState(pos, state.with(FAILS, 0), Block.NOTIFY_ALL);
            world.scheduleBlockTick(pos, this, TreeSpecies.REQUEST_INTERVAL);
            return;
        }

        if (species.checkEnvironment(world, pos)) {
            // 条件满足：掷随机数决定本次是否生长；骰子失败不计入休眠次数
            if (random.nextFloat() < species.growChance()) {
                growPart(state, world, pos, random);
                if (!(world.getBlockState(pos).getBlock() instanceof MoranBranchBlock)) {
                    return;
                }
                if (world.getBlockState(pos).get(FAILS) != 0) {
                    world.setBlockState(pos, world.getBlockState(pos).with(FAILS, 0), Block.NOTIFY_ALL);
                }
            }
            world.scheduleBlockTick(pos, this, TreeSpecies.REQUEST_INTERVAL);
        } else {
            int next = fails + 1;
            world.setBlockState(pos, state.with(FAILS, next), Block.NOTIFY_ALL);
            // 连续 3 次条件不满足：休眠 24 分钟
            world.scheduleBlockTick(pos, this, next >= TreeSpecies.MAX_FAILS ? TreeSpecies.DORMANT_TICKS : TreeSpecies.REQUEST_INTERVAL);
        }
    }

    /** 执行一次生长：自身成熟度 +1，再按身份推进结构（抽高/侧芽/侧枝/开花） */
    private void growPart(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        Direction facing = state.get(FACING);
        int growth = state.get(GROWTH);
        boolean trunk = facing == Direction.UP;

        // 自身成熟：主干至 trunkMaxGrowth，侧枝至 branchMaxGrowth
        int cap = trunk ? species.trunkMaxGrowth() : species.branchMaxGrowth();
        if (growth < cap) {
            growth += 1;
            world.setBlockState(pos, state.with(GROWTH, growth), Block.NOTIFY_ALL);
        }

        if (trunk) {
            growTrunk(world, pos, random, growth);
        } else {
            growBranch(world, pos, random, growth, facing);
        }
    }

    /** 主干生长：抽高 → 到顶后唤醒侧芽并长顶花苞 */
    private void growTrunk(ServerWorld world, BlockPos pos, Random random, int growth) {
        BlockPos above = pos.up();
        boolean top = !(world.getBlockState(above).getBlock() instanceof MoranBranchBlock);
        int height = heightBelow(world, pos, species.biologicalTop()) + 1;
        boolean topped = height >= species.biologicalTop() || (top && !world.getBlockState(above).isAir());

        if (topped) {
            // 到顶：唤醒全部休眠侧芽（幂等），侧枝期开始
            wakeDormantBuds(world, pos);
            if (top && growth >= species.topBudGrowth() && world.getBlockState(above).isAir()) {
                // 顶端方块成熟度达标：长出顶花苞
                world.setBlockState(above, species.budBlock().getDefaultState(), Block.NOTIFY_ALL);
            }
        } else if (top && world.getBlockState(above).isAir()) {
            // 抽高期：自身 >= 2 才能向上生新节（新节恒为 1，「新块最大为自身减一」）
            if (growth >= 2) {
                world.setBlockState(above, getDefaultState(), Block.NOTIFY_ALL);
            }
        }

        // 抽高期：按树种偏好萌发休眠侧芽（数量上限内，概率随数量递减 → 密集侧枝）
        if (!topped && growth >= 2) {
            int buds = countBudsAroundTrunk(world, pos);
            if (buds < species.maxBuds()
                    && species.isBudPosition(world, pos, height, height + trunkSegmentsAbove(world, pos))
                    && random.nextInt(species.budChanceDenom()) < (species.maxBuds() - buds)) {
                tryGrowBud(world, pos, random, true);
            }
        }
    }

    /** 侧枝生长：门槛后每次生长有概率停止进入开花，上限强制停；开花前可延伸一个子侧枝 */
    private void growBranch(ServerWorld world, BlockPos pos, Random random, int growth, Direction facing) {
        // 停止机制：一旦开始生成花苞就不再生成侧枝（停止即终末）
        if (growth >= species.branchStopGrowth()
                && (growth >= species.branchMaxGrowth() || random.nextFloat() < species.branchStopChance())) {
            species.onBranchStop(world, pos, facing, random);
            return;
        }

        // 子侧枝：成熟 >5 且链上位置距主干 >1，末端为空（末端被占即已生过，自然限一个）
        Direction childDir = species.branchChildDirection(world, pos, facing);
        BlockPos tip = pos.offset(childDir);
        if (growth > 5 && chainPosition(world, pos, facing) > 1
                && random.nextFloat() < species.subBranchChance()
                && world.getBlockState(tip).isAir()) {
            world.setBlockState(tip, getDefaultState().with(FACING, childDir), Block.NOTIFY_ALL);
        }
    }

    private void tryGrowBud(ServerWorld world, BlockPos pos, Random random, boolean dormant) {
        Direction d = TreeSpecies.HORIZONTALS[random.nextInt(TreeSpecies.HORIZONTALS.length)];
        BlockPos p = pos.offset(d);
        if (world.getBlockState(p).isAir()) {
            world.setBlockState(p, getDefaultState().with(FACING, d).with(DORMANT, dormant), Block.NOTIFY_ALL);
        }
    }

    private static int heightBelow(ServerWorld world, BlockPos pos, int limit) {
        int height = 0;
        BlockPos p = pos.down();
        while (height < limit) {
            BlockState s = world.getBlockState(p);
            if (!(s.getBlock() instanceof MoranBranchBlock) || s.get(FACING) != Direction.UP) {
                break;
            }
            height++;
            p = p.down();
        }
        return height;
    }

    /** 自身是否位于主干可萌发位置（由树种档案判定，默认上半部分） */
    private boolean inBudPosition(ServerWorld world, BlockPos pos) {
        int height = heightBelow(world, pos, species.biologicalTop()) + 1;
        int total = height + trunkSegmentsAbove(world, pos);
        return species.isBudPosition(world, pos, height, total);
    }

    private int trunkSegmentsAbove(ServerWorld world, BlockPos pos) {
        int n = 0;
        BlockPos p = pos.up();
        while (n < species.biologicalTop()) {
            BlockState s = world.getBlockState(p);
            if (!(s.getBlock() instanceof MoranBranchBlock) || s.get(FACING) != Direction.UP) {
                break;
            }
            n++;
            p = p.up();
        }
        return n;
    }

    /** 主干整列上已存在的侧芽数量（用于递减概率） */
    private int countBudsAroundTrunk(ServerWorld world, BlockPos trunkPos) {
        int count = 0;
        BlockPos p = trunkPos;
        while (world.getBlockState(p.down()).getBlock() instanceof MoranBranchBlock) {
            p = p.down();
        }
        BlockPos cur = p;
        while (true) {
            for (Direction d : TreeSpecies.HORIZONTALS) {
                if (world.getBlockState(cur.offset(d)).getBlock() instanceof MoranBranchBlock) {
                    count++;
                }
            }
            BlockState above = world.getBlockState(cur.up());
            if (above.getBlock() instanceof MoranBranchBlock && above.get(FACING) == Direction.UP) {
                cur = cur.up();
            } else {
                return count;
            }
        }
    }

    /** 主干到顶：唤醒整列上的全部休眠侧芽（幂等） */
    private void wakeDormantBuds(ServerWorld world, BlockPos trunkPos) {
        BlockPos p = trunkPos;
        while (world.getBlockState(p.down()).getBlock() instanceof MoranBranchBlock) {
            p = p.down();
        }
        BlockPos cur = p;
        while (true) {
            for (Direction d : TreeSpecies.HORIZONTALS) {
                BlockPos n = cur.offset(d);
                BlockState s = world.getBlockState(n);
                if (s.getBlock() instanceof MoranBranchBlock && s.get(DORMANT)) {
                    world.setBlockState(n, s.with(DORMANT, false), Block.NOTIFY_ALL);
                    world.scheduleBlockTick(n, s.getBlock(), TreeSpecies.REQUEST_INTERVAL);
                }
            }
            BlockState above = world.getBlockState(cur.up());
            if (above.getBlock() instanceof MoranBranchBlock && above.get(FACING) == Direction.UP) {
                cur = cur.up();
            } else {
                return;
            }
        }
    }

    /** 自身在同朝向侧枝链上的位置（距主干的水平距离） */
    private static int chainPosition(ServerWorld world, BlockPos pos, Direction facing) {
        int dist = 1;
        BlockPos p = pos.offset(facing.getOpposite());
        while (dist < 16) {
            BlockState s = world.getBlockState(p);
            if (!(s.getBlock() instanceof MoranBranchBlock) || s.get(FACING) != facing) {
                return dist;
            }
            dist++;
            p = p.offset(facing.getOpposite());
        }
        return dist;
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
        int growth = state.get(GROWTH);
        return state.get(FACING) == Direction.UP
                ? growth < species.trunkMaxGrowth()
                : growth < species.branchMaxGrowth();
    }

    @Override
    public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
        int growth = state.get(GROWTH);
        int cap = state.get(FACING) == Direction.UP ? species.trunkMaxGrowth() : species.branchMaxGrowth();
        if (growth < cap) {
            world.setBlockState(pos, state.with(GROWTH, growth + 1), Block.NOTIFY_ALL);
        }
    }
}
