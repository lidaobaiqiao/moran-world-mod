package com.lidao.moran.systems.blocks;

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
 * 桃源树枝——种植 AI 驱动的活树干，对应现实桃树的营养生长阶段。
 *
 * 生长是「请求 → 检测 → 执行」的循环：每个未成熟部分每分钟（1200 tick）向服务端
 * 发一次生长请求；服务端检查生长条件（光照、温度），连续 3 次不满足则休眠 24 分钟
 * （28800 tick）；条件满足后掷随机数，通过才生长。
 *
 * 每个方块独立生长、独立休眠，一棵树上有多个生长点并行活动：
 * - 主干（facing=up）：自身成熟度达到 2 后才能向上生出新节（新节恒为 1，即
 *   「新块最大为自身减一」），于是抽高与增粗并行，树形永远下粗上细；
 *   主干长到生物顶端（BIOLOGICAL_TOP 节或顶端被遮挡）后进入侧枝期。
 * - 休眠侧芽：抽高期随机出现在主干上半部分，到顶前不生长。
 * - 侧枝：到顶后侧芽苏醒，按与主干相同的规则生长（成熟度上限 7）；成熟度过门槛后
 *   每次生长都有概率停止——停止即进入开花：末端与四周生成花苞，此后不再生侧枝。
 *   现实对照：先营养生长后生殖生长，花芽着生在一年生枝的顶端与侧腋。
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
    /** 休眠侧芽：出现在主干上半部分，主干到顶后苏醒 */
    public static final BooleanProperty DORMANT = BooleanProperty.of("dormant");

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
    /** 生物顶端：主干最高节数（高大桃树） */
    private static final int BIOLOGICAL_TOP = 7;
    /** 顶端长出顶花苞所需的成熟度 */
    private static final int TOP_BUD_GROWTH = 5;
    /** 主干侧芽上限（概率随已有数量递减，形成密集侧枝） */
    private static final int MAX_BUDS = 4;
    /** 侧枝成熟度上限 */
    private static final int BRANCH_MAX_GROWTH = 7;
    /** 侧枝可开始停止的成熟度门槛 */
    private static final int BRANCH_STOP_GROWTH = 4;
    /** 侧枝停止概率分母：1/4 */
    private static final int BRANCH_STOP_DENOM = 4;
    /** 侧枝延伸子侧枝概率分母：1/4 */
    private static final int SUB_BRANCH_DENOM = 4;
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

    public PeachBranchBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState()
                .with(GROWTH, 1)
                .with(FACING, Direction.UP)
                .with(FAILS, 0)
                .with(DORMANT, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(GROWTH, FACING, FAILS, DORMANT);
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
        // 休眠侧芽不参与请求循环，由主干到顶时统一唤醒
        if (state.get(DORMANT)) {
            return;
        }

        int fails = state.get(FAILS);
        if (fails >= MAX_FAILS) {
            // 休眠期满：醒来，重置计数，进入下一轮请求循环
            world.setBlockState(pos, state.with(FAILS, 0), Block.NOTIFY_ALL);
            world.scheduleBlockTick(pos, this, REQUEST_INTERVAL);
            return;
        }

        if (canGrowAt(world, pos)) {
            // 条件满足：掷随机数决定本次是否生长；骰子失败不计入休眠次数
            if (random.nextFloat() < GROW_CHANCE) {
                growPart(state, world, pos, random);
                if (!(world.getBlockState(pos).getBlock() instanceof PeachBranchBlock)) {
                    return;
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

    /** 生长条件检测：光照充足、温度适宜 */
    private boolean canGrowAt(ServerWorld world, BlockPos pos) {
        if (world.getLightLevel(pos.up()) < MIN_LIGHT) {
            return false;
        }
        float temperature = world.getBiome(pos).value().getTemperature();
        return temperature >= MIN_TEMPERATURE && temperature <= MAX_TEMPERATURE;
    }

    /** 执行一次生长：自身成熟度 +1，再按身份推进结构（抽高/侧芽/侧枝/开花） */
    private void growPart(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        Direction facing = state.get(FACING);
        int growth = state.get(GROWTH);
        boolean trunk = facing == Direction.UP;

        // 自身成熟：主干至 8，侧枝至 7
        if (growth < (trunk ? 8 : BRANCH_MAX_GROWTH)) {
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
        boolean top = !(world.getBlockState(above).getBlock() instanceof PeachBranchBlock);
        int height = heightBelow(world, pos) + 1;
        boolean topped = height >= BIOLOGICAL_TOP || (top && !world.getBlockState(above).isAir());

        if (topped) {
            // 到顶：唤醒全部休眠侧芽（幂等），侧枝期开始
            wakeDormantBuds(world, pos);
            if (top && growth >= TOP_BUD_GROWTH && world.getBlockState(above).isAir()) {
                // 顶端方块成熟度达标：长出顶花苞
                world.setBlockState(above, BlockSystem.PEACH_FLOWER_BUD.getDefaultState(), Block.NOTIFY_ALL);
            }
        } else if (top && world.getBlockState(above).isAir()) {
            // 抽高期：自身 >= 2 才能向上生新节（新节恒为 1，「新块最大为自身减一」）
            if (growth >= 2) {
                world.setBlockState(above, getDefaultState(), Block.NOTIFY_ALL);
            }
        }

        // 抽高期：主干上半部分随机生成休眠侧芽（上限 MAX_BUDS，概率随数量递减 → 密集侧枝）
        if (!topped && growth >= 2 && inUpperHalf(world, pos)) {
            int buds = countBudsAroundTrunk(world, pos);
            if (buds < MAX_BUDS && random.nextInt(8) < (MAX_BUDS - buds)) {
                tryGrowBud(world, pos, random, true);
            }
        }
    }

    /** 侧枝生长：门槛后每次生长有概率停止进入开花，7 强制停；开花前可延伸一个子侧枝 */
    private void growBranch(ServerWorld world, BlockPos pos, Random random, int growth, Direction facing) {
        BlockPos tip = pos.offset(facing);

        // 停止机制：一旦开始生成花苞就不再生成侧枝（停止即终末）
        if (growth >= BRANCH_STOP_GROWTH
                && (growth >= BRANCH_MAX_GROWTH || random.nextInt(BRANCH_STOP_DENOM) == 0)) {
            bloom(world, pos, facing);
            return;
        }

        // 子侧枝：成熟 >5 且链上位置距主干 >1，末端为空（末端被占即已生过，自然限一个）
        if (growth > 5 && chainPosition(world, pos, facing) > 1
                && random.nextInt(SUB_BRANCH_DENOM) == 0
                && world.getBlockState(tip).isAir()) {
            world.setBlockState(tip, getDefaultState().with(FACING, facing), Block.NOTIFY_ALL);
        }
    }

    /** 开花：末端与四周（上方 + 两侧垂直向）生成花苞；现实桃树的花芽着生方式 */
    private void bloom(ServerWorld world, BlockPos pos, Direction facing) {
        placeBudIfAir(world, pos.offset(facing), facing);
        Direction left = facing.rotateYCounterclockwise();
        Direction right = facing.rotateYClockwise();
        placeBudIfAir(world, pos.up(), Direction.UP);
        placeBudIfAir(world, pos.offset(left), left);
        placeBudIfAir(world, pos.offset(right), right);
    }

    private void placeBudIfAir(ServerWorld world, BlockPos pos, Direction facing) {
        if (world.getBlockState(pos).isAir()) {
            world.setBlockState(pos, BlockSystem.PEACH_FLOWER_BUD.getDefaultState()
                    .with(PeachFlowerBudBlock.FACING, facing), Block.NOTIFY_ALL);
        }
    }

    private void tryGrowBud(ServerWorld world, BlockPos pos, Random random, boolean dormant) {
        Direction d = HORIZONTALS[random.nextInt(HORIZONTALS.length)];
        BlockPos p = pos.offset(d);
        if (world.getBlockState(p).isAir()) {
            world.setBlockState(p, getDefaultState().with(FACING, d).with(DORMANT, dormant), Block.NOTIFY_ALL);
        }
    }

    private static int heightBelow(ServerWorld world, BlockPos pos) {
        int height = 0;
        BlockPos p = pos.down();
        while (height < BIOLOGICAL_TOP) {
            BlockState s = world.getBlockState(p);
            if (!(s.getBlock() instanceof PeachBranchBlock) || s.get(FACING) != Direction.UP) {
                break;
            }
            height++;
            p = p.down();
        }
        return height;
    }

    /** 自身是否位于主干当前高度的上半部分（现实桃树主枝自上部萌发） */
    private boolean inUpperHalf(ServerWorld world, BlockPos pos) {
        int height = heightBelow(world, pos) + 1;
        int total = height + trunkSegmentsAbove(world, pos);
        return height > total / 2;
    }

    private static int trunkSegmentsAbove(ServerWorld world, BlockPos pos) {
        int n = 0;
        BlockPos p = pos.up();
        while (n < BIOLOGICAL_TOP) {
            BlockState s = world.getBlockState(p);
            if (!(s.getBlock() instanceof PeachBranchBlock) || s.get(FACING) != Direction.UP) {
                break;
            }
            n++;
            p = p.up();
        }
        return n;
    }

    /** 主干整列上已存在的侧芽数量（用于递减概率） */
    private static int countBudsAroundTrunk(ServerWorld world, BlockPos trunkPos) {
        int count = 0;
        BlockPos p = trunkPos;
        while (world.getBlockState(p.down()).getBlock() instanceof PeachBranchBlock) {
            p = p.down();
        }
        BlockPos cur = p;
        while (true) {
            for (Direction d : HORIZONTALS) {
                if (world.getBlockState(cur.offset(d)).getBlock() instanceof PeachBranchBlock) {
                    count++;
                }
            }
            BlockState above = world.getBlockState(cur.up());
            if (above.getBlock() instanceof PeachBranchBlock && above.get(FACING) == Direction.UP) {
                cur = cur.up();
            } else {
                return count;
            }
        }
    }

    /** 主干到顶：唤醒整列上的全部休眠侧芽（幂等） */
    private static void wakeDormantBuds(ServerWorld world, BlockPos trunkPos) {
        BlockPos p = trunkPos;
        while (world.getBlockState(p.down()).getBlock() instanceof PeachBranchBlock) {
            p = p.down();
        }
        BlockPos cur = p;
        while (true) {
            for (Direction d : HORIZONTALS) {
                BlockPos n = cur.offset(d);
                BlockState s = world.getBlockState(n);
                if (s.getBlock() instanceof PeachBranchBlock && s.get(DORMANT)) {
                    world.setBlockState(n, s.with(DORMANT, false), Block.NOTIFY_ALL);
                    world.scheduleBlockTick(n, s.getBlock(), REQUEST_INTERVAL);
                }
            }
            BlockState above = world.getBlockState(cur.up());
            if (above.getBlock() instanceof PeachBranchBlock && above.get(FACING) == Direction.UP) {
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
            if (!(s.getBlock() instanceof PeachBranchBlock) || s.get(FACING) != facing) {
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
        return state.get(FACING) == Direction.UP ? growth < 8 : growth < BRANCH_MAX_GROWTH;
    }

    @Override
    public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
        int growth = state.get(GROWTH);
        int cap = state.get(FACING) == Direction.UP ? 8 : BRANCH_MAX_GROWTH;
        if (growth < cap) {
            world.setBlockState(pos, state.with(GROWTH, growth + 1), Block.NOTIFY_ALL);
        }
    }
}
