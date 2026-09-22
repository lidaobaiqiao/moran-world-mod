package com.lidao.moran.core.config;

import com.lidao.moran.MoranMod;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;

/**
 * 配置管理器
 * 负责初始化和管理 Cloth Config 配置系统
 */
public class ConfigManager {
    
    private static ModConfig config;
    
    /**
     * 初始化配置系统
     */
    public static void initialize() {
        try {
            // 注册配置类
            AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
            
            // 获取配置实例
            config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
            
            MoranMod.LOGGER.info("📝 Cloth Config 配置系统初始化完成");
            
        } catch (Exception e) {
            MoranMod.LOGGER.error("❌ 配置系统初始化失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取配置实例
     */
    public static ModConfig getConfig() {
        if (config == null) {
            // 如果配置未初始化，尝试重新初始化
            initialize();
        }
        return config;
    }
    
    /**
     * 保存配置
     */
    public static void saveConfig() {
        if (AutoConfig.getConfigHolder(ModConfig.class) != null) {
            AutoConfig.getConfigHolder(ModConfig.class).save();
            MoranMod.LOGGER.info("💾 配置已保存");
        }
    }
    
    /**
     * 重新加载配置
     */
    public static void reloadConfig() {
        if (AutoConfig.getConfigHolder(ModConfig.class) != null) {
            AutoConfig.getConfigHolder(ModConfig.class).load();
            MoranMod.LOGGER.info("🔄 配置已重新加载");
        }
    }
    
    /**
     * 检查维度是否启用
     */
    public static boolean isDimensionEnabled(String dimensionName) {
        ModConfig config = getConfig();
        if (config == null) return true; // 默认启用
        
        switch (dimensionName.toLowerCase()) {
            case "peach_blossom":
                return config.dimensions.enablePeachBlossom;
            case "wind_realm":
                return config.dimensions.enableWindRealm;
            case "sky_realm":
                return config.dimensions.enableSkyRealm;
            case "underground_realm":
                return config.dimensions.enableUndergroundRealm;
            case "ice_realm":
                return config.dimensions.enableIceRealm;
            case "fire_realm":
                return config.dimensions.enableFireRealm;
            case "thunder_realm":
                return config.dimensions.enableThunderRealm;
            case "mystery_realm":
                return config.dimensions.enableMysteryRealm;
            default:
                return config.dimensions.enableAllDimensions;
        }
    }
    
    /**
     * 获取生物群系配置
     */
    public static int getBiomesPerDimension() {
        ModConfig config = getConfig();
        return config != null ? config.biomes.biomesPerDimension : 24;
    }
    
    /**
     * 检查是否启用自定义地形
     */
    public static boolean isCustomTerrainEnabled() {
        ModConfig config = getConfig();
        return config != null ? config.terrain.enableCustomTerrain : true;
    }
    
    /**
     * 获取地形高度倍数
     */
    public static double getTerrainHeightMultiplier() {
        ModConfig config = getConfig();
        return config != null ? config.terrain.terrainHeightMultiplier : 1.0;
    }
    
    /**
     * 检查是否启用调试日志
     */
    public static boolean isDebugLoggingEnabled() {
        ModConfig config = getConfig();
        return config != null ? config.debug.enableDebugLogging : false;
    }
}
