// src/main/java/com/lidao/moran/dimensions/DimensionRegistry.java
package com.lidao.moran.dimensions;

import com.lidao.moran.dimensions.base.BaseDimension;
import com.lidao.moran.dimensions.peach_blossom.PeachBlossomDimension;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.World;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 维度注册表 - 管理所有8个维度
 * 注意：此类的“注册”是指内部管理，而非向Minecraft注册表注册。
 * 真正的维度注册由数据文件 完成。
 */
public class DimensionRegistry {
    private static final Map<String, BaseDimension> DIMENSIONS = new HashMap<>();

    /**
     * 初始化并实例化所有维度
     * 这个方法应该在模组的 onInitialize 阶段被调用
     */
    public static void initialize() {
        // 实例化并注册桃花源维度
        PeachBlossomDimension peachBlossomDimension = new PeachBlossomDimension();
        registerDimension(peachBlossomDimension);

        // 在这里实例化并注册其他未来的维度
        // registerDimension(new WindRealmDimension());

        System.out.println("✅ 墨世界维度管理器初始化完成，共管理 " + DIMENSIONS.size() + " 个维度。");
    }

    /**
     * 将维度实例添加到内部管理器中
     * @param dimension 维度实例
     */
    public static void registerDimension(BaseDimension dimension) {
        String id = dimension.getDimensionId();
        DIMENSIONS.put(id, dimension);
        System.out.println("📌 已将维度 '" + id + "' 添加到内部管理器。");
    }

    /**
     * 根据ID获取维度实例
     */
    public static BaseDimension getDimension(String id) {
        return DIMENSIONS.get(id);
    }

    /**
     * 根据ID获取维度的RegistryKey
     */
    public static RegistryKey<World> getDimensionKey(String id) {
        BaseDimension dimension = DIMENSIONS.get(id);
        return dimension != null ? dimension.getDimensionKey() : null;
    }

    /**
     * 获取所有已管理的维度实例
     */
    public static Collection<BaseDimension> getAllDimensions() {
        return DIMENSIONS.values();
    }

    /**
     * 获取所有已管理的维度ID（用于调试）
     */
    public static Set<String> getRegisteredDimensionIds() {
        return new HashSet<>(DIMENSIONS.keySet());
    }
}
