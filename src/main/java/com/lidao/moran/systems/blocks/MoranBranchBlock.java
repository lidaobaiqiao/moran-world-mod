package com.lidao.moran.systems.blocks;

import com.lidao.moran.systems.trees.HormoneProfile;
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
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

import net.minecraft.registry.RegistryKey;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通用活树枝干方块——生长引擎的树干/侧枝部分，所有树种共用。
 * 行为由 {@link TreeSpecies} 档案驱动，环境参数系统接入：
 * 水度（生长速度软增益 + 硬门槛）、土壤三轴偏好、空间竞争、向光性、修剪响应。
 * 激素模型：每棵树按树基坐标推出自己的激素档案（生长素/细胞分裂素/赤霉素），
 * 调制生长速率、侧芽萌出、枝条延伸与分叉偏好 —— 同种不同形。
 *
 * 掉落按生长度分档（见各树种枝干方块的战利品表）：
 * 1-2 桃源树枝，3-6 粗壮桃源树枝，7-8 粗壮桃源树干。
 */
public class MoranBranchBlock extends Block implements Fertilizable {

    public static final IntProperty GROWTH = IntProperty.of("growth", 1, 8);
    public static final EnumProperty<Direction> FACING = EnumProperty.of("facing", Direction.class,
            List.of(Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH,
                    Direction.WEST, Direction.EAST));
    /**
     * 主干标记——身份与朝向分离。
     *
     * FACING 只描述「朝哪延伸」，不承载身份：上生子枝的 FACING 同样是 UP，
     * 若沿用 facing==UP 判断主干，子枝会冒充主干去抽高/封顶/萌侧芽，树形直接崩。
     * 故身份单独用本属性承载。
     *
     * 默认 true = 主干。主干只有「树苗长成 / 野生种子落地」一个创建入口且用默认状态，
     * 故默认值指向主干时漏设风险最小；侧枝/子枝在四个放置处显式置 false。
     * 旧存档的枝干缺此属性时自动落 true，老树不损。
     */
    public static final BooleanProperty TRUNK = BooleanProperty.of("trunk");
    public static final IntProperty FAILS = IntProperty.of("fails", 0, TreeSpecies.MAX_FAILS);
    /** 休眠侧芽：出现在主干上半部分，主干到顶后苏醒 */
    public static final BooleanProperty DORMANT = BooleanProperty.of("dormant");
    /** 野生模式（世界生成）生长请求周期：0.1 秒 */
    public static final int NATURAL_INTERVAL = 2;
    /** 野生模式（世界生成）：0.1s 一次请求、条件过即立即生长、条件失败直接冻结 */
    public static final BooleanProperty NATURAL = BooleanProperty.of("natural");
    /** 主干封顶标记：到顶决策持久化，唤醒侧芽与定干分叉由此驱动 */
    public static final BooleanProperty TOPPED = BooleanProperty.of("topped");

    /**
     * 本节分出的子枝集合。取代了原先的 BRANCH(布尔) + SUB(单方向)。
     *
     * 为什么需要它：枝是「一格一块」的，分叉点那格在模型上要同时画出
     * 「穿过本格的母枝」和「从母枝侧面伸出的子枝」；单元素模型画不出 T 字形，
     * 分叉点就会缺一根——视觉上就是「侧枝只有一半」。
     * 多侧枝要求「一节能同时分出好几根」，单方向装不下，所以要记一整组。
     *
     * <h2>为什么是枚举而不是 IntProperty 位掩码</h2>
     * MC 的状态哈希是「Σ(属性哈希 XOR 值哈希)」，而 XOR 一个小整数只翻低位。
     * {@code IntProperty} 的值哈希就是那个整数本身（0~15），于是 24.6 万个状态的
     * 哈希全挤成一团——实测哈希种类从 4752 掉到 624，桶里塞几百个状态、退化成
     * 红黑树，方块注册从 28 秒涨到 383 秒。换成枚举后，枚举常量的身份哈希是
     * 大随机数，多样性反而超过原设计（8256 种）。
     *
     * 位序由 {@link #perpendiculars(Direction)} 定义，必须与 blockstate 生成器
     * （.workbuddy/build_bs_final.py 的 perp_for）严格一致，否则模型 id 会对不上。
     */
    public static final EnumProperty<ForkSet> FORK_SET = EnumProperty.of("forkset", ForkSet.class);

    /**
     * 本节分出的子枝集合，枚举。名字里的 F0~F3 是
     * {@link #perpendiculars(Direction)} 里的槽位序号，例如 F013 = 第 0、1、3 槽有子枝。
     *
     * 用枚举而非整数位掩码是<b>为了哈希熵</b>（见 {@link #FORK_SET} 的说明）：
     * MC 的状态哈希取值的身份哈希，枚举常量天然是良分布的大随机数，小整数不是。
     */
    public enum ForkSet implements StringIdentifiable {
        NONE(0),
        F0(1), F1(2), F2(4), F3(8),
        F01(3), F02(5), F03(9), F12(6), F13(10), F23(12),
        F012(7), F013(11), F023(13), F123(14), F0123(15);

        private final int mask;

        ForkSet(int mask) {
            this.mask = mask;
        }

        /** 位掩码：第 i 位 = 第 i 个槽位（{@link #perpendiculars} 的顺序）有子枝 */
        public int mask() {
            return mask;
        }

        private static final ForkSet[] BY_MASK = new ForkSet[(1 << TreeSpecies.MAX_FORKS)];

