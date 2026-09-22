package com.lidao.moran.dimensions;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.lidao.moran.MoranMod;

import java.util.HashMap;
import java.util.Map;

/**
 * 桃花源维度键注册表。维度内容由 data/moran_mod/dimension 与 worldgen JSON 定义。
 */
public class DimensionRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(MoranMod.MOD_ID);
    private static final Map<String, RegistryKey<World>> DIMENSION_KEYS = new HashMap<>();

    public static void initialize() {
        LOGGER.info("📦 注册维度管理器...");

        // 注册维度 RegistryKey（可安全地在 onInitialize 中调用）
        registerDimension("peach_blossom", new Identifier("moran_mod", "peach_blossom_dimension"));

        LOGGER.info("✅ 维度管理器就绪，共 {} 个维度", DIMENSION_KEYS.size());
    }

    private static void registerDimension(String id, Identifier dimensionId) {
        RegistryKey<World> key = RegistryKey.of(RegistryKeys.WORLD, dimensionId);
        DIMENSION_KEYS.put(id, key);
        LOGGER.info("📌 注册维度: {} -> {}", id, dimensionId);
    }

    public static RegistryKey<World> getDimensionKey(String id) {
        return DIMENSION_KEYS.get(id);
    }

}
