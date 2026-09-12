package com.lidao.moran.systems.trees;

/**
 * 树种注册表：每种树一份 {@link TreeSpecies} 档案。
 * 新增树种在此登记，并在 BlockSystem 注册对应的枝干/花苞方块实例后 bind。
 */
public final class Trees {

    /** 桃树：高大主干、上半部萌芽、花芽着生一年生枝顶与侧腋、先花后叶 */
    public static final TreeSpecies PEACH = TreeSpecies.builder("peach")
            .biologicalTop(7)
            .topBudGrowth(5)
            .maxBuds(4)
            .branchMaxGrowth(7)
            .branchStopGrowth(4)
            .branchStopChance(0.25F)
            .subBranchChance(0.25F)
            .growChance(0.5F)
            .minLight(9)
            .temperature(0.3F, 1.2F)
            .build();

    private Trees() {
    }
}
