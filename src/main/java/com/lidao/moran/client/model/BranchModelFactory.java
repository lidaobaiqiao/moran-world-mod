package com.lidao.moran.client.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lidao.moran.systems.trees.TreeSpecies;
import net.minecraft.util.math.Direction;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 树枝模型工厂——把「枝的走向 + 档位」直接算成模型 JSON，磁盘上零模型文件。
 *
 * <h2>为什么可以算</h2>
 * 一根树枝就是「沿某轴贯穿整格的方柱」，截面半宽由档位唯一确定；
 * 六个面的 uv 也全部是档位的闭式函数。已用作者手作的 32 个单元素侧枝
 * （4 方位 x 8 档）逐字段核对，除 4 类已知小偏差外全部吻合：
 * <ul>
 *   <li>g1 的窄侧面（宽 2）作者特贴在纹理 12~14 区，公式给 14~16，差 2px</li>
 *   <li>南侧枝的 up/down 面 rotation 与公式差 180°（纹理上下颠倒，树干纹理上看不出来）</li>
 *   <li>g7 的端面作者用了 #1 槽而非 #2</li>
 * </ul>
 * 这三类经作者确认不予追究——视觉上不可辨，且统一后代码干净得多。
 *
 * <h2>几何约定</h2>
 * <b>基准系</b>：柱体沿 +Z 从 0 贯穿到 16，截面居中于 (8,8)，半宽 h。
 * 每根柱体 = 基准柱体 经「正交标架」映射到世界坐标：标架的 z 轴指向几何 +Z 的世界方向，
 * y 轴取世界 UP（垂直枝取 NORTH），x = y × z。
 *
 * <h2>uv 公式（基准系）</h2>
 * <pre>
 *   端面 (基准 +Z)  ：[8-h, 8-h, 8+h, 8+h]  rot 无    #2
 *   端面 (基准 -Z)  ：[8-h, 8-h, 8+h, 8+h]  rot 180   #2
 *   侧面 (基准 +X)  ：[16-w, 0, 16, 16]     rot 90    #0    w = 2h
 *   侧面 (基准 -X)  ：[0, 0, w, 16]         rot 270   #0
 *   侧面 (基准 +Y)  ：[s, 0, s+w, 16]       rot 无    #0    s = min(8, 16-w)
 *   侧面 (基准 -Y)  ：[16-(s+w), 0, 16-s, 16] rot 180 #0
 * </pre>
 *
 * <h2>子枝的额外规则</h2>
 * 子枝不贯穿，沿自身轴只占 {@code [8±growth, 0|16]} 那半格：母枝占 [8-h, 8+h]，
 * 正方向留出 [8+h, 16]、负方向留出 [0, 8-h]，**子枝把这个空间全部填满**，
 * 所以长度不用单独指定，由空间反推（= 8 - 档位）。
 * 截面半宽 {@code c = max(1, growth-1)}（档位减一，g1 没有更细的档）。
 * <ul>
 *   <li>子枝的面数据<b>只跟轴有关</b>：手作件验证 下生==上生、西生==东生、北生==南生</li>
 *   <li>六个面全部贴图，<b>不留 #missing 占位</b>——占位面一旦因为生长方向写反而翻到外面，
 *       就是一块渲染不出来的面。宁可把贴母枝那端也贴满</li>
 * </ul>
 */
public final class BranchModelFactory {

    private BranchModelFactory() {
    }

    // 方向向量表（用于正交标架与面朝向判定）
    private static final int[][] VEC = new int[6][];
    private static final Direction[] DIRS = {
            Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };

    static {
        VEC[Direction.DOWN.ordinal()] = new int[]{0, -1, 0};
        VEC[Direction.UP.ordinal()] = new int[]{0, 1, 0};
        VEC[Direction.NORTH.ordinal()] = new int[]{0, 0, -1};
        VEC[Direction.SOUTH.ordinal()] = new int[]{0, 0, 1};
        VEC[Direction.WEST.ordinal()] = new int[]{-1, 0, 0};
        VEC[Direction.EAST.ordinal()] = new int[]{1, 0, 0};
    }

    /**
     * 生成结果缓存：同一个 (树种, 方向, 档位, 子枝集合) 只算一次。
     * 键里用树种 id 而不是贴图串 —— 一个树种一套贴图，id 更短且语义更准。
     */
    private static final Map<String, JsonObject> CACHE = new ConcurrentHashMap<>();

