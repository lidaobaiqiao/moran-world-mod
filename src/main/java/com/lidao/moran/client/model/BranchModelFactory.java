package com.lidao.moran.client.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.util.math.Direction;

import java.util.Set;

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
 * 子枝不贯穿，沿自身轴只占 {@code [8±growth, 0|16]} 那半格，长度 {@code L = 8-growth}，
 * 截面半宽 {@code c = max(1, growth-1)}。与母枝的区别：
 * <ul>
 *   <li>贴母枝那一端的面用 {@code #missing} 占位（正常看不见）</li>
 *   <li>子枝的「几何 +Z」恒指向所在轴的正方向：上/下生共用一套面名映射，
 *       东/西生同理（作者手作件逐字节验证：下生==上生、西生==东生）。
 *       东西生的面名按 Y 轴置换 up(cap)→north、down(missing)→east、
 *       east→up、west→down、north→west、south→south，rotation 统一 +270（轴端面 +90）</li>
 *   <li>四侧面 uv 按固定 4px 槽位（east 0 / south 4 / north 8 / west 16-w），v 范围 = L；
 *       {@code c >= 5} 时槽位重叠，四面统一居中 {@code [8-c, 0, 8+c, L]}</li>
 *   <li>轴端面（朝外）uv = 居中正方形 {@code [8-c, 8-c, 8+c, 8+c]}，贴截断面</li>
 * </ul>
 * 残留偏差（作者手作自身不一致，已确认不追，合计 110 处全为 uv 值差）：
 * g1/g2 第四侧面写死 12 而非 16-w；g3 四面用了居中；g4 侧面 v 写 3 而 L=4；
 * g1 的截断面贴在 north 面而非 up 面；g3 上/下生的 up 面 rotation=90。
 */
public final class BranchModelFactory {

    private BranchModelFactory() {
    }

    /** 侧面树皮 */
    private static final String TEX_SIDE = "moran_mod:block/peach_log";
    /** 端面截断（粗壮树干横截面） */
    private static final String TEX_CAP = "moran_mod:item/thick_peach_trunk_side";

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
     * 造一个树枝模型。
     *
     * @param facing 母枝走向（本方块 FACING）
     * @param growth 母枝档位 1~8（本方块 GROWTH）
     * @param subs   从母枝侧面伸出的子枝方向集合（本方块的分叉方向）
     */
    public static JsonObject build(Direction facing, int growth, Set<Direction> subs) {
        JsonObject model = new JsonObject();
        // 继承原版 block/block：它只提供标准 display（GUI/手持的立体展示）与 gui_light=side，
        // 不含 textures / elements，故不会覆盖下面的纹理与几何。
        // 注意不要自己写 display —— 那会把父模型的标准 display 覆盖掉，物品栏里显示会变形。
        model.addProperty("parent", "minecraft:block/block");
        model.addProperty("render_type", "minecraft:cutout");

        JsonObject textures = new JsonObject();
        textures.addProperty("0", TEX_SIDE);
        textures.addProperty("2", TEX_CAP);
        textures.addProperty("particle", TEX_SIDE);
        model.add("textures", textures);

        JsonArray elements = new JsonArray();

        // 母枝：贯穿整格（沿 facing 轴 0..16），半宽 = growth
        elements.add(column(facing, growth, 0, 16, false));
        // 子枝：半宽 c = max(1, growth-1)，长度 L = 8-growth
        // 只占半格——从母枝表面(8±growth)伸到方块边界(0 或 16)，不贯穿。
        int c = Math.max(1, growth - 1);
        for (Direction sub : subs) {
            boolean positive = sub == Direction.SOUTH || sub == Direction.EAST || sub == Direction.UP;
            elements.add(positive
                    ? column(sub, c, 8 + growth, 16, true)
                    : column(sub, c, 0, 8 - growth, true));
        }

        model.add("elements", elements);
        return model;
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
        // ---- 正交标架：x = y × z ----
        // 关键：子枝的"几何 +Z"恒指向所在轴的**正方向**。
        // 也就是说「下生」与「上生」共用同一套面名映射，只是几何占位不同（[0,8-g] vs [8+g,16]）；
        // 「西生」与「东生」同理。作者手作件已验证：同档位下 下生==上生、西生==东生，逐字节相同。
        Direction zWorld;
        if (isSub) {
            // 注：实际候选里子枝永远垂直于母枝（北/南母枝 → 东西上下，东/西母枝 → 北南上下，
            // 垂直母枝 → 四个水平向），NORTH/SOUTH 子枝不会出现，case 只为穷举。
            zWorld = switch (dir) {
                case DOWN, UP -> Direction.UP;
                case WEST, EAST -> Direction.EAST;
                case NORTH, SOUTH -> Direction.SOUTH;
            };
        } else {
            zWorld = geometricZWorld(dir);
        }
        Direction yWorld = (dir.getAxis() == Direction.Axis.Y) ? Direction.NORTH : Direction.UP;
        int[] zv = VEC[zWorld.ordinal()];
        int[] yv = VEC[yWorld.ordinal()];
        int[] xv = cross(yv, zv);
        Direction xWorld = dirOf(xv);

        // ---- 几何 ----
        // 基准：from=[8-h, 8-h, lo], to=[8+h, 8+h, hi]
        int h = half;
        int[] fromW = mapPoint(8 - h, 8 - h, lo, xv, yv, zv);
        int[] toW = mapPoint(8 + h, 8 + h, hi, xv, yv, zv);
        // 归一化：MC 要求 from <= to（标架映射可能翻转某个轴的坐标顺序）
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

        // ---- 六面数据 ----
        int w = 2 * h;
        // 沿轴长度（用于子枝侧面的 v 范围）
        int len = hi - lo;

        JsonObject faces = new JsonObject();

        // 沿轴方向的两个面（子枝的这两个面在下方的分支里一并写入）
        if (!isSub) {
            // 母枝/主干：两端都是截断面
            faces.add(zWorld.asString(), face(sq(h), null, TEX_CAP));
            faces.add(zWorld.getOpposite().asString(), face(sq(h), 180, TEX_CAP));
        }

        // 垂直子枝轴的四个侧面
        if (!isSub) {
            // 母枝：按「基准系」的固定槽位（宽 w，v 满 0..16）
            int s = Math.min(8, 16 - w);
            faces.add(xWorld.asString(), face(new int[]{16 - w, 0, 16, 16}, 90, TEX_SIDE));
            faces.add(xWorld.getOpposite().asString(), face(new int[]{0, 0, w, 16}, 270, TEX_SIDE));
            faces.add(yWorld.asString(), face(new int[]{s, 0, s + w, 16}, null, TEX_SIDE));
            faces.add(yWorld.getOpposite().asString(), face(new int[]{16 - (s + w), 0, 16 - s, 16}, 180, TEX_SIDE));
        } else {
            // 子枝四侧面：按固定 4px 槽位起步（0 / 4 / 8 / 16-w），v 范围 = 长度
            // c >= 5 时槽位互相重叠，改统一居中（对齐作者在 g6/g7 的处理）
            int[] uvEast, uvNorth, uvSouth, uvWest;
            if (w >= 10) {
                int[] cc = new int[]{8 - h, 0, 8 + h, len};
                uvEast = uvNorth = uvSouth = uvWest = cc;
            } else {
                uvEast = new int[]{0, 0, w, len};
                uvSouth = new int[]{4, 0, 4 + w, len};
                uvNorth = new int[]{8, 0, 8 + w, len};
                uvWest = new int[]{16 - w, 0, 16, len};
            }
            // 轴向（dir 所在轴）
            if (dir.getAxis() == Direction.Axis.Y) {
                // 上生/下生：面名映射与上生完全一致，作者手作件已验证逐字节相同
                // up 面 = 朝外端面（cap），down 面 = 贴母枝（missing）
                faces.add(Direction.UP.asString(), face(sq(h), null, TEX_CAP));
                faces.add(Direction.DOWN.asString(), missing());
                faces.add(Direction.EAST.asString(), face(uvEast, null, TEX_SIDE));
                faces.add(Direction.WEST.asString(), face(uvWest, null, TEX_SIDE));
                faces.add(Direction.NORTH.asString(), face(uvNorth, null, TEX_SIDE));
                faces.add(Direction.SOUTH.asString(), face(uvSouth, null, TEX_SIDE));
            } else {
                // 东生/西生：面名按 Y 轴顺 90° 置换，rotation 统一 +270（轴端面 +90）。
                // 置换表（已用作者手作件逐个核对）：
                //   up(cap) -> north, down(missing) -> east, east -> up, west -> down,
                //   north -> west, south -> south
                int r = 270;
                faces.add(Direction.NORTH.asString(), face(sq(h), 90, TEX_CAP));
                faces.add(Direction.EAST.asString(), missing(r));
                faces.add(Direction.UP.asString(), face(uvEast, r, TEX_SIDE));
                faces.add(Direction.DOWN.asString(), face(uvWest, r, TEX_SIDE));
                faces.add(Direction.WEST.asString(), face(uvNorth, r, TEX_SIDE));
                faces.add(Direction.SOUTH.asString(), face(uvSouth, r, TEX_SIDE));
            }
        }

        el.add("faces", faces);
        return el;
    }

    /** 居中正方形 uv（母枝/子枝的端面） */
    private static int[] sq(int h) {
        return new int[]{8 - h, 8 - h, 8 + h, 8 + h};
    }

    /** #missing 占位面（子枝贴母枝那一端，正常看不见） */
    private static JsonObject missing() {
        return missing(null);
    }

    private static JsonObject missing(Integer rotation) {
        JsonObject f = new JsonObject();
        f.add("uv", arr(new int[]{0, 0, 7, 2}));
        if (rotation != null && rotation != 0) {
            f.addProperty("rotation", rotation);
        }
        f.addProperty("texture", "#missing");
        return f;
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

    private static JsonObject face(int[] uv, Integer rotation, String texture) {
        JsonObject f = new JsonObject();
        f.add("uv", arr(uv));
        if (rotation != null && rotation != 0) {
            f.addProperty("rotation", rotation);
        }
        f.addProperty("texture", TEX_CAP.equals(texture) ? "#2" : "#0");
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

    private static Direction dirOf(int[] v) {
        for (Direction d : DIRS) {
            int[] u = VEC[d.ordinal()];
            if (u[0] == v[0] && u[1] == v[1] && u[2] == v[2]) {
                return d;
            }
        }
        throw new IllegalArgumentException("not a unit axis vector");
    }

    private static JsonArray arr(int[] a) {
        JsonArray j = new JsonArray();
        for (int v : a) {
            j.add(v);
        }
        return j;
    }
}