        static {
            for (ForkSet v : values()) {
                BY_MASK[v.mask] = v;
            }
        }

        /** 掩码 -> 枚举；越界或未定义时落到 NONE */
        public static ForkSet of(int mask) {
            int m = mask & (BY_MASK.length - 1);
            ForkSet v = BY_MASK[m];
            return v == null ? NONE : v;
        }

        public boolean isEmpty() {
            return mask == 0;
        }

        public int count() {
            return Integer.bitCount(mask);
        }

        /** blockstate 里的属性值就是这个名字（MC 的 EnumProperty 要求实现 StringIdentifiable） */
        @Override
        public String asString() {
            return this.name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /**
     * 分叉槽位的规范顺序 —— 必须与 blockstate 生成器逐位一致。
     *
     * <pre>
     *   垂直母枝（上/下生）  ->  [北, 南, 东, 西]
     *   水平母枝            ->  [顺时针 90°, 逆时针 90°, 上, 下]
     * </pre>
     *
     * 注意 rotateYClockwise/Counterclockwise 对 UP/DOWN 会返回自身，
     * 垂直母枝必须走四个水平向，不能直接用这两个方法。
     */
    public static Direction[] perpendiculars(Direction facing) {
        if (facing.getAxis() == Direction.Axis.Y) {
            return new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        }
        return new Direction[]{facing.rotateYClockwise(), facing.rotateYCounterclockwise(),
                               Direction.UP, Direction.DOWN};
    }

    /** 方向 -> 槽位序号（0~3）；该方向不是本母枝的垂直方向时返回 -1 */
    public static int slotOf(Direction facing, Direction side) {
        Direction[] slots = perpendiculars(facing);
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == side) {
                return i;
            }
        }
        return -1;
    }

