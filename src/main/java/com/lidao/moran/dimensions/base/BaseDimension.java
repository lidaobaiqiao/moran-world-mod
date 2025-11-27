// src/main/java/com/lidao/moran/dimensions/base/BaseDimension.java
package com.lidao.moran.dimensions.base;

import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import terrablender.api.Region;

/**
 * 所有维度的基类 - 为未来8个维度提供统一接口
 */
// 修改 BaseDimension.java - 添加 TerraBlender 桥接
public abstract class BaseDimension {
    public abstract RegistryKey<World> getDimensionKey();

    public abstract String getDimensionId();

    // 新增：每个维度必须提供自己的 Region
    public abstract Region createRegion();

    // 新增：每个维度必须提供 SurfaceRule
    public abstract void registerSurfaceRules();

}