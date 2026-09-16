package com.lidao.moran.systems.trees;

import com.lidao.moran.systems.trees.genes.SpeciesGeneLoader;

/**
 * 树种注册表：每种树一份基因档案（基因文件驱动，config/moran_mod/genes/&lt;id&gt;.json，
 * 缺失自动抄出内置默认——档案文件即「品种说明书」，改文件即改树）。
 *
 * 新增树种 = 写一份基因文件 + 此处登记一行 + 注册枝干/花苞方块。
 */
public final class Trees {

    /** 桃树:矮桩开心形(6-8格)、顶腋侧腋着花、先花后叶、盛花小花团 */
    public static final TreeSpecies PEACH = SpeciesGeneLoader.load("peach");
    /** 垂柳:近水硬门槛、渐下垂枝;青丝垂柳:旱柳 = 3:1 */
    public static final TreeSpecies WILLOW = SpeciesGeneLoader.load("willow");
    /** 劲松:主干形全高、轮生层性、水平塔盘枝;劲松:矮松 = 2:1 */
    public static final TreeSpecies PINE = SpeciesGeneLoader.load("pine");
    /** 寒梅:矮桩疏枝、耐寒、顶腋侧腋贴枝着花;寒梅:绿萼 = 2:1 */
    public static final TreeSpecies PLUM = SpeciesGeneLoader.load("plum");
    /** 银杏:慢生长寿、高主干开张;雄株开张:塔型 = 2:1 */
    public static final TreeSpecies GINKGO = SpeciesGeneLoader.load("ginkgo");

    /** 按档案 id 查树种；未登记返回 null。（模型生成器按 id 解析树种贴图） */
    public static TreeSpecies byId(String id) {
        return BY_ID.get(id);
    }

    /** 全部已登记树种 */
    public static TreeSpecies[] all() {
        return new TreeSpecies[]{PEACH, WILLOW, PINE, PLUM, GINKGO};
    }

    private static final java.util.Map<String, TreeSpecies> BY_ID = new java.util.HashMap<>();

    static {
        for (TreeSpecies s : all()) {
            BY_ID.put(s.id(), s);
        }
    }

    private Trees() {
    }
}
