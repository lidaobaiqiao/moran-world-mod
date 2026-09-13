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

    private Trees() {
    }
}
