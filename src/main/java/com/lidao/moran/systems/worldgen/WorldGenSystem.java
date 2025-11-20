// src/main/java/com/lidao/moran/systems/worldgen/WorldGenSystem.java
package com.lidao.moran.systems.worldgen;

import com.lidao.moran.dimensions.DimensionRegistry;
import com.lidao.moran.dimensions.base.BaseDimension;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

public class WorldGenSystem {

    public static void initialize() {
        // 监听世界加载事件，为每个维度设置游戏规则
        ServerWorldEvents.LOAD.register((server, world) -> {
            setupDimensionRules(world);
        });

        System.out.println("✅ 世界生成系统初始化完成");
    }

    private static void setupDimensionRules(ServerWorld world) {
        Identifier worldId = world.getRegistryKey().getValue();
        String dimensionId = worldId.getPath();

        BaseDimension dimension = DimensionRegistry.getDimension(dimensionId);
        if (dimension != null) {
            // 为所有墨世界维度设置统一的友好规则
            world.getGameRules().get(net.minecraft.world.GameRules.DO_TILE_DROPS).set(true, world.getServer());
            world.getGameRules().get(net.minecraft.world.GameRules.DO_MOB_GRIEFING).set(true, world.getServer());
            world.getGameRules().get(net.minecraft.world.GameRules.DO_ENTITY_DROPS).set(true, world.getServer());
            world.getGameRules().get(net.minecraft.world.GameRules.DO_MOB_LOOT).set(true, world.getServer());

            System.out.println("✅ 已设置维度规则: " + dimensionId);
        }
    }
}
