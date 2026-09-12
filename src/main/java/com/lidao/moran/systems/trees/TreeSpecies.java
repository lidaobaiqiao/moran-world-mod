package com.lidao.moran.systems.trees;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

/**
 * 树种档案——生长库的核心抽象。
 *
 * 一份档案描述一种树的生长偏好与形态：数值参数 + 少量策略钩子。
 * 生长引擎（{@link com.lidao.moran.systems.blocks.MoranBranchBlock} 与
 * {@link com.lidao.moran.systems.blocks.MoranFlowerBudBlock}）对所有树种共用，
 * 新增树种 = 写一份档案 + 注册两个方块实例，零引擎改动。
 *
 * 现实物候由各档案的钩子表达：
 * - 桃（默认范式）：上半部萌芽、先营养后生殖、先花后叶；
 * - 垂柳：覆写 {@link #branchChildDirection} 实现枝条渐下垂、覆写环境检测要求近水；
 * - 劲松：覆写 {@link #isBudPosition} 实现轮生枝；
 * - 寒梅：覆写 {@link #onBranchStop} 实现贴枝开花、放宽温度下限；
 * - 银杏：调低 {@code growChance} 并稀疏侧芽数值。
 */
public class TreeSpecies {

    // —— 请求-休眠引擎常量（树种无关） ——
    /** 生长请求周期：60 秒 */
    public static final int REQUEST_INTERVAL = 1200;
    /** 休眠时长：24 分钟 */
    public static final int DORMANT_TICKS = 28800;
    /** 连续条件失败次数上限 */
    public static final int MAX_FAILS = 3;
    public static final Direction[] HORIZONTALS = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    private final String id;
    // —— 数值参数 ——
    private final int biologicalTop;
    private final int trunkMaxGrowth;
    private final int topBudGrowth;
    private final int maxBuds;
    private final int budChanceDenom;
    private final int branchMaxGrowth;
    private final int branchStopGrowth;
    private final float branchStopChance;
    private final float subBranchChance;
    private final float growChance;
    private final int minLight;
    private final float minTemperature;
    private final float maxTemperature;
    // —— 方块引用（注册后绑定） ——
    private Block branchBlock;
    private Block budBlock;
    private Block leavesBlock;

    private TreeSpecies(Builder b) {
        this.id = b.id;
        this.biologicalTop = b.biologicalTop;
        this.trunkMaxGrowth = b.trunkMaxGrowth;
        this.topBudGrowth = b.topBudGrowth;
        this.maxBuds = b.maxBuds;
        this.budChanceDenom = b.budChanceDenom;
        this.branchMaxGrowth = b.branchMaxGrowth;
        this.branchStopGrowth = b.branchStopGrowth;
        this.branchStopChance = b.branchStopChance;
        this.subBranchChance = b.subBranchChance;
        this.growChance = b.growChance;
        this.minLight = b.minLight;
        this.minTemperature = b.minTemperature;
        this.maxTemperature = b.maxTemperature;
    }

    public String id() {
        return id;
    }

    public int biologicalTop() {
        return biologicalTop;
    }

    public int trunkMaxGrowth() {
        return trunkMaxGrowth;
    }

    public int topBudGrowth() {
        return topBudGrowth;
    }

    public int maxBuds() {
        return maxBuds;
    }

    public int budChanceDenom() {
        return budChanceDenom;
    }

    public int branchMaxGrowth() {
        return branchMaxGrowth;
    }

    public int branchStopGrowth() {
        return branchStopGrowth;
    }

    public float branchStopChance() {
        return branchStopChance;
    }

    public float subBranchChance() {
        return subBranchChance;
    }

    public float growChance() {
        return growChance;
    }

    /** 注册完成后绑定本树种的三类方块实例 */
    public void bind(Block branch, Block bud, Block leaves) {
        this.branchBlock = branch;
        this.budBlock = bud;
        this.leavesBlock = leaves;
    }

    public Block branchBlock() {
        return branchBlock;
    }

    public Block budBlock() {
        return budBlock;
    }

    public Block leavesBlock() {
        return leavesBlock;
    }

    // ===== 策略钩子（默认实现 = 桃树范式，树种按需覆写） =====

    /** 生长环境检测：光照与温度（垂柳覆写时叠加近水要求） */
    public boolean checkEnvironment(ServerWorld world, BlockPos pos) {
        if (world.getLightLevel(pos.up()) < minLight) {
            return false;
        }
        float temperature = world.getBiome(pos).value().getTemperature();
        return temperature >= minTemperature && temperature <= maxTemperature;
    }

