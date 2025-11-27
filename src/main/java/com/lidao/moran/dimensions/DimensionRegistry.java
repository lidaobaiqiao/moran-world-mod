package com.lidao.moran.dimensions;

import com.lidao.moran.worldgen.PeachRegion;
import com.lidao.moran.worldgen.PeachSurfaceRules;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import terrablender.api.Regions;
import com.lidao.moran.MoranMod;

import java.util.HashMap;
import java.util.Map;

/**
 * 维度注册表（TerraBlender 兼容版）
 * 分离维度键注册和 TerraBlender 组件注册
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

    /**
     * ✅ 新增：注册 TerraBlender 组件（必须在 onTerraBlenderInitialized 中调用）
     */
    public static void registerTerraBlenderComponents() {
        LOGGER.info("🌍 注册 TerraBlender 组件...");
        registerAllRegions();
        registerAllSurfaceRules();
        LOGGER.info("✅ TerraBlender 组件注册完成");
    }

    private static void registerDimension(String id, Identifier dimensionId) {
        RegistryKey<World> key = RegistryKey.of(RegistryKeys.WORLD, dimensionId);
        DIMENSION_KEYS.put(id, key);
        LOGGER.info("📌 注册维度: {} -> {}", id, dimensionId);
    }

    public static RegistryKey<World> getDimensionKey(String id) {
        return DIMENSION_KEYS.get(id);
    }

    private static void registerAllRegions() {
        // ✅ 只传 1 个参数：Region 实例
        Regions.register(new PeachRegion(new Identifier("moran_mod", "peach_region")));
        LOGGER.info("🗺️ 注册 TerraBlender Region");
    }

    private static void registerAllSurfaceRules() {
        PeachSurfaceRules.register();
        LOGGER.info("🏔️ 注册 TerraBlender SurfaceRule");
    }
}