package com.lidao.moran.systems.trees;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.LightType;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.WorldView;

import java.util.HashMap;
import java.util.Map;

/**
 * 树种档案——生长库的核心抽象（v2：环境参数系统）。
 *
 * 一份档案描述一种树的生长偏好与形态：数值参数 + 少量策略钩子。
 * 生长引擎（{@link com.lidao.moran.systems.blocks.MoranBranchBlock} 与
 * {@link com.lidao.moran.systems.blocks.MoranFlowerBudBlock}）对所有树种共用，
 * 新增树种 = 写一份档案 + 注册两个方块实例，零引擎改动。
 *
 * 表现型 = 基因型（本档案）× 环境：
 * - 水度：半径 4 格内最近水源距离映射 0-8（同原版农田灌溉尺度），
 *   低于 minHydration 直接算环境失败（水生树种用），同时作为生长速度软增益；
 * - 湿度：群系 downfall（自建映射表，原版群系回退 hasPrecipitation 二值近似，
 *   1.20.1 API 已移除 downfall 数值读取）；
 * - 土壤：SoilProfile 三轴（排水/气密/保水），树种声明偏好区间，出区扣生长倍率；
 * - 高度：biologicalTopMin~Max 区间内掷封顶骰，同林树木天然高矮不一。
 *
 * 现实物候由各档案的钩子表达：
 * - 桃（默认范式）：上半部萌芽、先营养后生殖、先花后叶、耐旱怕涝；
 * - 垂柳：覆写 {@link #branchChildDirection} 枝条渐下垂、{@link #checkEnvironment} 要求水度；
 * - 劲松：覆写 {@link #isBudPosition} 轮生枝、放宽温度下限；
 * - 寒梅：覆写 {@link #onBranchStop} 贴枝开花、耐寒；
 * - 银杏：调低 growChance、稀疏侧芽。
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

    /** 群系湿度映射表（桃花源群系取各自 biome JSON 的 downfall） */
    private static final Map<String, Float> BIOME_HUMIDITY = new HashMap<>();
    static {
        BIOME_HUMIDITY.put("moran_mod:yaozhuohuayuan", 0.3F);
        BIOME_HUMIDITY.put("moran_mod:peach_valley", 0.5F);
        BIOME_HUMIDITY.put("moran_mod:bamboo_grove", 0.6F);
        BIOME_HUMIDITY.put("moran_mod:farm_plains", 0.4F);
        BIOME_HUMIDITY.put("moran_mod:green_hills", 0.5F);
        BIOME_HUMIDITY.put("moran_mod:blossom_stream", 0.6F);
        BIOME_HUMIDITY.put("moran_mod:mirror_lakes", 0.7F);
        BIOME_HUMIDITY.put("moran_mod:hidden_depths", 0.4F);
    }

    private final String id;
    // —— 结构参数 ——
    private final int biologicalTopMin;
    private final int biologicalTopMax;
    private final int trunkMaxGrowth;
    private final int topBudGrowth;
    private final int maxBuds;
    private final int budChanceDenom;
    private final int branchMaxGrowth;
    private final int branchStopGrowth;
    private final float branchStopChance;
    private final float subBranchChance;
    // —— 环境参数 ——
    private final float growChance;
    private final int minLight;
    private final float minTemperature;
    private final float maxTemperature;
    private final int minHydration;
    private final float minHumidity;
    private final int[] drainageRange;
    private final int[] aerationRange;
    private final int[] retentionRange;
    // —— 交互参数 ——
    private final float pruneResponseChance;
    private final int minSpacing;
    private final int branchNutritionDecay;
    private final int chainNutritionDecay;
    // —— 方块引用（注册后绑定） ——
    private Block branchBlock;
    private Block budBlock;
    private Block leavesBlock;

    protected TreeSpecies(Builder b) {
        this.id = b.id;
        this.biologicalTopMin = b.biologicalTopMin;
        this.biologicalTopMax = b.biologicalTopMax;
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
        this.minHydration = b.minHydration;
        this.minHumidity = b.minHumidity;
        this.drainageRange = b.drainageRange;
        this.aerationRange = b.aerationRange;
        this.retentionRange = b.retentionRange;
        this.pruneResponseChance = b.pruneResponseChance;
        this.minSpacing = b.minSpacing;
        this.branchNutritionDecay = b.branchNutritionDecay;
        this.chainNutritionDecay = b.chainNutritionDecay;
    }

    public String id() {
        return id;
    }

    public int biologicalTopMin() {
        return biologicalTopMin;
    }

    public int biologicalTopMax() {
        return biologicalTopMax;
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

    public float pruneResponseChance() {
        return pruneResponseChance;
    }

    /** 最小树干间距（水平切比雪夫距离）：世界生成放置种子时的安全距离 */
    public int minSpacing() {
        return minSpacing;
    }

    /** 养分分叉损耗：每分出一级枝，养分减损（分流损失，>距离损耗） */
    public int branchNutritionDecay() {
        return branchNutritionDecay;
    }

    /** 养分距离损耗：同链每延伸一节，养分减损 */
    public int chainNutritionDecay() {
        return chainNutritionDecay;
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

    // ===== 环境参数系统 =====

    /** 水度：半径 4 格内最近水源映射 0-8（每格距离扣 2，同原版农田灌溉尺度） */
    public static int hydration(WorldView world, BlockPos pos) {
        for (int d = 0; d <= 4; d++) {
            for (int dx = -d; dx <= d; dx++) {
                for (int dz = -d; dz <= d; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != d) {
                        continue; // 只扫当前距离环，最近水源优先
                    }
                    for (int dy = 0; dy >= -1; dy--) {
                        if (world.getBlockState(pos.add(dx, dy, dz)).isOf(Blocks.WATER)) {
                            return Math.max(0, 8 - 2 * d);
                        }
                    }
                }
            }
        }
        return 0;
    }

    /** 群系湿度：桃花源群系查表，其余回退「是否有降水」二值近似 */
    public static float biomeHumidity(ServerWorld world, BlockPos pos) {
        String id = world.getBiome(pos).getKey()
                .map(k -> k.getValue().toString())
                .orElse("");
        Float v = BIOME_HUMIDITY.get(id);
        if (v != null) {
            return v;
        }
        return world.getBiome(pos).value().hasPrecipitation() ? 0.6F : 0.15F;
    }

    // —— 环境评分阈值（每项达标记 1 分，目标高度 = min + 分数） ——
    /** 天空光存储值达标线（露天 15，浓荫 <11） */
    private static final int SCORE_SKY_LIGHT = 11;
    /** 水度达标线（4/8 = 2 格内有水） */
    private static final int SCORE_HYDRATION = 4;
    /** 温度适宜区间 */
    private static final float SCORE_TEMP_MIN = 0.5F;
    private static final float SCORE_TEMP_MAX = 0.95F;

    /**
     * 生长前一次性环境评估 → 目标高度 = biologicalTopMin + 得分数（0-4）。
     * 光照（露天）/ 水分（近水）/ 温度（适宜）/ 土壤（三轴全落偏好区间）各占一分。
     * 评估在根部进行，结果持久化为方块 target 属性并随生长继承——
     * 随机生长模式下每棵树的最终高度在生长期始即已确定。
     */
    public int evaluateTargetHeight(net.minecraft.world.WorldView world, BlockPos root) {
        int score = 0;
        if (world.getLightLevel(LightType.SKY, root.up()) >= SCORE_SKY_LIGHT) {
            score++;
        }
        if (hydration(world, root) >= SCORE_HYDRATION) {
            score++;
        }
        float temp = world.getBiome(root).value().getTemperature();
        if (temp >= SCORE_TEMP_MIN && temp <= SCORE_TEMP_MAX) {
            score++;
        }
        if (soilFactor(Soils.of(world, root.down())) >= 1.0F) {
            score++;
        }
        return Math.min(biologicalTopMax, biologicalTopMin + score);
    }

    /** 水度生长因子：干土 0.6 倍速 → 饱和 1.0 倍速 */
    public float hydrationFactor(int hydration) {
        return 0.6F + hydration * 0.05F;
    }

    /** 单轴土壤因子：在偏好区间内 1.0，偏离每格扣 0.15 */
    private static float axisFactor(int value, int[] range) {
        if (value >= range[0] && value <= range[1]) {
            return 1.0F;
        }
        int dist = value < range[0] ? range[0] - value : value - range[1];
        return Math.max(0.0F, 1.0F - dist * 0.15F);
    }

    /** 土壤生长因子：三轴乘法累积，下限 0.3 */
    public float soilFactor(SoilProfile soil) {
        float f = axisFactor(soil.drainage(), drainageRange)
                * axisFactor(soil.aeration(), aerationRange)
                * axisFactor(soil.retention(), retentionRange);
        return Math.max(0.3F, f);
    }

    /** 有效生长概率 = 基础 × 水度因子 × 土壤因子 */
    public float effectiveGrowChance(ServerWorld world, BlockPos pos, int hydration, SoilProfile soil) {
        return growChance * hydrationFactor(hydration) * soilFactor(soil);
    }

    // ===== 策略钩子（默认实现 = 桃树范式，树种按需覆写） =====

    /**
     * 生长环境检测：光照、温度、水度硬门槛（水生树种用）、群系湿度硬门槛。
     * 垂柳覆写时叠加近水要求。
     */
    public boolean checkEnvironment(ServerWorld world, BlockPos pos, int hydration) {
        // 天空光存储值与时间无关（夜间 15 仍是 15），避免夜间加载区块的野生树被误冻结
        int light = Math.max(world.getLightLevel(LightType.SKY, pos.up()),
                world.getLightLevel(LightType.BLOCK, pos.up()));
        if (light < minLight) {
            return false;
        }
        float temperature = world.getBiome(pos).value().getTemperature();
        if (temperature < minTemperature || temperature > maxTemperature) {
            return false;
        }
        if (hydration < minHydration) {
            return false;
        }
        return biomeHumidity(world, pos) >= minHumidity;
    }

    /** 该主干方块是否处于可萌发侧芽的位置。默认：预定目标高度的上半部分（现实：主枝自上部萌发） */
    public boolean isBudPosition(ServerWorld world, BlockPos pos, int height, int targetHeight) {
        return height > targetHeight / 2;
    }

    /** 侧枝延伸时子枝的方向。默认：直线延伸（垂柳覆写为渐下垂） */
    public Direction branchChildDirection(ServerWorld world, BlockPos pos, Direction facing) {
        return facing;
    }

    /**
     * 侧枝停止生长时的开花方式。中性默认：仅末端生成一颗花苞。
     * 树种覆写定制花芽着生方式（桃树范式见 PeachSpecies：末端与四周）。
     */
    public void onBranchStop(ServerWorld world, BlockPos pos, Direction facing, Random random, boolean natural) {
        placeBudIfAir(world, pos.offset(facing), facing, natural);
    }

    /**
     * 花苞完全成熟。中性默认：化为本树种树叶。
     * 树种覆写定制成熟形态（桃树范式见 PeachSpecies：先花后叶 + 顶部带冠）。
     */
    public void onBudMature(ServerWorld world, BlockPos pos, Direction facing, Random random) {
        world.setBlockState(pos, leavesBlock().getDefaultState(), Block.NOTIFY_ALL);
    }

    protected void placeBudIfAir(ServerWorld world, BlockPos pos, Direction facing, boolean natural) {
        if (world.getBlockState(pos).isAir()) {
            world.setBlockState(pos, budBlock.getDefaultState()
                    .with(com.lidao.moran.systems.blocks.MoranFlowerBudBlock.FACING, facing)
                    .with(com.lidao.moran.systems.blocks.MoranFlowerBudBlock.NATURAL, natural), Block.NOTIFY_ALL);
        }
    }

    protected void placeLeafIfAir(ServerWorld world, BlockPos pos) {
        if (world.getBlockState(pos).isAir()) {
            world.setBlockState(pos, leavesBlock().getDefaultState(), Block.NOTIFY_ALL);
        }
    }

    // ===== Builder =====

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static class Builder {
        private final String id;
        private int biologicalTopMin = 8;
        private int biologicalTopMax = 12;
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
        private int minHydration = 0;
        private float minHumidity = 0.0F;
        private int[] drainageRange = {4, 8};
        private int[] aerationRange = {3, 8};
        private int[] retentionRange = {2, 6};
        private float pruneResponseChance = 0.5F;
        private int minSpacing = 4;
        private int branchNutritionDecay = 2;
        private int chainNutritionDecay = 1;

        private Builder(String id) {
            this.id = id;
        }

        /** 生物顶端高度区间：主干在 min~max 之间掷封顶骰，同一片林子天然高矮不一 */
        public Builder biologicalTop(int min, int max) { this.biologicalTopMin = min; this.biologicalTopMax = max; return this; }
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
        /** 最低水度（硬条件）：水生/喜水树种设 >0 */
        public Builder minHydration(int v) { this.minHydration = v; return this; }
        /** 最低群系湿度（硬条件） */
        public Builder minHumidity(float v) { this.minHumidity = v; return this; }
        /** 土壤偏好区间：排水/气密/保水 各 min,max */
        public Builder soilPreference(int dMin, int dMax, int aMin, int aMax, int rMin, int rMax) {
            this.drainageRange = new int[]{dMin, dMax};
            this.aerationRange = new int[]{aMin, aMax};
            this.retentionRange = new int[]{rMin, rMax};
            return this;
        }
        /** 修剪响应：断口相邻枝干萌发新芽的概率 */
        public Builder pruneResponseChance(float v) { this.pruneResponseChance = v; return this; }
        /** 最小树干间距：世界生成种子放置的安全距离（水平切比雪夫距离） */
        public Builder minSpacing(int v) { this.minSpacing = v; return this; }
        /** 养分分叉损耗（每分一级枝） */
        public Builder branchNutritionDecay(int v) { this.branchNutritionDecay = v; return this; }
        /** 养分距离损耗（同链每节） */
        public Builder chainNutritionDecay(int v) { this.chainNutritionDecay = v; return this; }

        public TreeSpecies build() {
            return new TreeSpecies(this);
        }
    }
}
