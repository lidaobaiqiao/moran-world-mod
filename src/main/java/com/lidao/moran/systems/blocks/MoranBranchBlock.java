package com.lidao.moran.systems.blocks;

import com.lidao.moran.systems.trees.SoilProfile;
import com.lidao.moran.systems.trees.Soils;
import com.lidao.moran.systems.trees.TreeSpecies;
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
 * 行为由 {@link TreeSpecies} 档案驱动，环境参数系统接入：
 * 水度（生长速度软增益 + 硬门槛）、土壤三轴偏好、空间竞争、向光性、修剪响应。
 *
 * 掉落按生长度分档（见各树种枝干方块的战利品表）：
 * 1-2 桃源树枝，3-6 粗壮桃源树枝，7-8 粗壮桃源树干。
 */
public class MoranBranchBlock extends Block implements Fertilizable {

    public static final IntProperty GROWTH = IntProperty.of("growth", 1, 8);
    public static final EnumProperty<Direction> FACING = EnumProperty.of("facing", Direction.class,
            List.of(Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST));
    public static final IntProperty FAILS = IntProperty.of("fails", 0, TreeSpecies.MAX_FAILS);
    /** 休眠侧芽：出现在主干上半部分，主干到顶后苏醒 */
    public static final BooleanProperty DORMANT = BooleanProperty.of("dormant");
    /** 野生模式（世界生成）生长请求周期：0.1 秒 */
    public static final int NATURAL_INTERVAL = 2;
    /** 野生模式（世界生成）：0.1s 一次请求、条件过即立即生长、条件失败直接冻结 */
    public static final BooleanProperty NATURAL = BooleanProperty.of("natural");
    /** 主干封顶标记：到顶决策持久化，唤醒侧芽与顶花苞由此驱动 */
    public static final BooleanProperty TOPPED = BooleanProperty.of("topped");

    private final TreeSpecies species;