    /**
     * 造一个树枝模型——「生成器相加」的结果。
     *
     * <p>结构：
     * <ol>
     *   <li><b>四侧生成器</b>：按母枝方向 + 档位，生成贯穿整格的主柱，并返回它的占位（坐标区间 + 半宽）</li>
     *   <li><b>子枝生成器</b>（上下 / 南北 / 东西三选一）：<b>以上一步的返回值为输入</b>——
     *       由母枝半宽推出子枝半宽（档位减一），由母枝占位推出剩余空间充当子枝长度，
     *       填满那半格</li>
     *   <li>还有别的子枝请求就继续相加，同一个母枝可以叠多根</li>
     * </ol>
     * 子枝自己长出的子枝不由这里管——那是一根新枝，由它自己的方块再走一遍同样的流程。
     *
     * @param facing 母枝走向（方块 FACING）
     * @param growth 母枝档位 1~8（方块 GROWTH）
     * @param subs   本节分出的子枝方向集合（方块 submask）
     * @param species 树种档案（贴图由它提交）
     */
    public static JsonObject build(Direction facing, int growth, Set<Direction> subs, TreeSpecies species) {
        String key = species.id() + "|" + facing.asString() + "|" + growth + "|" + canonicalSubs(subs);
        return CACHE.computeIfAbsent(key, k -> generate(facing, growth, subs, species));
    }

    /** 子枝集合转成稳定字符串（顺序无关，保证同一状态永远同一个缓存键） */
    private static String canonicalSubs(Set<Direction> subs) {
        StringBuilder sb = new StringBuilder();
        for (Direction d : DIRS) {
            if (subs.contains(d)) {
                sb.append(d.asString()).append('_');
            }
        }
        return sb.toString();
    }

    private static JsonObject generate(Direction facing, int growth, Set<Direction> subs, TreeSpecies species) {
        JsonObject model = new JsonObject();
        // 继承原版 block/block：它只提供标准 display 与 gui_light=side，不含 textures/elements，
        // 故不会覆盖下面的纹理与几何。不要自己写 display，否则物品栏显示会变形。
        model.addProperty("parent", "minecraft:block/block");
        model.addProperty("render_type", "minecraft:cutout");

        // 贴图由树种提交，生成器只是搬运工 —— 引擎里不含任何树种外观
        JsonObject textures = new JsonObject();
        textures.addProperty("0", species.barkTexture());
        textures.addProperty("2", species.capTexture());
        textures.addProperty("particle", species.barkTexture());
        model.add("textures", textures);

        JsonArray elements = new JsonArray();

        // ① 四侧生成器 —— 母枝，沿 facing 轴贯穿 0..16，半宽 = 档位
        Footprint trunk = sideGenerator(facing, growth);
        elements.add(trunk.element);

        // ② 子枝生成器 —— 以母枝的返回值为输入，逐个相加
        for (Direction sub : subs) {
            elements.add(subGenerator(trunk, sub));
        }

        model.add("elements", elements);
        return model;
    }

    /** 一根已生成柱子的占位信息，供下一层生成器当输入 */
    private record Footprint(Direction axis, int half, int lo, int hi, JsonObject element) {
    }

    /** 四侧生成器：母枝/主干。占满整格，把占位交给下一层 */
    private static Footprint sideGenerator(Direction facing, int growth) {
        return new Footprint(facing, growth, 0, 16, column(facing, growth, 0, 16, false));
    }

    /**
     * 子枝生成器（上下 / 南北 / 东西三选一，按子枝所在轴）。
     *
     * <p>输入是母枝的占位：半宽 → 子枝半宽 = max(1, 母枝半宽 - 1)（档位减一）；
     * 母枝占 [8-h, 8+h]，于是正方向那半格 [8+h, 16]、负方向那半格 [0, 8-h] 就是
     * 留出来的空间，全部填满——子枝长度因此不需要单独指定，由空间反推。
     */
    private static JsonObject subGenerator(Footprint parent, Direction sub) {
        int c = Math.max(1, parent.half() - 1);
        boolean positive = VEC[sub.ordinal()][sub.getAxis().ordinal()] > 0;
        int lo = positive ? 8 + parent.half() : 0;
        int hi = positive ? 16 : 8 - parent.half();
        return column(sub, c, lo, hi, true);
    }

