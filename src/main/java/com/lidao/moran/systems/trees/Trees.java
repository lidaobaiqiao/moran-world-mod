package com.lidao.moran.systems.trees;

/**
 * 树种注册表：每种树一份 {@link TreeSpecies} 档案。
 * 新增树种在此登记，并在 BlockSystem 注册对应的枝干/花苞方块实例后 bind。
 */
public final class Trees {

    /** 桃树：高大（8-12 米）、上半部萌芽、花芽着生一年生枝顶与侧腋、耐旱怕涝（排水敏感） */
    public static final TreeSpecies PEACH = TreeSpecies.builder("peach")
            .biologicalTop(8, 12)
            .toppingChance(0.25F)
            .topBudGrowth(5)
            .maxBuds(6)
            .budChanceDenom(4)
            .branchMaxGrowth(7)
            .branchStopGrowth(4)
            .branchStopChance(0.25F)
            .subBranchChance(0.25F)
            .growChance(0.5F)
            .minLight(9)
            .temperature(0.3F, 1.2F)
            .minHydration(0)
            .minHumidity(0.0F)
            .soilPreference(4, 8, 3, 8, 2, 6)
            .pruneResponseChance(0.5F)
            .build();

    private Trees() {
    }
}
