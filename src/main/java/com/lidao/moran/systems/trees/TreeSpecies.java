package com.lidao.moran.systems.trees;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.LightType;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.WorldView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
 * 激素模型：物种偏好的生理学载体是激素（生长素/细胞分裂素/赤霉素）。
 * 相似性的单位是<b>表型原型</b>（{@link Phenotype}）：原型 = 一套把拮抗关系
 * 配好的激素档案，同原型的树趋向一致、树形相似；原型间的配比差异就是
 * 文献里测到的激素趋向（直立/垂枝是离散遗传性状，不是连续散点）。
 * 个体只在原型内做小幅残差抖动（推导式状态，零存储）。
 * 见 {@link HormoneProfile} 与 {@link #hormones(WorldView, BlockPos, Direction)}。
 *
 * 现实物候由各档案的钩子表达：
 * - 桃（默认范式）：上半部萌芽、先营养后生殖、先花后叶、耐旱怕涝；
 * - 垂柳：覆写 {@link #branchChildDirection} 枝条渐下垂、{@link #checkEnvironment} 要求水度；
 * - 劲松：覆写 {@link #isBudPosition} 轮生枝、{@link #branchForkGain} 只认水平向、放宽温度下限；
 * - 寒梅：覆写 {@link #onBranchStop} 贴枝开花、耐寒；
 * - 银杏：调低 growChance、稀疏侧芽。
 *
 * <b>分叉的两个维度分属两处，别搞混</b>：
 * 「往哪长」是物种偏好，走 {@link #branchForkGain}（可覆写）；
 * 「能长几根」是离根的代价，走 {@link #forkCapacity}（营养的函数）。
 * 引擎只负责把两者拼起来，不含任何树种偏好。
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
    private final int maxBuds;
    private final int budChanceDenom;
    private final float branchStopChance;
    private final float subBranchChance;
    // —— 分叉参数（多侧枝；成熟度门槛相对本树 max，见 treeMaxGrowth） ——
    /** 距主干至少几节才分叉（贴干首节不分） */
    private final int forkMinChainPos;
    /** 空间冷却：分叉后隔几节才允许再分（密度主旋钮） */
    private final int forkSpacing;
    /** 概率衰减：本节每多一根子枝，概率乘这个数 */
    private final float forkDecay;
    /** 分叉的最低收益：候选方向光照收益低于它就干脆不长（大多数方向不长叉的原因） */
    private final int forkMinGain;
    // —— 激素：常态原型（Builder 未声明任何原型时的唯一档案；1.0 = 中性） ——
    private final float auxinNorm;
    private final float cytokininNorm;
    private final float gibberellinNorm;
    /** 表型原型（至少一条）：相似性的单位，见 {@link Phenotype} */
    private final List<Phenotype> phenotypes;
    private final double phenotypeWeightTotal;
    /** 原型内个体残差幅度：默认 0.05（±5%）—— 大差异归原型，残差只管个体微调 */
    private final float hormoneVariation;
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
    // —— 外观（模型头部 textures 段；由模型生成器读取，树种自己提交） ——
    /** 树皮：贴在长条侧面 */
    private final String barkTexture;
    /** 截断面：贴轴端面（正方形面；水平/竖直枝通用——年轮贴图只属于 trunk=true 的手作主干件） */
    private final String capTexture;
    // —— 方块引用（注册后绑定） ——
    private Block branchBlock;
    private Block budBlock;
    private Block leavesBlock;

    protected TreeSpecies(Builder b) {
        this.id = b.id;
        this.biologicalTopMin = b.biologicalTopMin;
        this.biologicalTopMax = b.biologicalTopMax;
        this.trunkMaxGrowth = b.trunkMaxGrowth;
        this.maxBuds = b.maxBuds;
        this.budChanceDenom = b.budChanceDenom;
        this.branchStopChance = b.branchStopChance;
        this.subBranchChance = b.subBranchChance;
        this.forkMinChainPos = b.forkMinChainPos;
        this.forkSpacing = b.forkSpacing;
        this.forkDecay = b.forkDecay;
        this.forkMinGain = b.forkMinGain;
        this.auxinNorm = b.auxinNorm;
        this.cytokininNorm = b.cytokininNorm;
        this.gibberellinNorm = b.gibberellinNorm;
        List<Phenotype> ph = new ArrayList<>(b.phenotypes);
        if (ph.isEmpty()) {
            ph.add(new Phenotype("default", 1F, new HormoneProfile(
                    clampHormone(auxinNorm), clampHormone(cytokininNorm), clampHormone(gibberellinNorm))));
        }
        this.phenotypes = List.copyOf(ph);
        double weightTotal = 0;
        for (Phenotype p : ph) {
            weightTotal += p.weight();
        }
        this.phenotypeWeightTotal = weightTotal;
        this.hormoneVariation = b.hormoneVariation;
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
        this.barkTexture = b.barkTexture;
        this.capTexture = b.capTexture;
        if (this.barkTexture == null || this.capTexture == null) {
            throw new IllegalStateException("树种 " + this.id + " 没有提交贴图：Builder.textures(bark, cap)");
        }
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

    /**
     * 本树成熟档位（「成熟标志成熟度」max）—— <b>档位体系全部相对它展开（max-i），
     * 不是每棵树都要长满 8 档</b>。
     *
     * <p>由树基坐标确定性推出（与激素档案同一套路数：位置=基因，零存储零状态）：
     * 在 biologicalTop 区间内掷出 —— 桃 6-8 → max ∈ [6,8]，即主干成熟度 6-8 档。
     * trunkMaxGrowth 退为物种天花板。
     *
     * <p><b>档位（粗细）与格数（高度）是两回事，别再混用</b>：max 只定粗细与档位门槛，
     * 高度由 {@link #trunkHalfHeight(int)}（max/2）决定 —— 桃 = 主干 3-4 格的矮壮树形
     * （整树 6-8 格 = 干 max/2 + 冠层）。早先拿 max 当格数用，得到的是
     * 「3-4 档的细杆 + 撑不起来的冠层」，与参照图的粗壮矮干不符。
     *
     * <p>语义变化（相对旧 TARGET 评估）：环境不再直接决定最终高度/档位，
     * 改为通过生长速度（水度/土壤因子）与存亡（硬门槛）起作用；
     * 个体高低差异由树基种子承担，与激素抖动同一哲学。
     *
     * <p>种子混入第二常数，与激素档案的随机流分离 —— 两份档案互不相关。
     */
    public int treeMaxGrowth(long treeSeed) {
        long seed = treeSeed * 0x9E3779B97F4A7C15L;
        seed ^= seed >>> 32;
        seed ^= 0x6A09E667F3BCC909L;
        Random r = Random.create(seed);
        int lo = Math.max(3, biologicalTopMin);
        int hi = Math.max(lo, Math.min(trunkMaxGrowth, biologicalTopMax));
        return lo + r.nextInt(hi - lo + 1);
    }

    /** 主干格数（半高封顶）：整树高 = 2 × 干高，桃 = 3-4 格干 + 冠层 = 6-8 格 */
    public int trunkHalfHeight(int max) {
        return Math.max(2, max / 2);
    }

    public int maxBuds() {
        return maxBuds;
    }

    public int budChanceDenom() {
        return budChanceDenom;
    }

    public float branchStopChance() {
        return branchStopChance;
    }

    public float subBranchChance() {
        return subBranchChance;
    }

    /** 分叉概率衰减：本节每多一根子枝，概率乘这个数（密度副旋钮） */
    public float forkDecay() {
        return forkDecay;
    }

    public int forkMinChainPos() {
        return forkMinChainPos;
    }

    public int forkSpacing() {
        return forkSpacing;
    }

    public int forkMinGain() {
        return forkMinGain;
    }

    // ===== 激素模型 =====

    /**
     * 本树的激素档案：先按权重抽定<b>表型原型</b>（同原型的树趋向一致、树形相似），
     * 再在原型内掷小幅个体残差。
     *
     * <p>全部由树基坐标（{@code MoranBranchBlock.treeOrigin}）确定性推出——
     * 同一棵树的每个节算出同一份档案，跨重启不变，不占任何存储。
     * 「相似性有共同原因、趋向随原型走」由此落地。
     */
    public HormoneProfile hormones(WorldView world, BlockPos pos, Direction facing) {
        if (hormoneVariation <= 0F && phenotypes.size() == 1) {
            return phenotypes.get(0).hormones();   // 快路径：单原型无残差，不必走树基
        }
        BlockPos origin = com.lidao.moran.systems.blocks.MoranBranchBlock.treeOrigin(world, pos, facing);
        return hormoneProfileForSeed(origin.asLong());
    }

    /**
     * 由树种子直接推导激素档案（纯函数，不碰世界）—— 游戏内种子 = 树基坐标 asLong。
     *
     * <p>抽出来成纯函数是给<b>离线树形模拟器</b>（tools.TreeSimulator）用的：
     * 游戏与模拟器共用这同一条代码路径，保证「游戏里某位置的树」与
     * 「模拟器同 seed 的树」激素配比完全一致，模拟结果可以反向指导调参。
     */
    public HormoneProfile hormoneProfileForSeed(long treeSeed) {
        return seededHormones(treeSeed).hormones();
    }

    /** 同 {@link #hormoneProfileForSeed}，但连同抽中的<b>原型</b>一起返回（id 供标注） */
    public Phenotype phenotypeForSeed(long treeSeed) {
        return seededHormones(treeSeed).phenotype();
    }

    public SeededHormones seededHormones(long treeSeed) {
        long seed = treeSeed * 0x9E3779B97F4A7C15L;   // 黄金比例常数搅散，相邻树不相关
        seed ^= seed >>> 32;
        Random r = Random.create(seed);
        Phenotype p = pickPhenotype(r);
        if (hormoneVariation <= 0F) {
            return new SeededHormones(p, p.hormones());
        }
        return new SeededHormones(p, new HormoneProfile(
                rollHormone(r, p.hormones().auxin()),
                rollHormone(r, p.hormones().cytokinin()),
                rollHormone(r, p.hormones().gibberellin())));
    }

    /** 种子 → (原型, 最终档案) 的配对载体，供模拟器标注用 */
    public record SeededHormones(Phenotype phenotype, HormoneProfile hormones) {
    }

    /** 按权重从原型中确定性抽取（权重和已在建档时算好） */
    private Phenotype pickPhenotype(Random r) {
        if (phenotypes.size() == 1) {
            return phenotypes.get(0);
        }
        double roll = r.nextDouble() * phenotypeWeightTotal;
        for (Phenotype p : phenotypes) {
            roll -= p.weight();
            if (roll < 0) {
                return p;
            }
        }
        return phenotypes.get(phenotypes.size() - 1);
    }

    /** 原型内个体残差：原型配比 × (1 ± 残差)，夹在 [0.4, 2.5] 内防极端档案 */
    private float rollHormone(Random r, float norm) {
        float v = norm * (1F + hormoneVariation * (2F * r.nextFloat() - 1F));
        return Math.max(0.4F, Math.min(2.5F, v));
    }

    private static float clampHormone(float v) {
        return Math.max(0.4F, Math.min(2.5F, v));
    }

    /** 分叉额度上限 —— SUBMASK 是 4 位掩码，最多 4 根 */
    public static final int MAX_FORKS = 4;

    /**
     * 这一节最多能养出几根子枝，由营养决定（<b>相对本树 max 的 max-i 制</b>）。
     *
     * 营养从根部发起、沿结构递减（同链每节 -1，换向分叉 -2），所以越靠梢越细弱、
     * 能养的侧枝越少——这条规律不需要额外参数，营养本身就是它。
     * 阈值相对本树成熟档位 max：满营养 3 根、亏 2 档 2 根、亏 3 档 1 根——
     * max=8 时即旧的 8/6/5 绝对阈值，矮小的树（max 小）额度整体收缩。
     * 树种可覆写（比如垂柳更爱分叉、劲松轮生枝）。
     */
    public int forkCapacity(int nutrition, int max) {
        if (nutrition >= max) return 3;
        if (nutrition >= max - 2) return 2;
        if (nutrition >= max - 3) return 1;
        return 0;
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

    /** 树皮贴图 id（贴长条侧面）。模型生成器把它写进模型头部的 textures 段 */
    public String barkTexture() {
        return barkTexture;
    }

    /** 截断面贴图 id（贴轴端面；年轮贴图只属于 trunk=true 的手作主干件） */
    public String capTexture() {
        return capTexture;
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

    /**
     * 该主干方块是否处于可萌发侧芽的位置。
     *
     * 桃范式（参照现实照片定标）：侧枝<b>只在主干尽头定干</b>——主干半高封顶后
     * 顶端四向抽主枝（见 MoranBranchBlock 的定干分叉），中下部一律不萌。
     * 故默认实现恒 false：抽高期侧芽机制整体关闭，侧枝完全由定干承担。
     * 需要逐节侧芽的树形（灌丛状）覆写本钩子，例如 {@code height * 4 > targetHeight}。
     */
    public boolean isBudPosition(ServerWorld world, BlockPos pos, int height, int targetHeight) {
        return false;
    }

    /** 侧枝延伸时子枝的方向。默认：直线延伸（垂柳覆写为渐下垂） */
    public Direction branchChildDirection(ServerWorld world, BlockPos pos, Direction facing) {
        return facing;
    }

    /**
     * 开口度：候选方向 2 格内空气数 + 该方向斜上方是否开阔（0~3）。
     *
     * 这是「光照进不进得来」的廉价近似 —— 比起真实光照值，它不依赖光照更新，
     * 在树自己还在长、上方尚未定型时也成立。
     * 向光萌发与分叉收益共用这一个度量，避免同一个概念在两处各写一遍。
     */
    public static int opennessAround(ServerWorld world, BlockPos pos, Direction dir) {
        int open = 0;
        for (int step = 1; step <= 2; step++) {
            if (world.getBlockState(pos.offset(dir, step)).isAir()) {
                open++;
            }
        }
        if (world.getBlockState(pos.offset(dir).up()).isAir()) {
            open++;
        }
        return open;
    }

    /**
     * 分叉候选方向的「收益」—— <b>物种偏好，不是引擎机制</b>。
     *
     * 引擎拿它决定「往哪长」：收益越高越容易被选中，低于
     * {@code forkMinGain × auxin} 的干脆不长（顶端优势抬门槛）。
     * 与「能长几根」分工不同 —— 那个由营养承担（离根的代价：同链 -1、换向 -2）。
     *
     * 默认实现 = 桃的范式：往光更好的地方长，向顶性强度跟着生长素走，
     * 向上发散权重高（auxin = 1.0 时向上 +5 / 向下 -6）——
     * 冠层靠主枝上的上生子枝层层抬起，构成向上发散的伞面（参照现实桃树照片定标）。
     * 覆写示例：垂柳偏向斜下（枝条渐下垂）、劲松只认水平四向（轮生枝）、
     * 阴性树种把 {@code opennessAround} 的权重调低甚至取负。
     */
    public int branchForkGain(ServerWorld world, BlockPos pos, Direction facing, Direction candidate, HormoneProfile hormones) {
        int gain = opennessAround(world, pos, candidate) * 2;
        int upBias = Math.round(5F * hormones.auxin());
        if (candidate == Direction.UP) {
            gain += upBias;
        } else if (candidate == Direction.DOWN) {
            gain -= upBias + 1;
        }
        return gain;
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
        private int maxBuds = 4;
        private int budChanceDenom = 8;
        private float branchStopChance = 0.25F;
        private float subBranchChance = 0.25F;
        private int forkMinChainPos = 2;
        private int forkSpacing = 2;
        private float forkDecay = 0.5F;
        private int forkMinGain = 4;
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
        // 激素：默认单一常态原型（1.0 = 中性）—— 桃树范式即中性基调；
        // 树种用 phenotype(...) 声明离散亚型后，差异由原型承担，残差只做微调
        private float auxinNorm = 1F;
        private float cytokininNorm = 1F;
        private float gibberellinNorm = 1F;
        private final List<Phenotype> phenotypes = new ArrayList<>();
        private float hormoneVariation = 0.05F;
        // 外观：没有默认值 —— 必须显式提交，否则模型会引用空贴图（渲染成紫黑格）
        private String barkTexture;
        private String capTexture;

        private Builder(String id) {
            this.id = id;
        }

        /** 生物顶端高度区间：主干在 min~max 之间掷封顶骰，同一片林子天然高矮不一 */
        public Builder biologicalTop(int min, int max) { this.biologicalTopMin = min; this.biologicalTopMax = max; return this; }
        public Builder trunkMaxGrowth(int v) { this.trunkMaxGrowth = v; return this; }
        public Builder maxBuds(int v) { this.maxBuds = v; return this; }
        public Builder budChanceDenom(int v) { this.budChanceDenom = v; return this; }
        public Builder branchStopChance(float v) { this.branchStopChance = v; return this; }
        public Builder subBranchChance(float v) { this.subBranchChance = v; return this; }
        public Builder forkMinChainPos(int v) { this.forkMinChainPos = v; return this; }
        /** 空间冷却：分叉后隔几节才能再分。1 = 允许相邻节都分（最密），2 = 隔一节 */
        public Builder forkSpacing(int v) { this.forkSpacing = v; return this; }
        /** 概率衰减：本节每多一根子枝，概率乘这个数 */
        public Builder forkDecay(float v) { this.forkDecay = v; return this; }
        /** 分叉最低光照收益：调高 = 只在光很好的地方分叉（树更稀疏通透） */
        public Builder forkMinGain(int v) { this.forkMinGain = v; return this; }
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

        /**
         * 默认原型的激素常态：生长素（顶端优势）/ 细胞分裂素（促分支）/ 赤霉素（伸长）。
         * 1.0 = 中性。声明了 {@link #phenotype} 后它退为兜底档案，可不再调。
         */
        public Builder hormones(float auxin, float cytokinin, float gibberellin) {
            this.auxinNorm = auxin;
            this.cytokininNorm = cytokinin;
            this.gibberellinNorm = gibberellin;
            return this;
        }

        /**
         * 声明一个<b>表型原型</b>（同种树的离散亚型，相似性的单位，见 {@link Phenotype}）。
         *
         * @param id          原型名（调试与未来的形状覆写用）
         * @param weight      选中权重（如垂枝桃论文 F2 的 3:1 分离比就写成 3 和 1）
         * @param auxin       生长素（顶端优势强度）
         * @param cytokinin   细胞分裂素（促分支）
         * @param gibberellin 赤霉素（伸长）
         */
        public Builder phenotype(String id, float weight, float auxin, float cytokinin, float gibberellin) {
            this.phenotypes.add(new Phenotype(id, weight,
                    new HormoneProfile(clampHormone(auxin), clampHormone(cytokinin), clampHormone(gibberellin))));
            return this;
        }

        /** 原型内个体残差幅度：0 = 原型内完全一致；默认 0.05（±5%，只管个体微调） */
        public Builder hormoneVariation(float v) { this.hormoneVariation = v; return this; }

        /**
         * 树种提交的贴图对（对应模型头部的 textures 段）。新增树种必须填。
         *
         * @param bark 树皮，贴长条侧面
         * @param cap  截断面，贴轴端面（正方形面；水平/竖直枝通用）
         */
        public Builder textures(String bark, String cap) {
            this.barkTexture = bark;
            this.capTexture = cap;
            return this;
        }

        public TreeSpecies build() {
            return new TreeSpecies(this);
        }
    }
}