    /** 该主干方块是否处于可萌发侧芽的位置。默认：当前高度的上半部分（现实：主枝自上部萌发） */
    public boolean isBudPosition(ServerWorld world, BlockPos pos, int height, int totalHeight) {
        return height > totalHeight / 2;
    }

    /** 侧枝延伸时子枝的方向。默认：直线延伸（垂柳覆写为渐下垂） */
    public Direction branchChildDirection(ServerWorld world, BlockPos pos, Direction facing) {
        return facing;
    }

    /**
     * 侧枝停止生长时的开花方式。默认（桃树范式）：末端与四周（上方 + 两侧垂直向）生成花苞，
     * 此后不再生侧枝——现实桃树的花芽着生一年生枝顶端与侧腋。
     */
    public void onBranchStop(ServerWorld world, BlockPos pos, Direction facing, Random random) {
        placeBudIfAir(world, pos.offset(facing), facing);
        Direction left = facing.rotateYCounterclockwise();
        Direction right = facing.rotateYClockwise();
        placeBudIfAir(world, pos.up(), Direction.UP);
        placeBudIfAir(world, pos.offset(left), left);
        placeBudIfAir(world, pos.offset(right), right);
    }

    /**
     * 花苞完全成熟（先花后叶物候）。默认：化为本树种树叶；
     * 顶部花苞（facing=up）额外生成小树冠（四向必放 + 四角半数）。
     */
    public void onBudMature(ServerWorld world, BlockPos pos, Direction facing, Random random) {
        world.setBlockState(pos, leavesBlock.getDefaultState(), Block.NOTIFY_ALL);
        if (facing == Direction.UP) {
            for (Direction d : HORIZONTALS) {
                placeLeafIfAir(world, pos.offset(d));
            }
            for (Direction d : HORIZONTALS) {
                if (random.nextBoolean()) {
                    placeLeafIfAir(world, pos.offset(d).up());
                }
            }
        }
    }

    void placeBudIfAir(ServerWorld world, BlockPos pos, Direction facing) {
        if (world.getBlockState(pos).isAir()) {
            world.setBlockState(pos, budBlock.getDefaultState()
                    .with(com.lidao.moran.systems.blocks.MoranFlowerBudBlock.FACING, facing), Block.NOTIFY_ALL);
        }
    }

    private void placeLeafIfAir(ServerWorld world, BlockPos pos) {
        if (world.getBlockState(pos).isAir()) {
            world.setBlockState(pos, leavesBlock.getDefaultState(), Block.NOTIFY_ALL);
        }
    }

    // ===== Builder =====

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static class Builder {
        private final String id;
        private int biologicalTop = 7;
        private int trunkMaxGrowth = 8;
        private int topBudGrowth = 5;
        private int maxBuds = 4;
        private int budChanceDenom = 8;
        private int branchMaxGrowth = 7;
        private int branchStopGrowth = 4;
        private float branchStopChance = 0.25F;
        private float subBranchChance = 0.25F;
        private float growChance = 0.5F;
        private int minLight = 9;
        private float minTemperature = 0.3F;
        private float maxTemperature = 1.2F;

        private Builder(String id) {
            this.id = id;
        }

        public Builder biologicalTop(int v) { this.biologicalTop = v; return this; }
        public Builder trunkMaxGrowth(int v) { this.trunkMaxGrowth = v; return this; }
        public Builder topBudGrowth(int v) { this.topBudGrowth = v; return this; }
        public Builder maxBuds(int v) { this.maxBuds = v; return this; }
        public Builder budChanceDenom(int v) { this.budChanceDenom = v; return this; }
        public Builder branchMaxGrowth(int v) { this.branchMaxGrowth = v; return this; }
        public Builder branchStopGrowth(int v) { this.branchStopGrowth = v; return this; }
        public Builder branchStopChance(float v) { this.branchStopChance = v; return this; }
        public Builder subBranchChance(float v) { this.subBranchChance = v; return this; }
        public Builder growChance(float v) { this.growChance = v; return this; }
        public Builder minLight(int v) { this.minLight = v; return this; }
        public Builder temperature(float min, float max) { this.minTemperature = min; this.maxTemperature = max; return this; }

        public TreeSpecies build() {
            return new TreeSpecies(this);
        }
    }
}
