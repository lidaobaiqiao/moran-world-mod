// src/main/java/com/lidao/moran/core/config/ModConfig.java
package com.lidao.moran.core.config;

import com.lidao.moran.MoranMod;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

/**
 * 墨世界模组配置类
 * 使用 Cloth Config 提供配置界面
 */
@Config(name = "moran-mod")
public class ModConfig implements ConfigData {
    
    @ConfigEntry.Gui.Excluded
    public static final int CONFIG_VERSION = 1;
    
    // 维度配置
    @ConfigEntry.Category("dimensions")
    public DimensionConfig dimensions = new DimensionConfig();
    
    // 生物群系配置
    @ConfigEntry.Category("biomes")
    public BiomeConfig biomes = new BiomeConfig();
    
    // 地形生成配置
    @ConfigEntry.Category("terrain")
    public TerrainConfig terrain = new TerrainConfig();
    
    // 性能配置
    @ConfigEntry.Category("performance")
    public PerformanceConfig performance = new PerformanceConfig();
    
    // 调试配置
    @ConfigEntry.Category("debug")
    public DebugConfig debug = new DebugConfig();
    
    // 保留原有的简单配置（向后兼容）
    public static class Teleport {
        public static int RAFT_STATIONARY_SECONDS = 30;
        public static boolean ENABLE_RAFT_TELEPORT = true;
    }
    
    /**
     * 维度配置
     */
    public static class DimensionConfig {
        @ConfigEntry.Gui.Tooltip
        public boolean enableAllDimensions = true;
        
        @ConfigEntry.Gui.Tooltip
        public boolean enablePeachBlossom = true;
        
        // 8个维度的开关
        @ConfigEntry.Gui.Tooltip
        public boolean enableWindRealm = true;
        
        @ConfigEntry.Gui.Tooltip
        public boolean enableSkyRealm = true;
        
        @ConfigEntry.Gui.Tooltip
        public boolean enableUndergroundRealm = true;
        
        @ConfigEntry.Gui.Tooltip
        public boolean enableIceRealm = true;
        
        @ConfigEntry.Gui.Tooltip
        public boolean enableFireRealm = true;
        
        @ConfigEntry.Gui.Tooltip
        public boolean enableThunderRealm = true;
        
        @ConfigEntry.Gui.Tooltip
        public boolean enableMysteryRealm = true;
    }
    
    /**
     * 生物群系配置
     */
    public static class BiomeConfig {
        @ConfigEntry.Gui.Tooltip
        public boolean enableCustomBiomes = true;
        
        @ConfigEntry.Gui.Tooltip
        public int biomesPerDimension = 24;
        
        @ConfigEntry.Gui.Tooltip
        public boolean enableBiomeTransitions = true;
        
        @ConfigEntry.Gui.Tooltip
        public double biomeTransitionSize = 1.0;
        
        @ConfigEntry.Gui.Tooltip
        public boolean enableBiomeVariants = true;
    }
    
    /**
     * 地形生成配置
     */
    public static class TerrainConfig {
        @ConfigEntry.Gui.Tooltip
        public boolean enableCustomTerrain = true;
        
        @ConfigEntry.Gui.Tooltip
        public double terrainHeightMultiplier = 1.0;
        
        @ConfigEntry.Gui.Tooltip
        public double terrainRoughness = 1.0;
        
        @ConfigEntry.Gui.Tooltip
        public boolean enableAdvancedNoise = true;
        
        @ConfigEntry.Gui.Tooltip
        public int noiseOctaves = 4;
        
        @ConfigEntry.Gui.Tooltip
        public double noiseScale = 0.02;
        
        @ConfigEntry.Gui.Tooltip
        public boolean enableCaveGeneration = true;
        
        @ConfigEntry.Gui.Tooltip
        public boolean enableOreGeneration = true;
    }
    
    /**
     * 性能配置
     */
    public static class PerformanceConfig {
        @ConfigEntry.Gui.Tooltip
        public int biomeCacheSize = 1024;
        
        @ConfigEntry.Gui.Tooltip
        public int terrainCacheSize = 512;
        
        @ConfigEntry.Gui.Tooltip
        public boolean enableAsyncGeneration = true;
        
        @ConfigEntry.Gui.Tooltip
        public int maxGenerationThreads = 4;
        
        @ConfigEntry.Gui.Tooltip
        public boolean enablePreloading = true;
        
        @ConfigEntry.Gui.Tooltip
        public int preloadRadius = 2;
    }
    
    /**
     * 调试配置
     */
    public static class DebugConfig {
        @ConfigEntry.Gui.Tooltip
        public boolean enableDebugLogging = false;
        
        @ConfigEntry.Gui.Tooltip
        public boolean enableBiomeDebug = false;
        
        @ConfigEntry.Gui.Tooltip
        public boolean enableTerrainDebug = false;
        
        @ConfigEntry.Gui.Tooltip
        public boolean enablePerformanceMonitoring = false;
        
        @ConfigEntry.Gui.Tooltip
        public boolean enableDebugCommands = true;
        
        @ConfigEntry.Gui.Tooltip
        public boolean showBiomeBorders = false;
        
        @ConfigEntry.Gui.Tooltip
        public boolean showTerrainInfo = false;
    }
    
    /**
     * 加载配置
     */
    public static void loadConfig() {
        MoranMod.LOGGER.info("📝 加载墨世界模组配置...");
        // 配置会通过 Cloth Config 自动加载
    }
    
    /**
     * 保存配置
     */
    public static void saveConfig() {
        MoranMod.LOGGER.info("💾 保存墨世界模组配置...");
        // 配置会通过 Cloth Config 自动保存
    }
}