    /** 各生长度的枝干横截面半宽：直径 2/4/6/8/10/12/14/16（用户定稿），1-2 是可穿行的细枝，8 为满格 */
    private static final double[] HALF_WIDTH = {0, 1, 2, 3, 4, 5, 6, 7, 8};
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
                .with(DORMANT, false)
                .with(TOPPED, false)
                .with(NATURAL, false));
    }

    public TreeSpecies species() {
        return species;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(GROWTH, FACING, FAILS, DORMANT, TOPPED, NATURAL);
    }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean moved) {
        // 新枝条入世（玩家种植/母株长出），登记第一次生长请求
        if (!world.isClient && world.getBlockState(pos).getBlock() instanceof MoranBranchBlock) {
            BlockState self = world.getBlockState(pos);
            world.scheduleBlockTick(pos, this,
                    self.contains(NATURAL) && self.get(NATURAL)
                            ? NATURAL_INTERVAL : TreeSpecies.REQUEST_INTERVAL);
        }
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        // 休眠侧芽不参与请求循环，由主干到顶时统一唤醒
        if (state.get(DORMANT)) {
            return;
        }

        boolean natural = state.get(NATURAL);
        int interval = natural ? NATURAL_INTERVAL : TreeSpecies.REQUEST_INTERVAL;

        int fails = state.get(FAILS);
        if (fails >= TreeSpecies.MAX_FAILS) {
            // 休眠期满：醒来，重置计数，进入下一轮请求循环
            world.setBlockState(pos, state.with(FAILS, 0), Block.NOTIFY_ALL);
            world.scheduleBlockTick(pos, this, interval);
            return;
        }

        // 环境取样：沿主干向下找根，土壤取根部下方，水度在根层测
        BlockPos root = rootPos(world, pos, species.biologicalTopMax());
        SoilProfile soil = Soils.of(world, root.down());
        int hydration = TreeSpecies.hydration(world, root);

        if (!species.checkEnvironment(world, pos, hydration)) {
            if (natural) {
                return; // 野生树：条件不满足直接冻结，不再请求
            }
            int next = fails + 1;
            world.setBlockState(pos, state.with(FAILS, next), Block.NOTIFY_ALL);
            // 连续 3 次条件不满足：休眠 24 分钟
            world.scheduleBlockTick(pos, this,
                    next >= TreeSpecies.MAX_FAILS ? TreeSpecies.DORMANT_TICKS : TreeSpecies.REQUEST_INTERVAL);
            return;
        }

        // 条件满足：野生树立即生长；种植树掷骰（有效概率 = 基础 × 水度 × 土壤）
        if (natural || random.nextFloat() < species.effectiveGrowChance(world, pos, hydration, soil)) {
            growPart(state, world, pos, random);
            if (!(world.getBlockState(pos).getBlock() instanceof MoranBranchBlock)) {
                return;
            }
            if (world.getBlockState(pos).get(FAILS) != 0) {
                world.setBlockState(pos, world.getBlockState(pos).with(FAILS, 0), Block.NOTIFY_ALL);
            }
        }
        // 野生树完成使命（长满且整树封顶 / 侧枝长满）后永久静默，防 tick 风暴；
        // 种植树保持低频常驻（60s 一次，成本可忽略）
        boolean done = natural && isFullyGrown(state, world, pos);
        if (!done) {
            world.scheduleBlockTick(pos, this, interval);
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
            growTrunk(state, world, pos, random, growth);
        } else {
            growBranch(state, world, pos, random, growth, facing);
        }
    }

    /** 主干生长：抽高与封顶决策 → 到顶唤醒侧芽并长顶花苞 */
    private void growTrunk(BlockState state, ServerWorld world, BlockPos pos, Random random, int growth) {
        BlockPos above = pos.up();
        boolean top = !(world.getBlockState(above).getBlock() instanceof MoranBranchBlock);
        int height = heightBelow(world, pos, species.biologicalTopMax()) + 1;

        // 封顶决策（只在顶端做一次，结果持久化为 TOPPED）：
        // 高度达上限强制封顶；进入区间后每次抽高前掷封顶骰；上方被遮挡视为到顶
        if (top && !state.get(TOPPED)) {
            boolean canExtend = world.getBlockState(above).isAir();
            boolean capped = height >= species.biologicalTopMax() || !canExtend;
            boolean rollStop = height >= species.biologicalTopMin()
                    && random.nextFloat() < species.toppingChance();
            if (capped || (canExtend && rollStop)) {
                state = state.with(TOPPED, true);
                world.setBlockState(pos, state, Block.NOTIFY_ALL);
            }
        }

        if (state.get(TOPPED)) {
            // 到顶：唤醒全部休眠侧芽（幂等），侧枝期开始
            wakeDormantBuds(world, pos);
            if (top && growth >= species.topBudGrowth() && world.getBlockState(above).isAir()
                    && !(world.getBlockState(above).getBlock() instanceof MoranFlowerBudBlock)) {
                // 顶端方块成熟度达标：长出顶花苞
                world.setBlockState(above, species.budBlock().getDefaultState()
                        .with(MoranFlowerBudBlock.NATURAL, state.get(NATURAL)), Block.NOTIFY_ALL);
            }
        } else if (top && world.getBlockState(above).isAir()) {
            // 抽高期：自身 >= 2 才能向上生新节（新节恒为 1，「新块最大为自身减一」）
            if (growth >= 2) {
                world.setBlockState(above, getDefaultState()
                        .with(NATURAL, state.get(NATURAL)), Block.NOTIFY_ALL);
            }
        }

        // 抽高期：按树种偏好萌发休眠侧芽（数量上限内 + 概率递减 + 向光 + 空间竞争）
        // 整树封顶后不再萌发新侧芽（treeTopped 判整树，而非本方块的 TOPPED）
        if (!treeTopped(world, pos) && growth >= 2) {
            int buds = countBudsAroundTrunk(world, pos);
            if (buds < species.maxBuds()
                    && species.isBudPosition(world, pos, height, height + trunkSegmentsAbove(world, pos))
                    && random.nextInt(species.budChanceDenom()) < (species.maxBuds() - buds)) {
                Direction d = phototropicDirection(world, pos, random);
                BlockPos p = pos.offset(d);
                if (world.getBlockState(p).isAir() && !isCrowded(world, p)) {
                    world.setBlockState(p, getDefaultState()
                            .with(FACING, d).with(DORMANT, true)
                            .with(NATURAL, state.get(NATURAL)), Block.NOTIFY_ALL);
                }
            }
        }
    }

    /** 侧枝生长：门槛后每次生长有概率停止进入开花，上限强制停；开花前可延伸一个子侧枝 */
    private void growBranch(BlockState state, ServerWorld world, BlockPos pos, Random random, int growth, Direction facing) {
        // 停止机制：一旦开始生成花苞就不再生成侧枝（停止即终末）
        if (growth >= species.branchStopGrowth()
                && (growth >= species.branchMaxGrowth() || random.nextFloat() < species.branchStopChance())) {
            species.onBranchStop(world, pos, facing, random, state.get(NATURAL));
            return;
        }

        // 子侧枝：成熟 >5 且链上位置距主干 >1，末端为空（末端被占即已生过，自然限一个）
        Direction childDir = species.branchChildDirection(world, pos, facing);
        BlockPos tip = pos.offset(childDir);
        if (growth > 5 && chainPosition(world, pos, facing) > 1
                && random.nextFloat() < species.subBranchChance()
                && world.getBlockState(tip).isAir()) {
            world.setBlockState(tip, getDefaultState()
                    .with(FACING, childDir).with(NATURAL, state.get(NATURAL)), Block.NOTIFY_ALL);
        }
    }

    /**
     * 修剪响应：枝干被剪断后，断口相邻的枝干按档案概率向断口萌发新芽
     * （现实园艺：修剪促萌蘖——玩家由此可以主动塑形自己的树）。
     */
    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        super.onStateReplaced(state, world, pos, newState, moved);
        if (world.isClient
                || newState.getBlock() instanceof MoranBranchBlock
                || world.getRandom().nextFloat() >= species.pruneResponseChance()) {
            return;
        }
        for (Direction d : Direction.values()) {
            BlockPos n = pos.offset(d);
            BlockState neighbor = world.getBlockState(n);
            if (neighbor.getBlock() instanceof MoranBranchBlock) {
                // 断口处生成朝向母体的新芽（growth=1），立即登记生长请求
                world.setBlockState(pos, neighbor
                        .with(FACING, d.getOpposite())
                        .with(GROWTH, 1)
                        .with(DORMANT, false)
                        .with(TOPPED, false)
                        .with(FAILS, 0), Block.NOTIFY_ALL);
                world.scheduleBlockTick(pos, neighbor.getBlock(),
                        neighbor.contains(NATURAL) && neighbor.get(NATURAL)
                                ? NATURAL_INTERVAL : TreeSpecies.REQUEST_INTERVAL);
                break;
            }
        }
    }

    /** 整树是否已封顶（沿主干向上读到顶端方块的 TOPPED，而非本方块的标记） */
    private boolean treeTopped(ServerWorld world, BlockPos pos) {
        BlockPos p = pos;
        while (true) {
            BlockState above = world.getBlockState(p.up());
            if (above.getBlock() instanceof MoranBranchBlock && above.get(FACING) == Direction.UP) {
                p = p.up();
            } else {
                break;
            }
        }
        return world.getBlockState(p).contains(TOPPED) && world.getBlockState(p).get(TOPPED);
    }

    /** 野生树是否已完成全部生长使命（可永久静默） */
    private boolean isFullyGrown(BlockState state, ServerWorld world, BlockPos pos) {
        int growth = state.get(GROWTH);
        if (state.get(FACING) == Direction.UP) {
            if (growth < species.trunkMaxGrowth()) {
                return false;
            }
            // 顶端且上方是空气：顶花苞还没放下，还需 tick
            BlockPos above = pos.up();
            boolean top = !(world.getBlockState(above).getBlock() instanceof MoranBranchBlock);
            if (top && world.getBlockState(above).isAir()) {
                return false;
            }
            return treeTopped(world, pos);
        }
        return growth >= species.branchMaxGrowth();
    }

    /** 根部位置：沿同柱向下找第一个非枝干方块（土壤在其下方） */
    public static BlockPos rootPos(WorldView world, BlockPos pos, int maxDepth) {
        BlockPos p = pos;
        for (int i = 0; i < maxDepth; i++) {
            BlockState below = world.getBlockState(p.down());
            if (below.getBlock() instanceof MoranBranchBlock || below.isAir()) {
                p = p.down();
                continue;
            }
            break;
        }
        return p;
    }

    /** 向光性近似（开口度启发式）：各水平方向数 2 格内空气 + 上方开口，加权随机 */
    private Direction phototropicDirection(ServerWorld world, BlockPos pos, Random random) {
        int totalWeight = 0;
        int[] weights = new int[TreeSpecies.HORIZONTALS.length];
        for (int i = 0; i < TreeSpecies.HORIZONTALS.length; i++) {
            Direction d = TreeSpecies.HORIZONTALS[i];
            int open = 0;
            for (int step = 1; step <= 2; step++) {
                if (world.getBlockState(pos.offset(d, step)).isAir()) {
                    open++;
                }
            }
            if (world.getBlockState(pos.offset(d).up()).isAir()) {
                open++;
            }
            weights[i] = 1 + open;
            totalWeight += weights[i];
        }
        int roll = random.nextInt(totalWeight);
        for (int i = 0; i < TreeSpecies.HORIZONTALS.length; i++) {
            roll -= weights[i];
            if (roll < 0) {
                return TreeSpecies.HORIZONTALS[i];
            }
        }
        return TreeSpecies.HORIZONTALS[0];
    }

    /** 空间竞争：目标位置周围实心邻居达到阈值即压抑萌芽（密林瘦高、孤树开张的涌现来源） */
    private static boolean isCrowded(ServerWorld world, BlockPos target) {
        int solid = 0;
        for (Direction d : Direction.values()) {
            if (!world.getBlockState(target.offset(d)).isAir()) {
                solid++;
            }
        }
        return solid >= 5;
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
        int height = heightBelow(world, pos, species.biologicalTopMax()) + 1;
        int total = height + trunkSegmentsAbove(world, pos);
        return species.isBudPosition(world, pos, height, total);
    }

    private int trunkSegmentsAbove(ServerWorld world, BlockPos pos) {
        int n = 0;
        BlockPos p = pos.up();
        while (n < species.biologicalTopMax()) {
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
                    world.scheduleBlockTick(n, s.getBlock(),
                            s.get(NATURAL) ? NATURAL_INTERVAL : TreeSpecies.REQUEST_INTERVAL);
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
