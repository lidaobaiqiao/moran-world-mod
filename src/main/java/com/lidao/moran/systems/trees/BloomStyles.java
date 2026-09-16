package com.lidao.moran.systems.trees;

import java.util.HashMap;
import java.util.Map;

/**
 * 开花/成熟「风格」注册表——基因文件里 bloomingStyle/maturationStyle 的取值来源。
 *
 * <p>风格是代码(行为注入型性状的实现),基因文件只选名字——用户改配置换形态,
 * 模组作者加代码出新风格。未注册的名字回退中性默认(末端单苞/化叶)。
 */
public final class BloomStyles {

    private static final Map<String, TreeSpecies.BranchStopHook> BLOOM = new HashMap<>();
    private static final Map<String, TreeSpecies.BudMatureHook> MATURE = new HashMap<>();

    static {
        BLOOM.put("peach_axillary", PeachBloom::axillaryStop);
        MATURE.put("peach_canopy", PeachBloom::canopyMature);
    }

    private BloomStyles() {
    }

    /** 按名取开花方式;null = 中性默认(末端单苞) */
    public static TreeSpecies.BranchStopHook blooming(String name) {
        return name == null ? null : BLOOM.get(name);
    }

    /** 按名取成熟形态;null = 中性默认(化为本树种树叶) */
    public static TreeSpecies.BudMatureHook maturation(String name) {
        return name == null ? null : MATURE.get(name);
    }

    public static boolean hasBlooming(String name) {
        return name == null || "neutral".equals(name) || BLOOM.containsKey(name);
    }

    public static boolean hasMaturation(String name) {
        return name == null || "neutral".equals(name) || MATURE.containsKey(name);
    }
}
