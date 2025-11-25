package com.lidao.moran.systems.worldgen;

import com.lidao.moran.MoranMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.world.ServerWorld;

public class WorldGenSystem {

    public static void initialize() {
        System.out.println("🌍 世界生成系统初始化");

        try {
            // 测试模式：使用原版生成器，不注册自定义组件
            System.out.println("✅ 测试模式：世界生成系统初始化完成（使用原版生成器）");
        } catch (Exception e) {
            System.err.println("❌ 世界生成系统初始化失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}