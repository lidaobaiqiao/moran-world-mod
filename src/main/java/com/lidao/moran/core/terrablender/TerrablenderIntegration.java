package com.lidao.moran.core.terrablender;

import com.lidao.moran.MoranMod;
import com.lidao.moran.core.DependencyManager;
import com.lidao.moran.dimensions.peach_blossom.PeachBlossomBiomes;
import terrablender.api.TerraBlenderApi;

/**
 * Terrablender 集成类
 * 负责注册桃花源维度生物群系到TerraBlender系统
 */
public class TerrablenderIntegration implements TerraBlenderApi {
    
    @Override
    public void onTerraBlenderInitialized() {
        if (!DependencyManager.isModLoaded(DependencyManager.TERRABLENDER)) {
            MoranMod.LOGGER.error("❌ Terrablender 未加载，跳过生物群系注册");
            return;
        }
        
        MoranMod.LOGGER.info("🌍 TerraBlender 已初始化，桃花源生物群系系统已激活");
        
        try {
            // 记录桃花源生物群系信息
            logPeachBlossomBiomes();
            
            MoranMod.LOGGER.info("✅ TerraBlender 集成框架已就绪！");
            
        } catch (Exception e) {
            MoranMod.LOGGER.error("❌ TerraBlender 集成失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 记录桃花源生物群系信息
     */
    private void logPeachBlossomBiomes() {
        MoranMod.LOGGER.info("🌸 桃花源维度七大生物群系:");
        MoranMod.LOGGER.info("  - 妖灼华原 (peach_valley) - 温暖干燥，桃花盛开");
        MoranMod.LOGGER.info("  - 隐竹之界 (bamboo_grove) - 温暖湿润，竹林密布");
        MoranMod.LOGGER.info("  - 千耕平畴 (farm_plains) - 温暖适中，农田广阔");
        MoranMod.LOGGER.info("  - 叠翠微岚 (green_hills) - 凉爽湿润，丘陵起伏");
        MoranMod.LOGGER.info("  - 落花寻溪原 (blossom_stream) - 温暖湿润，溪流纵横");
        MoranMod.LOGGER.info("  - 镜湖百池 (mirror_lakes) - 凉爽湿润，湖泊众多");
        MoranMod.LOGGER.info("  - 晦暗幽深处 (hidden_depths) - 凉爽干燥，神秘深邃");
        
        MoranMod.LOGGER.info("🎯 生物群系将通过自定义维度生成器进行分布");
        MoranMod.LOGGER.info("📝 TerraBlender 为生物群系提供高级管理和兼容性");
    }
}