    /**
     * 造一根沿 dir 走、沿轴占据 {@code [lo, hi]}（0..16 坐标系）的方柱。
     *
     * <p>做法：先在基准系（沿 +Z 从 0 到 16）里算好几何与六面数据，再按正交标架映射出去。
     * 这样 uv 公式只需在基准系推一遍。
     *
     * @param isSub true = 子枝（有自己的侧面 uv 槽位规则、朝外端面才贴端面纹理）；
     *              false = 母枝/主干（贯穿件，两端面都是截断面）
     */
    private static JsonObject column(Direction dir, int half, int lo, int hi, boolean isSub) {
        // ---- 正交标架（几何用；已用手作件逐字节验证）----
        // 子枝的几何 +Z 恒指向所在轴的**正方向**：下生==上生、西生==东生，
        // 面数据完全相同，只是占位半格不同（[0,8-g] vs [8+g,16]）。
        Direction zWorld = isSub ? positiveOf(dir.getAxis()) : geometricZWorld(dir);
        Direction yWorld = (dir.getAxis() == Direction.Axis.Y) ? Direction.NORTH : Direction.UP;
        int[] zv = VEC[zWorld.ordinal()];
        int[] yv = VEC[yWorld.ordinal()];
        int[] xv = cross(yv, zv);

        int h = half;
        int[] fromW = mapPoint(8 - h, 8 - h, lo, xv, yv, zv);
        int[] toW = mapPoint(8 + h, 8 + h, hi, xv, yv, zv);
        for (int i = 0; i < 3; i++) {
            if (fromW[i] > toW[i]) {
                int t = fromW[i];
                fromW[i] = toW[i];
                toW[i] = t;
            }
        }

        JsonObject el = new JsonObject();
        el.add("from", arr(fromW));
        el.add("to", arr(toW));

        // ---- 六面：uv 尺寸从面的几何算，rotation 从生长方向算 ----
        // 生长方向 g：母枝取 FACING；子枝只取「轴」——
        // 作者手作件验证 下生==上生、西生==东生、北生==南生，面数据与正负无关。
        Direction g = isSub ? positiveOf(dir.getAxis()) : dir;

        JsonObject faces = new JsonObject();
        for (Direction f : DIRS) {
            int[] fd = faceDims(f, fromW, toW);
            boolean end = f.getAxis() == g.getAxis();
            // 贴图归属：**正方形的面**贴截断面，长条面贴树皮。
            // 手作件全样本符合：母枝两端（正方形）是截断面、四个侧面（长条）是树皮；
            // 子枝的三个轴同理，平方的那个面才是截断面（不是「朝外」的那个）。
            boolean isCap = fd[0] == fd[1];
            int rot = isSub ? subRot(dir.getAxis(), f) : (end ? (f == g ? 180 : 0) : sideRot(g, f));
            faces.add(f.asString(), face(uvFor(fd, rot), rot, isCap));
        }

        el.add("faces", faces);
        return el;
    }

    // ---------- uv：尺寸一律等于面的几何尺寸 ----------

    /** 面在 MC 的 u/v 方向上的尺寸 {宽, 高}。u/v 轴约定：法向 X→(u=Z,v=Y)，Y→(X,Z)，Z→(X,Y) */
    private static int[] faceDims(Direction f, int[] from, int[] to) {
        int dx = to[0] - from[0], dy = to[1] - from[1], dz = to[2] - from[2];
        return switch (f.getAxis()) {
            case X -> new int[]{dz, dy};
            case Y -> new int[]{dx, dz};
            case Z -> new int[]{dx, dy};
        };
    }

    /**
     * 按面的真实尺寸取 uv 矩形。rotation 为 90/270 时宽高先对调，
     * 保证「uv 占用的尺寸 == 面的尺寸」，不会拉伸。正方形面居中取，长方形面从 v=0 起。
     */
    private static int[] uvFor(int[] fd, int rot) {
        int fw = fd[0], fh = fd[1];
        boolean swap = (rot == 90 || rot == 270);
        int rw = swap ? fh : fw;
        int rh = swap ? fw : fh;
        int u0 = 8 - rw / 2;
        int v0 = (fw == fh) ? 8 - rh / 2 : 0;
        return new int[]{u0, v0, u0 + rw, v0 + rh};
    }

    // ---------- rotation：全部由「生长方向 + 面法向」算，不查表 ----------

    /** 某轴的正方向 */
    private static Direction positiveOf(Direction.Axis a) {
        return switch (a) {
            case X -> Direction.EAST;
            case Y -> Direction.UP;
            case Z -> Direction.SOUTH;
        };
    }

