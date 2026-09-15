package com.lidao.moran.systems.trees;

/**
 * 树种注册表：每种树一份档案。
 * 树种以 TreeSpecies 子类的形式定制（范式覆写钩子、参数走 Builder），
 * 在 BlockSystem 注册对应枝干/花苞方块实例后 bind。
 *
 * 新增树种 = 建子类（参照 PeachSpecies）+ 此处登记 + 注册方块。
 */
public final class Trees {

    /** 桃树：高大（环境评分定高 8-12）、密集侧芽、花芽顶腋着生、先花后叶带顶冠 */
    public static final PeachSpecies PEACH = PeachSpecies.peach();

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
