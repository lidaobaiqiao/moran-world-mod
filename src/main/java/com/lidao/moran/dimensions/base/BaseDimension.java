// src/main/java/com/lidao/moran/dimensions/base/BaseDimension.java
package com.lidao.moran.dimensions.base;

import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

/**
 * 所有维度的基类 - 为未来8个维度提供统一接口
 */
public abstract class BaseDimension {
    public abstract RegistryKey<World> getDimensionKey();
    public abstract String getDimensionId();
    public abstract void onPlayerEnter(ServerPlayerEntity player);
    public abstract void onPlayerLeave(ServerPlayerEntity player);

    // 预留方法 - 为未来维度扩展
    public void registerBiomes() {}
    public void registerFeatures() {}
    public void registerStructures() {}
}