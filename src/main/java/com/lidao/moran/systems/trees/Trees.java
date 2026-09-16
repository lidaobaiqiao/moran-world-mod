package com.lidao.moran.systems.trees;

/**
 * 树种注册表：每种树一份基因档案。
 * 档案由基因文件驱动(config/moran_mod/genes/&lt;id&gt;.json,缺失自动抄出默认),
 * 行为风格注册在 BloomStyles,参数全部文件可调——
 * 新增树种 = 写一份基因文件 + 此处登记一行 + 注册方块。
 */
public final class Trees {

    /** 桃树:矮桩开心形(6-8格)、顶腋侧腋着花、先花后叶带顶冠、盛花小花团 */
    public static final TreeSpecies PEACH = com.lidao.moran.systems.trees.genes.SpeciesGeneLoader.load("peach");

    /**
     * 按档案 id 查树种；未登记返回 null。
     *
     * 模型生成器用它从模型 id（{@code branch/<species>/<facing>_g<n>}）里解析树种，
     * 再取该树种提交的贴图 —— 这样贴图跟着档案走，引擎不掺和任何树种外观。
     */
    public static TreeSpecies byId(String id) {
        return BY_ID.get(id);
    }

    /** 全部已登记树种（新增树种时把实例加进这里） */
    public static TreeSpecies[] all() {
        return new TreeSpecies[]{PEACH};
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
