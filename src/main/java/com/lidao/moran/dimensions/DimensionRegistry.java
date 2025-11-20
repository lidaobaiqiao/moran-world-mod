// src/main/java/com/lidao/moran/dimensions/DimensionRegistry.java
package com.lidao.moran.dimensions;

import com.lidao.moran.dimensions.base.BaseDimension;

import java.util.*;

import static net.minecraft.server.command.ChaseCommand.DIMENSIONS;

/**
 * 维度注册表 - 管理所有8个维度
 */
public class DimensionRegistry {
    private static final Map<String, BaseDimension> DIMENSIONS = new HashMap<>();

    public static void registerDimension(BaseDimension dimension) {
        String id = dimension.getDimensionId();
        DIMENSIONS.put(id, dimension);
        System.out.println("✅ 已注册维度: " + id);
    }

    public static BaseDimension getDimension(String id) {
        return DIMENSIONS.get(id);
    }

    public static Collection<BaseDimension> getAllDimensions() {
        return DIMENSIONS.values();
    }

    // 获取所有已注册的维度ID（用于调试）
    public static Set<String> getRegisteredDimensionIds() {
        return new HashSet<>(DIMENSIONS.keySet());
    }

    public static List<String> getDimensionOrder() {
        return new ArrayList<>(DIMENSIONS.keySet());
    }
}