    /** 槽位序号 -> 方向；越界返回 null */
    public static Direction sideOf(Direction facing, int slot) {
        Direction[] slots = perpendiculars(facing);
        return slot >= 0 && slot < slots.length ? slots[slot] : null;
    }

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
                // 水平枝条截面取扁矩形：竖向半高收窄（<=3），横向保持粗度——
                // 竖向若按主干半宽走，粗枝会渲染成 12 格高的方墩，像竖着的截干
                double w = Math.min(HALF_WIDTH[g], 6);
                double h = Math.min(HALF_WIDTH[g], 3);
                shapes[g] = switch (d) {
                    // 枝条贯穿整格（长度 16）：贴干端接上主干，尖端到方块界面
                    case EAST -> Block.createCuboidShape(0, 8 - h, 8 - w, 16, 8 + h, 8 + w);
                    case WEST -> Block.createCuboidShape(0, 8 - h, 8 - w, 16, 8 + h, 8 + w);
                    case SOUTH -> Block.createCuboidShape(8 - w, 8 - h, 0, 8 + w, 8 + h, 16);
                    case NORTH -> Block.createCuboidShape(8 - w, 8 - h, 0, 8 + w, 8 + h, 16);
                    default -> VoxelShapes.empty();
                };
            }
            BUD_SHAPES.put(d, shapes);
        }
        // 垂直子枝（上生/下生）：形态与主干同为竖直柱，直接复用主干形状数组。
        // 不补这两个方向会留 null，getOutlineShape 一取即崩。
        BUD_SHAPES.put(Direction.UP, TRUNK_SHAPES);
        BUD_SHAPES.put(Direction.DOWN, TRUNK_SHAPES);
    }

    public MoranBranchBlock(TreeSpecies species, Settings settings) {
        super(settings);
        this.species = species;
        setDefaultState(getDefaultState()
                .with(GROWTH, 1)
                .with(FACING, Direction.UP)
                .with(TRUNK, true)
                .with(FAILS, 0)
                .with(DORMANT, false)
                .with(TOPPED, false)
                .with(NATURAL, false)
                .with(FORK_SET, ForkSet.NONE));
    }

    public TreeSpecies species() {
        return species;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(GROWTH, FACING, TRUNK, FAILS, DORMANT, TOPPED, NATURAL, FORK_SET);
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

        // 条件满足：野生树立即生长；种植树掷骰（有效概率 = 基础 × 水度 × 土壤 × 赤霉素）
        // 激素档案：这棵树自己的内分泌（由树基坐标确定性推出，同树同档、零存储）
        HormoneProfile hormones = species.hormones(world, pos, state.get(FACING));
        // 本树成熟档位 max：由树基种子推出（与激素同一树基，独立随机流）。
        // 必须走 treeOrigin 沿链回溯——不能复用上面的 root（rootPos 是土壤取样的
        // 垂直下探，侧枝用它落到自己脚下的地面，每个枝算出各自的 max，
        // 整树档位体系就散了：分叉门槛/开花年龄/成熟上限全部失准，
        // 且与离线模拟器的单树 max 系统性漂移）。
        int max = species.treeMaxGrowth(treeOrigin(world, pos, state.get(FACING)).asLong());
        boolean progressed = false;
        if (natural || random.nextFloat()
                < species.effectiveGrowChance(world, pos, hydration, soil) * hormones.gibberellin()) {
            progressed = growPart(state, world, pos, random, hormones, max);
            if (!(world.getBlockState(pos).getBlock() instanceof MoranBranchBlock)) {
                return;
            }
            if (world.getBlockState(pos).get(FAILS) != 0) {
                world.setBlockState(pos, world.getBlockState(pos).with(FAILS, 0), Block.NOTIFY_ALL);
            }
        }

        if (!natural) {
            // 种植树保持低频常驻（60s 一次，成本可忽略）——玩家会修剪、骨粉，得留着响应
            world.scheduleBlockTick(pos, this, interval);
            return;
        }

        // 野生树完成使命（结构上判定长满）后永久静默，防 tick 风暴
        if (isFullyGrown(world.getBlockState(pos), world, pos)) {
            clearIdle(world, pos);
            return;
        }

        // 生命周期终止：连续 IDLE_LIMIT 次请求一点产出都没有，就认为这一节长完了。
        // 这是「结构完整性」判据兜不住的那些情况的统一出口 —— 养分封顶停在半路的枝、
        // 环境长期凑不够的枝，靠它收场；否则它们会永远占着 tick 队列。
        if (progressed) {
            clearIdle(world, pos);
        } else if (bumpIdle(world, pos) >= IDLE_LIMIT) {
            terminate(world, pos, random);
            clearIdle(world, pos);
            return;   // 不再登记 tick，这棵树这一节的生命周期到此为止
        }
        world.scheduleBlockTick(pos, this, interval);
    }

    // ———————————————————— 生命周期终止 ————————————————————

    /** 连续这么多次请求没有任何产出，就认为这一节长完了（野生树） */
    private static final int IDLE_LIMIT = 10;

    /**
     * 运行期的「连续无产出」计数，按 (世界, 位置) 记。
     *
     * <p><b>为什么不放进 blockstate</b>：加一个 0~10 的属性会把方块状态数从 245,760
     * 抬到 2,703,360（11 倍）。方块注册的代价是「状态数 × 属性取值数之和」次 Map 查找，
     * 11 倍状态就意味着注册耗时涨一个量级（实测过：状态数 +33% 时耗时曾涨 13 倍）。
     * 而丢表的代价仅仅是某棵树重新数 10 次请求 —— 不值得为它付状态空间的账。
     *
     * <p>计数本身不入档没关系：终止的<b>结果</b>会写回方块状态
     * （主干 {@link #TOPPED} / 侧枝 {@link #DORMANT}），那个是持久的，重启后不会复活。
     */
    private static final Map<RegistryKey<World>, Map<BlockPos, Integer>> IDLE = new ConcurrentHashMap<>();

    private static int bumpIdle(ServerWorld world, BlockPos pos) {
        return IDLE.computeIfAbsent(world.getRegistryKey(), k -> new ConcurrentHashMap<>())
                .merge(pos, 1, Integer::sum);
    }

    private static void clearIdle(ServerWorld world, BlockPos pos) {
        Map<BlockPos, Integer> m = IDLE.get(world.getRegistryKey());
        if (m != null) {
            m.remove(pos);
        }
    }

    /**
     * 生命周期终止：把这节标记为「长完了」，此后不再生长。
     *
     * <p>用<b>已有属性</b>做持久标记，不新增属性（理由见 {@link #IDLE}）：
     * <ul>
     *   <li>主干 → {@link #TOPPED}：不再抽高；砍断上方也不会重新抽
     *       —— 这正是治「树挖一节长一节」的地方</li>
     *   <li>侧枝 → {@link #DORMANT}：不参与请求循环，且修剪时不再萌蘖</li>
     * </ul>
     *
     * <p>侧枝若已够开花年龄（growth ≥ (max+1)/2），终止 = <b>开花收场</b>：
     * 养分封顶的冠层枝（上生子枝）到不了旧的 max-1 强制停止线，分叉窗口期满
     * 走到这里是它们唯一的花叶出口——终止绝不能让它们光秃秃地睡去。
     */
    private void terminate(ServerWorld world, BlockPos pos, Random random) {
        BlockState s = world.getBlockState(pos);
        if (!(s.getBlock() instanceof MoranBranchBlock)) {
            return;
        }
        if (s.get(TRUNK)) {
            world.setBlockState(pos, s.with(TOPPED, true), Block.NOTIFY_ALL);
            return;
        }
        world.setBlockState(pos, s.with(DORMANT, true), Block.NOTIFY_ALL);
        int max = species.treeMaxGrowth(treeOrigin(world, pos, s.get(FACING)).asLong());
        if (s.get(GROWTH) >= (max + 1) / 2) {
            species.onBranchStop(world, pos, s.get(FACING), random, s.get(NATURAL));
        }
    }

    /**
     * 执行一次生长：自身成熟度 +1，再按身份推进结构（抽高/侧芽/侧枝/开花）。
     *
     * @return 本拍是否真的产出了什么（成熟度提升 / 放了新方块 / 分叉 / 开花）。
     *         生命周期终止判定靠它：连续若干次请求全都没产出，就认为这棵树长完了。
     */
    private boolean growPart(BlockState state, ServerWorld world, BlockPos pos, Random random, HormoneProfile hormones, int max) {
        Direction facing = state.get(FACING);
        int growth = state.get(GROWTH);
        boolean trunk = state.get(TRUNK);   // 身份看 TRUNK，不看 FACING（上生子枝 FACING 也是 UP）

        // 自身成熟：档位体系相对本树 max（树基种子推出，成熟标志）——
        // 主干至 max；侧枝至 min(max-1, 养分值)——侧枝恒细于母干一档。
        // 养分从根部发起（主干=当前 growth），沿结构传递递减：
        // 同链每节距离损耗、换向分叉分流损耗，末梢天然细小
        int cap = trunk ? max
                : Math.min(max - 1, nutritionAt(world, pos, facing));
        boolean progressed = false;
        if (growth < cap) {
            growth += 1;
            world.setBlockState(pos, state.with(GROWTH, growth), Block.NOTIFY_ALL);
            progressed = true;
        }

        if (trunk) {
            progressed |= growTrunk(state, world, pos, random, growth, hormones, max);
        } else {
            progressed |= growBranch(state, world, pos, random, growth, facing, hormones, max);
        }
        return progressed;
    }

    /** 主干生长：抽高与封顶决策 → 到顶唤醒侧芽并定干分叉 */
    private boolean growTrunk(BlockState state, ServerWorld world, BlockPos pos, Random random, int growth, HormoneProfile hormones, int max) {
        boolean progressed = false;
        BlockPos above = pos.up();
        boolean top = !(world.getBlockState(above).getBlock() instanceof MoranBranchBlock);
        int height = heightBelow(world, pos, species.biologicalTopMax()) + 1;

        // 封顶决策（只在顶端做一次，结果持久化为 TOPPED）：
        // 桃树范式（开心形）：主干只长 max/2 格（桃 3-4 格），上半部分完全交给侧枝
        // 展开成冠层，整树 6-8 格。上方被遮挡同样视为到顶。
        if (top && !state.get(TOPPED)) {
            boolean canExtend = world.getBlockState(above).isAir();
            if (height >= species.trunkHalfHeight(max) || !canExtend) {
                state = state.with(TOPPED, true);
                world.setBlockState(pos, state, Block.NOTIFY_ALL);
                progressed = true;
            }
        }

        if (state.get(TOPPED)) {
            // 到顶：唤醒全部休眠侧芽（幂等），侧枝期开始
            wakeDormantBuds(world, pos);
            // 定干分叉（开心形核心）：主干尽头不再长顶花苞（无直立中央头），
            // 而是四水平向逐拍抽出主枝（几乎与干等粗），向上发散构成冠层。
            // 幂等判据 = 邻居本身（已是主枝的方向跳过），无需新增状态；
            // 每拍最多落一根 —— 四根主枝四拍完成，符合「每拍最多一落」的节奏。
            // 等干长满（growth 到 max）再定干。主枝出生档位 = max-2：
            // 既有一定粗度（现实主枝与干近乎等粗），又留出长到 max-1 的空间——
            // 出生即满档会被 isFullyGrown 判静默，冠层就长不出来。
            if (top && growth >= max && world.getBlockState(pos).get(FORK_SET).count() < species.limbCount()) {
                for (Direction d : TreeSpecies.HORIZONTALS) {
                    BlockPos p = pos.offset(d);
                    if (world.getBlockState(p).isAir() && !isCrowded(world, p)) {
                        world.setBlockState(p, getDefaultState()
                                .with(GROWTH, Math.max(1, max - 2))
                                .with(FACING, d).with(TRUNK, false)
                                .with(NATURAL, state.get(NATURAL)), Block.NOTIFY_ALL);
                        // 主干记下这一位：主干侧向填充（trunk/ 动态模型按 FORK_SET 现场拼）
                        // 由此生成；侧枝被砍时 onStateReplaced 的清位逻辑会同步摘掉填充。
                        BlockState cur = world.getBlockState(pos);
                        int slot = slotOf(cur.get(FACING), d);
                        if (slot >= 0) {
                            world.setBlockState(pos, cur.with(FORK_SET,
                                    ForkSet.of(cur.get(FORK_SET).mask() | (1 << slot))), Block.NOTIFY_ALL);
                        }
                        progressed = true;
                        break;   // 每拍一根
                    }
                }
            }
        } else if (top && world.getBlockState(above).isAir()) {
            // 抽高期：自身 >= 2 才能向上生新节（新节恒为 1，「新块最大为自身减一」）
            if (growth >= 2) {
                world.setBlockState(above, getDefaultState()
                        .with(NATURAL, state.get(NATURAL)), Block.NOTIFY_ALL);
                progressed = true;
            }
        }

        // 侧芽萌发：位置由树种判定（桃范式恒 false —— 侧枝完全由定干承担），
        // 机制保留给需要逐节侧芽的树形。数量上限内概率递减，向光 + 空间竞争；
        // 激素：细胞分裂素促萌出 × 生长素压萌出（顶端优势的生理本义）。
        // 抽高期萌发的为休眠态（到顶统一唤醒）；封顶后补芽直接苏醒态（密度受 maxBuds 硬上限约束）
        if (growth >= 2) {
            int buds = countBudsAroundTrunk(world, pos);
            float budChance = (float) (species.maxBuds() - buds)
                    * hormones.cytokinin() * (2F - hormones.auxin()) / species.budChanceDenom();
            if (buds < species.maxBuds()
                    && species.isBudPosition(world, pos, height, max)
                    && random.nextFloat() < budChance) {
                Direction d = phototropicDirection(world, pos, random);
                BlockPos p = pos.offset(d);
                if (world.getBlockState(p).isAir() && !isCrowded(world, p)) {
                    boolean dormant = !treeTopped(world, pos);
                    world.setBlockState(p, getDefaultState()
                            .with(FACING, d).with(TRUNK, false).with(DORMANT, dormant)
                            .with(NATURAL, state.get(NATURAL)), Block.NOTIFY_ALL);
                    progressed = true;
                }
            }
        }
        return progressed;
    }

    /** 侧枝链长上限绝对值（含贴干首节）：养分高的一级枝 6 节，末级枝按营养递减。
     *  旧值 3 把冠缘横展锁死在 ~4 格（宽高比 ≤0.8），长不出伞形开张树冠 —— 放宽到 6，
     *  一级枝 6 节 + 换向二级 4 节 + 末级 2 节，冠缘距轴可达 ~10-12 格（宽高比 ~1.5）。 */
    private static final int MAX_BRANCH_CHAIN = 6;
    /** 侧枝末端延伸概率（主干同款的「抽高」，横向版） */
    private static final float BRANCH_EXTEND_CHANCE = 0.75F;

    /**
     * 侧枝生长（与主干同构）：末端延伸成链 -> 链上分叉 -> 停止开花。
     * 此前版本缺失延伸逻辑且子侧枝距离判定写反（量父枝位置，贴干芽恒为1），
     * 导致侧枝永远单节、直接开花，树形光秃。
     */
    private boolean growBranch(BlockState state, ServerWorld world, BlockPos pos, Random random, int growth, Direction facing, HormoneProfile hormones, int max) {
        int chainPos = chainPosition(world, pos, facing); // 1 = 贴干首节
        BlockPos tip = pos.offset(facing);
        boolean tipAir = world.getBlockState(tip).isAir();

        // 末端延伸：链上限 = 养分 - 链长预算（预算随本树 max 缩放，max/4；
        // max=8 时营养 8 → 6 节）。下限 2 节 —— 上生子枝
        // （营养 max-2）也至少能抬 2 格，冠层高度才撑得起整树 6-8 格。
        // 激素：赤霉素促节间伸长 —— 高的树把枝拉得更长。
        int chainLimit = Math.min(MAX_BRANCH_CHAIN, Math.max(2, nutritionAt(world, pos, facing) - max / 4));
        if (growth >= 2 && chainPos < chainLimit && tipAir
                && random.nextFloat() < BRANCH_EXTEND_CHANCE * hormones.gibberellin()) {
            world.setBlockState(tip, getDefaultState()
                    .with(FACING, facing).with(TRUNK, false).with(NATURAL, state.get(NATURAL)), Block.NOTIFY_ALL);
            return true;
        }

        // 分叉：成熟 >5 且本节距主干 >1（链上第二节起），抽出一根子侧枝；
        // 侧向被占即本节已分过叉（自然限一节一叉）。
        // 候选方向按母枝朝向分两类——注意 rotateYClockwise/Counterclockwise 对 UP/DOWN
        // 返回的是自身（MC 实现如此），垂直母枝直接用会把子枝放到自己身上：
        //   水平母枝 -> 左右两侧 + 正上/正下（即「上生」「下生」）
        //   垂直母枝（上生/下生抽出的枝再分叉）-> 四个水平向
        // 「自然限一节一叉」：本节已抽出过子枝就不再分叉。
        // 原先只靠「侧向被占」间接限制，而候选方向一多（水平母枝有 4 个），
        // 会被逐 tick 逐个占满，长成一节四叉的畸形——必须显式判定。
        // 分叉：够成熟、离干够远、还有额度、且不在冷却里。
        //   额度由营养决定（越靠梢越细弱、能养的侧枝越少），概率随已分数衰减。
        //   每拍最多落一根 —— 四叉要四拍各自命中，天然稀有，也不会四根同时蹦出来。
        // 分叉门槛相对本树 max（forkMinGrowth = max-1）。两个约束同时卡住这个值：
        // ① 必须严格高于子枝出生档位 1 —— 否则新子枝落地即能再分叉，
        //    树指数爆炸填满空间（实测跑满 3000 轮不收敛）；max-2 在 max=3 时等于 1，会炸。
        // ② 不能高于主枝上限 max-1 —— 那样永远够不到；等于它正好：
        //    主枝长满即可分叉，中途开花早停的那部分主枝不分叉（自然变异）。
        // 分叉：够成熟、离干够远、还有额度、且不在冷却里。
        //   额度由营养决定（越靠梢越细弱、能养的侧枝越少），概率随已分数衰减。
        //   每拍最多落一根 —— 四叉要四拍各自命中，天然稀有，也不会四根同时蹦出来。
        if (growth >= max - 1 && chainPos >= species.forkMinChainPos()) {
            int mask = state.get(FORK_SET).mask();
            int have = Math.max(state.get(FORK_SET).count(), neighborForkCount(world, pos, facing));
            int cap = Math.min(TreeSpecies.MAX_FORKS,
                    species.forkCapacity(nutritionAt(world, pos, facing), max));
            if (have < cap && !forkedWithin(world, pos, facing, species.forkSpacing())) {
                // 激素调制分叉倾向：细胞分裂素促分叉，生长素压分叉（顶端优势，2-auxin 当 auxin=1 时为 1）。
                // 两者都围绕 1.0 抖动，常态下与无激素时完全一致。
                float p = species.subBranchChance() * (float) Math.pow(species.forkDecay(), have)
                        * hormones.cytokinin() * (2F - hormones.auxin());
                if (random.nextFloat() < p) {
                    // 往哪长？—— 不是「随机挑个空位」，是「往光更好的地方去」。
                    // 收益用开口度度量（与侧芽萌发同一个启发式），向顶性强度跟着生长素走；
                    // 收益低于阈值的候选直接排除：光不好就不长，这才是大多数方向不长叉的原因。
                    // 门槛本身也随生长素抬升（顶端优势：激素旺的树只在光特别好的地方才分叉）。
                    List<Direction> free = new ArrayList<>(4);
                    int[] gains = new int[4];
                    int totalGain = 0;
                    int bestGain = Integer.MIN_VALUE;
                    for (Direction side : perpendiculars(facing)) {
                        int slot = slotOf(facing, side);
                        if (slot < 0 || (mask & (1 << slot)) != 0) {
                            continue;
                        }
                        if (!world.getBlockState(pos.offset(side)).isAir()) {
                            continue;
                        }
                        int g = species.branchForkGain(world, pos, facing, side, hormones);
                        if (g > bestGain) {
                            bestGain = g;
                        }
                        if (g > 0) {
                            gains[slot] = g;
                            free.add(side);
                            totalGain += g;
                        }
                    }
                    // 一个值得长的方向都没有 —— 本拍就不分叉
                    int minGain = Math.round(species.forkMinGain() * hormones.auxin());
                    if (bestGain >= minGain && !free.isEmpty()) {
                        // 按收益加权随机：偏向光好的那边，但不总是同一个方向
                        int roll = random.nextInt(totalGain);
                        Direction side = free.get(free.size() - 1);
                        for (Direction candidate : free) {
                            roll -= gains[slotOf(facing, candidate)];
                            if (roll < 0) {
                                side = candidate;
                                break;
                            }
                        }
                        // 子枝档位：新枝一律从 1 档长起（先细后粗，由营养定上限）。
                        // 不能按「母枝档位-1」给出生值 —— 那样二级/up 子枝出生档位
                        // 就等于它的营养上限，永远长不到可延伸的 2 档，冠层抬不起来。
                        // 视觉上子枝的粗细由「母枝档位 + 槽位」在模型工厂里算，
                        // 与本节自身的 GROWTH 无关，两者不冲突。
                        world.setBlockState(pos.offset(side), getDefaultState()
                                .with(FACING, side).with(TRUNK, false)
                                .with(NATURAL, state.get(NATURAL)), Block.NOTIFY_ALL);
                        // 母枝那格记下这一位，好挑对应的分叉模型；不记就只画得出一根母枝。
                        // 档位不必存：模型由母枝 GROWTH + 这一组方向唯一确定。
                        // 必须从世界重读本格状态——传进来的 state 是生长前的快照，
                        // 上面可能刚提过 GROWTH，直接复用会把它写回旧值。
                        world.setBlockState(pos, world.getBlockState(pos)
                                .with(FORK_SET, ForkSet.of(mask | (1 << slotOf(facing, side)))), Block.NOTIFY_ALL);
                        return true;
                    }
                }
            }
        }

        // 主动停止进入开花（养分封顶前的提前成花）：成熟度过门槛后按概率停，
        // 概率随链位置递增（先端易成花、中段持续生长）—— 冠层自然收成圆拱。
        // 只对还没到自身 cap 的节生效：到 cap 的节留在请求循环里走完分叉窗口，
        // 由生命周期终止（terminate）以开花收场。旧的「到 max-1 强制停」是
        // 树冠光秃的根因：强制线与分叉门槛同在一拍开启，分叉只获得一次掷骰；
        // 而养分封顶的冠层枝（上生子枝 cap = max-2 以下）根本够不到强制线，
        // 只剩低概率彩票对跑 10 拍终止计数，多数没开花就休眠了。
        int cap = Math.min(max - 1, nutritionAt(world, pos, facing));
        if (growth < cap && growth >= (max + 1) / 2
                && random.nextFloat() < species.branchStopChance() * chainPos / (float) chainLimit) {
            species.onBranchStop(world, pos, facing, random, state.get(NATURAL));
            // 停止即终末：开花后本节休眠——不再延伸/分叉/复花（scheduledTick 开头挡下，
            // 修剪响应也按终止处理，防「砍一节长一节」）。重读本格状态：state 是生长前快照。
            world.setBlockState(pos, world.getBlockState(pos).with(DORMANT, true), Block.NOTIFY_ALL);
            return true;   // 开花也算产出（放了花苞）
        }
        return false;
    }

    /**
     * 本节已有几根子枝。
     *
     * 主判据是自己记的 {@link #SUBMASK}——它是「本节分过叉」的持久记录，
     * 不受子枝后来长走、被剪、开花消失的影响。
     *
     * 兜底再扫一遍邻居：旧存档里的枝没有 submask（自动落 0），但确实已经长出了子枝，
     * 靠邻居扫描能把它们认出来，避免老树二次分叉。
     * 同类朝向（d == selfFacing）的邻居是链上的延伸节，不是子枝，必须排除——
     * 否则每根枝都会被自己的下一节误判成「已有子枝」，永远分不出叉。
     */
    private static int neighborForkCount(ServerWorld world, BlockPos pos, Direction selfFacing) {
        int mask = world.getBlockState(pos).get(FORK_SET).mask();
        if (mask != 0) {
            return Integer.bitCount(mask);
        }
        int n = 0;
        for (Direction d : Direction.values()) {
            if (d == selfFacing) {
                continue;
            }
            BlockState s = world.getBlockState(pos.offset(d));
            if (s.getBlock() instanceof MoranBranchBlock && !s.get(TRUNK) && s.get(FACING) == d) {
                n++;
            }
        }
        return n;
    }

    /**
     * 链上前 {@code spacing} 节内是否已经分过叉（空间冷却）。
     * 防止相邻几节都往外抽枝，视觉上糊成一团。
     */
    private static boolean forkedWithin(ServerWorld world, BlockPos pos, Direction facing, int spacing) {
        BlockPos p = pos;
        for (int i = 1; i < spacing; i++) {
            p = p.offset(facing.getOpposite());
            BlockState s = world.getBlockState(p);
            if (!(s.getBlock() instanceof MoranBranchBlock) || s.get(FACING) != facing) {
                return false;
            }
            if (!s.get(FORK_SET).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 修剪响应：枝干被剪断后，断口相邻的枝干按档案概率向断口萌发新芽
     * （现实园艺：修剪促萌蘖——玩家由此可以主动塑形自己的树）。
     */
    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        super.onStateReplaced(state, world, pos, newState, moved);
        if (world.isClient || newState.getBlock() instanceof MoranBranchBlock) {
            return;
        }
        if (world instanceof ServerWorld serverWorld) {
            clearIdle(serverWorld, pos);
        }

        // 生命周期已终止、或已经封顶的节，被砍掉不再萌蘖 —— 否则玩家砍一节，树又长一节。
        // 只挡「封顶的那一节」和「终止标记」：没封顶的普通节照旧响应修剪（那是设计里的塑形玩法）。
        if (state.get(DORMANT) || (state.get(TRUNK) && state.get(TOPPED))) {
            return;
        }

        // 被移除的是子枝 -> 通知母枝清掉对应的那一位，否则母枝会一直挂着
        // 「我这里有根子枝」的分叉模型，画出一根已经不存在的枝。
        if (!state.get(TRUNK) && state.get(FORK_SET).isEmpty()) {
            Direction back = state.get(FACING).getOpposite();
            BlockPos ppos = pos.offset(back);
            BlockState parent = world.getBlockState(ppos);
            if (parent.getBlock() instanceof MoranBranchBlock && !parent.get(FORK_SET).isEmpty()) {
                int slot = slotOf(parent.get(FACING), state.get(FACING));
                if (slot >= 0 && (parent.get(FORK_SET).mask() & (1 << slot)) != 0) {
                    world.setBlockState(ppos,
                            parent.with(FORK_SET, ForkSet.of(parent.get(FORK_SET).mask() & ~(1 << slot))),
                            Block.NOTIFY_ALL);
                }
            }
        }

        if (world.getRandom().nextFloat() >= species.pruneResponseChance()) {
            return;
        }
        // FACING 现已含 DOWN，断口在上方或下方都能生成朝向断口的新芽，不必再跳过 UP
        for (Direction d : Direction.values()) {
            BlockPos n = pos.offset(d);
            BlockState neighbor = world.getBlockState(n);
            if (neighbor.getBlock() instanceof MoranBranchBlock) {
                // 断口处生成朝向母体的新芽（growth=1），立即登记生长请求。
                // 用 neighbor.with(...) 从邻居复制状态：TRUNK 未在下方显式列出，故自动继承——
                // 主干断了长出来的仍是主干（继续抽高），侧枝断了长出来的仍是侧枝。
                // FORK_SET 必须显式清零：新芽自己没分叉，继承下来会画出不存在的分叉。
                world.setBlockState(pos, neighbor
                        .with(FACING, d.getOpposite())
                        .with(GROWTH, 1)
                        .with(DORMANT, false)
                        .with(TOPPED, false)
                        .with(FAILS, 0)
                        .with(FORK_SET, ForkSet.NONE), Block.NOTIFY_ALL);
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
            if (above.getBlock() instanceof MoranBranchBlock && above.get(TRUNK)) {
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
        int max = species.treeMaxGrowth(treeOrigin(world, pos, state.get(FACING)).asLong());
        if (state.get(TRUNK)) {
            if (growth < max) {
                return false;
            }
            BlockPos above = pos.up();
            boolean top = !(world.getBlockState(above).getBlock() instanceof MoranBranchBlock);
            if (top) {
                // 定干分叉未完成（主枝数未达 limbCount 且还有能落的空位）→ 还需 tick。
                // 主枝数以 FORK_SET 位计数(权威:开心形主枝 2~5,见 TreeSpecies.limbCount);
                // 全部占满或拥挤(放不出去)即视为完成;极端拥挤的死锁由 IDLE 兜底。
                if (state.get(FORK_SET).count() < species.limbCount()) {
                    for (Direction d : TreeSpecies.HORIZONTALS) {
                        BlockPos p = pos.offset(d);
                        if (world.getBlockState(p).isAir() && !isCrowded(world, p)) {
                            return false;
                        }
                    }
                }
            }
            return treeTopped(world, pos);
        }
        // 侧枝没有「结构长满即静默」的出口：养分封顶的节（cap < max-1）若在此静默，
        // 会跳过开花直接沉默——整层树冠的花和叶就没了。侧枝的终局只有一个：
        // 开花时写入的 DORMANT，由 scheduledTick 开头挡下；到 cap 未开花的节
        // 要留在请求循环里走完分叉窗口，再由 terminate 以开花收场。
        return false;
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

    /**
     * 树的「身份证」：树基（主干最底下那格）。
     *
     * 侧枝先沿链向回走（与 {@link #nutritionAt} 同一条路）到主干，再沿主干向下走到基座。
     * 激素档案以此为种子（见 {@link TreeSpecies#hormones}）—— 同一棵树的所有节
     * 都推出同一份档案，零存储、跨重启不变。链被打断后的孤儿枝走不到基座，
     * 按自己位置另起一份，后果只是偏好略变，可接受。
     */
    public static BlockPos treeOrigin(WorldView world, BlockPos pos, Direction facing) {
        BlockPos p = pos;
        Direction d = facing;
        for (int i = 0; i < 16; i++) {
            BlockState s = world.getBlockState(p);
            if (!(s.getBlock() instanceof MoranBranchBlock) || s.get(TRUNK)) {
                break;
            }
            BlockPos back = p.offset(d.getOpposite());
            BlockState bs = world.getBlockState(back);
            if (!(bs.getBlock() instanceof MoranBranchBlock)) {
                break;
            }
            p = back;
            d = bs.get(FACING);
        }
        for (int i = 0; i < 32; i++) {
            BlockState below = world.getBlockState(p.down());
            if (below.getBlock() instanceof MoranBranchBlock && below.get(TRUNK)) {
                p = p.down();
            } else {
                break;
            }
        }
        return p;
    }

    /** 向光性近似（开口度启发式）：各水平方向数 2 格内空气 + 上方开口，加权随机 */
    private Direction phototropicDirection(ServerWorld world, BlockPos pos, Random random) {
        int totalWeight = 0;
        int[] weights = new int[TreeSpecies.HORIZONTALS.length];
        for (int i = 0; i < TreeSpecies.HORIZONTALS.length; i++) {
            // 开口度与分叉收益共用同一个度量（TreeSpecies.opennessAround），不再各写一遍
            weights[i] = 1 + TreeSpecies.opennessAround(world, pos, TreeSpecies.HORIZONTALS[i]);
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
            if (!(s.getBlock() instanceof MoranBranchBlock) || !s.get(TRUNK)) {
                break;
            }
            height++;
            p = p.down();
        }
        return height;
    }

    private int trunkSegmentsAbove(ServerWorld world, BlockPos pos) {
        int n = 0;
        BlockPos p = pos.up();
        while (n < species.biologicalTopMax()) {
            BlockState s = world.getBlockState(p);
            if (!(s.getBlock() instanceof MoranBranchBlock) || !s.get(TRUNK)) {
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
            if (above.getBlock() instanceof MoranBranchBlock && above.get(TRUNK)) {
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
            if (above.getBlock() instanceof MoranBranchBlock && above.get(TRUNK)) {
                cur = cur.up();
            } else {
                return;
            }
        }
    }

    /**
     * 本格养分值：沿结构回溯到主干（根部养分发起，主干养分 = 其当前 growth），
     * 途中同链延伸每节扣距离损耗，换向分叉扣分叉损耗。主干长粗后侧枝上限自动放开。
     */
    private int nutritionAt(ServerWorld world, BlockPos pos, Direction facing) {
        int decay = 0;
        Direction dir = facing;
        BlockPos p = pos;
        for (int i = 0; i < 24; i++) {
            BlockPos back = p.offset(dir.getOpposite());
            BlockState s = world.getBlockState(back);
            if (!(s.getBlock() instanceof MoranBranchBlock)) {
                return Math.max(1, species.trunkMaxGrowth() - decay); // 断链孤儿：按剩余养分
            }
            if (s.get(TRUNK)) {
                return Math.max(1, s.get(GROWTH) - decay);
            }
            decay += (s.get(FACING) == dir) ? species.chainNutritionDecay() : species.branchNutritionDecay();
            p = back;
            dir = s.get(FACING);
        }
        return 1;
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
        // 主干与垂直子枝（上生/下生）同为竖直柱，共用主干形状表
        if (state.get(TRUNK) || state.get(FACING).getAxis() == Direction.Axis.Y) {
            return TRUNK_SHAPES[state.get(GROWTH)];
        }
        return BUD_SHAPES.get(state.get(FACING))[state.get(GROWTH)];
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        // 细枝可穿行，侧芽不挡路，粗壮的竖直枝干才实心
        boolean vertical = state.get(TRUNK) || state.get(FACING).getAxis() == Direction.Axis.Y;
        if (!vertical || state.get(GROWTH) < 3) {
            return VoxelShapes.empty();
        }
        return TRUNK_SHAPES[state.get(GROWTH)];
    }

    // 骨粉：直接催一节成熟度（绕过请求与随机数）
    @Override
    public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state, boolean isClient) {
        int growth = state.get(GROWTH);
        int max = species.treeMaxGrowth(treeOrigin(world, pos, state.get(FACING)).asLong());
        return state.get(TRUNK)
                ? growth < max
                : growth < max - 1;
    }

    @Override
    public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
        int growth = state.get(GROWTH);
        int max = species.treeMaxGrowth(treeOrigin(world, pos, state.get(FACING)).asLong());
        int cap = state.get(TRUNK) ? max
                : Math.min(max - 1, nutritionAt(world, pos, state.get(FACING)));
        if (growth < cap) {
            world.setBlockState(pos, state.with(GROWTH, growth + 1), Block.NOTIFY_ALL);
        }
        // 玩家骨粉 = 重新评估环境的钥匙：冻结/中断的树由此重新登记请求，
        // 下一拍重做环境检查——通过则继续生长，不通过维持冻结（失败被容许）
        world.scheduleBlockTick(pos, this,
                state.get(NATURAL) ? NATURAL_INTERVAL : TreeSpecies.REQUEST_INTERVAL);
    }
}
