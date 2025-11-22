package com.lidao.moran.systems.worldgen;

import com.lidao.moran.MoranMod;
import com.lidao.moran.dimensions.DimensionRegistry;
import com.lidao.moran.dimensions.base.BaseDimension;
import com.lidao.moran.dimensions.peach_blossom.PeachBiomeSource;
import com.lidao.moran.dimensions.peach_blossom.PeachChunkGenerator;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

public class WorldGenSystem {

    public static void initialize() {
        System.out.println("🌍 注册自定义世界生成组件");

        try {
            // 注册生物群系源
            Registry.register(Registries.BIOME_SOURCE,
                    new Identifier(MoranMod.MOD_ID, "peach_blossom"),
                    PeachBiomeSource.CODEC
            );

            // 注册区块生成器
            Registry.register(Registries.CHUNK_GENERATOR,
                    new Identifier(MoranMod.MOD_ID, "peach_blossom"),
                    PeachChunkGenerator.CODEC
            );

            System.out.println("✅ 世界生成系统初始化完成");
        } catch (Exception e) {
            System.err.println("❌ 世界生成系统初始化失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}