    /**
     * 侧面 rotation（母枝）。已用手作件 4 方位 × 3 档位全量核对，逐面命中。
     *
     * <p>规则：MC 各面的 u/v 轴随法向而变（法向 X→u∥Z,v∥Y；Y→u∥X,v∥Z；Z→u∥X,v∥Y）。
     * 哪条 uv 轴与生长方向平行，就决定了 rotation 落在 {0,180} 还是 {90,270}；
     * 具体值由「面相对分支朝向的左右上下」决定。
     */
    private static int sideRot(Direction g, Direction f) {
        int[] n = VEC[f.ordinal()], gv = VEC[g.ordinal()];
        Direction upRef = (g.getAxis() == Direction.Axis.Y) ? Direction.NORTH : Direction.UP;
        int[] uv = VEC[upRef.ordinal()];
        int[] r = cross(gv, uv);
        boolean uAlong = uvAxes(f)[0] == g.getAxis().ordinal();
        boolean gPositive = gv[g.getAxis().ordinal()] > 0;
        if (!uAlong) {                       // 长度落在 v 轴上
            int base = dot(n, uv) > 0 ? 0 : 180;
            return (base + (gPositive ? 180 : 0)) % 360;
        }
        if (same(n, r)) return 90;           // 长度落在 u 轴上
        if (same(n, neg(r))) return 270;
        return gPositive ? 90 : 270;
    }

    /** 子枝的 rotation：只跟轴有关（手作件验证 东生==西生、下生==上生），值取自样本 */
    private static int subRot(Direction.Axis a, Direction f) {
        return switch (a) {
            case Y -> 0;                                     // 上/下生：六个面一律不转
            case X -> f == Direction.NORTH ? 90 : 270;       // 东/西生：只有 north 是 90
            case Z -> switch (f) {                           // 北/南生
                case EAST -> 90;
                case WEST, SOUTH -> 270;
                case DOWN -> 180;
                default -> 0;                                // north = 截断面
            };
        };
    }

    /** MC 某面法向对应的 (u 轴序号, v 轴序号) */
    private static int[] uvAxes(Direction f) {
        return switch (f.getAxis()) {
            case X -> new int[]{2, 1};
            case Y -> new int[]{0, 2};
            case Z -> new int[]{0, 1};
        };
    }

    private static boolean same(int[] a, int[] b) {
        return a[0] == b[0] && a[1] == b[1] && a[2] == b[2];
    }

    private static int[] neg(int[] a) {
        return new int[]{-a[0], -a[1], -a[2]};
    }

    private static int dot(int[] a, int[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    /**
     * 几何 +Z 在世界中的方向。
     *
     * <p>注意：所有手作件的几何都是 from 负端 to 正端沿轴 0→16。
     * 「北侧枝」（枝向北伸）的几何 +Z 实际落在世界的 +Z，即 <b>south</b>。
     * 所以这里的映射是「枝名 -> 几何正轴的世界方向」，不是「枝名 -> 枝名」。
     */
    private static Direction geometricZWorld(Direction branchDir) {
        return switch (branchDir) {
            case NORTH -> Direction.SOUTH;   // 枝向北，几何 +Z 在世界 +Z(south)
            case SOUTH -> Direction.NORTH;
            case EAST -> Direction.EAST;     // 枝向东，几何沿 +X 0->16，+X = east
            case WEST -> Direction.WEST;
            case UP -> Direction.UP;         // 枝向上，几何沿 +Y 0->16，+Y = up
            case DOWN -> Direction.DOWN;
        };
    }

    /**
     * 一个面。贴图槽位只有两个：
     * <ul>
     *   <li>{@code #0} 树皮 —— 长条侧面（宽 ≠ 高）</li>
     *   <li>{@code #2} 截断面 —— 正方形面（宽 == 高）</li>
     * </ul>
     * 具体指向哪张贴图由模型头部的 textures 段决定（树种提交）。
     */
    private static JsonObject face(int[] uv, Integer rotation, boolean isCap) {
        JsonObject f = new JsonObject();
        f.add("uv", arr(uv));
        if (rotation != null && rotation != 0) {
            f.addProperty("rotation", rotation);
        }
        f.addProperty("texture", isCap ? "#2" : "#0");
        return f;
    }

    /** 基准点 (px,py,pz) 映射到世界整数坐标 */
    private static int[] mapPoint(int px, int py, int pz, int[] xv, int[] yv, int[] zv) {
        int dx = px - 8, dy = py - 8, dz = pz - 8;
        return new int[]{
                8 + dx * xv[0] + dy * yv[0] + dz * zv[0],
                8 + dx * xv[1] + dy * yv[1] + dz * zv[1],
                8 + dx * xv[2] + dy * yv[2] + dz * zv[2],
        };
    }

    private static int[] cross(int[] a, int[] b) {
        return new int[]{
                a[1] * b[2] - a[2] * b[1],
                a[2] * b[0] - a[0] * b[2],
                a[0] * b[1] - a[1] * b[0],
        };
    }

    private static JsonArray arr(int[] a) {
        JsonArray j = new JsonArray();
        for (int v : a) {
            j.add(v);
        }
        return j;
    }
}
