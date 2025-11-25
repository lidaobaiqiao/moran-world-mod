package com.lidao.moran.core;

import com.lidao.moran.MoranMod;
import net.fabricmc.loader.api.FabricLoader;

/**
 * 前置模组管理器
 * 负责检查和管理所有必需的前置模组
 */
public class DependencyManager {
    
    // 前置模组ID常量
    public static final String TERRABLENDER = "terrablender";
    public static final String GECKOLIB = "geckolib";
    public static final String CLOTH_CONFIG = "cloth-config";
    public static final String ARCHITECTURY = "architectury";
    
    /**
     * 检查所有必需的前置模组是否已加载
     */
    public static boolean checkDependencies() {
        boolean allLoaded = true;
        
        MoranMod.LOGGER.info("🔍 检查前置模组依赖...");
        
        // 检查 Terrablender
        if (isModLoaded(TERRABLENDER)) {
            MoranMod.LOGGER.info("✅ Terrablender 已加载 - 生物群系管理可用");
        } else {
            MoranMod.LOGGER.error("❌ Terrablender 未加载 - 无法使用自定义生物群系功能");
            allLoaded = false;
        }
        
        // 检查 Geckolib
        if (isModLoaded(GECKOLIB)) {
            MoranMod.LOGGER.info("✅ Geckolib 已加载 - 高级地形控制可用");
        } else {
            MoranMod.LOGGER.error("❌ Geckolib 未加载 - 无法使用高级地形生成功能");
            allLoaded = false;
        }
        
        // 检查 Cloth Config
        if (isModLoaded(CLOTH_CONFIG)) {
            MoranMod.LOGGER.info("✅ Cloth Config 已加载 - 配置界面可用");
        } else {
            MoranMod.LOGGER.error("❌ Cloth Config 未加载 - 无法使用配置界面");
            allLoaded = false;
        }
        
        // 检查 Architectury
        if (isModLoaded(ARCHITECTURY)) {
            MoranMod.LOGGER.info("✅ Architectury 已加载 - 跨平台API可用");
        } else {
            MoranMod.LOGGER.error("❌ Architectury 未加载 - 部分API功能不可用");
            allLoaded = false;
        }
        
        if (allLoaded) {
            MoranMod.LOGGER.info("🎉 所有前置模组依赖检查通过！");
        } else {
            MoranMod.LOGGER.error("💥 前置模组依赖检查失败，请安装所有必需的前置模组");
        }
        
        return allLoaded;
    }
    
    /**
     * 检查指定模组是否已加载
     */
    public static boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }
    
    /**
     * 获取前置模组版本信息
     */
    public static String getModVersion(String modId) {
        return FabricLoader.getInstance().getModContainer(modId)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("未知版本");
    }
    
    /**
     * 打印所有前置模组的版本信息
     */
    public static void printDependencyVersions() {
        MoranMod.LOGGER.info("📋 前置模组版本信息:");
        
        if (isModLoaded(TERRABLENDER)) {
            MoranMod.LOGGER.info("  - Terrablender: " + getModVersion(TERRABLENDER));
        }
        
        if (isModLoaded(GECKOLIB)) {
            MoranMod.LOGGER.info("  - Geckolib: " + getModVersion(GECKOLIB));
        }
        
        if (isModLoaded(CLOTH_CONFIG)) {
            MoranMod.LOGGER.info("  - Cloth Config: " + getModVersion(CLOTH_CONFIG));
        }
        
        if (isModLoaded(ARCHITECTURY)) {
            MoranMod.LOGGER.info("  - Architectury: " + getModVersion(ARCHITECTURY));
        }
    }
    
    /**
     * 检查是否为开发环境
     */
    public static boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }
}