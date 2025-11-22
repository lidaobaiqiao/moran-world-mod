package com.lidao.moran.core.event;

import com.lidao.moran.dimensions.peach_blossom.PeachBlossomDimension;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameRules;
public class WorldEventListener {

    public static void initialize() {
        // 监听世界加载
        ServerWorldEvents.LOAD.register((server, world) -> {
            if (world.getRegistryKey().equals(PeachBlossomDimension.DIMENSION_KEY)) {
                setupPeachBlossomWorld(world);
            }
        });

        System.out.println("✅ 世界事件监听器已初始化");
    }

    /**
     * 设置桃花源世界
     */
    private static void setupPeachBlossomWorld(ServerWorld world) {
        // 设置友好的游戏规则
        world.getGameRules().get(net.minecraft.world.GameRules.DO_TILE_DROPS).set(true, world.getServer());
        world.getGameRules().get(net.minecraft.world.GameRules.DO_MOB_GRIEFING).set(false, world.getServer());
        world.getGameRules().get(net.minecraft.world.GameRules.DO_ENTITY_DROPS).set(true, world.getServer());
        world.getGameRules().get(net.minecraft.world.GameRules.DO_MOB_LOOT).set(true, world.getServer());
        System.out.println("🌸 桃花源世界设置完成");
    }
}