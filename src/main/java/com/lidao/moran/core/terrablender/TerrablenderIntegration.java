package com.lidao.moran.core.terrablender;

import com.lidao.moran.MoranMod;
import com.lidao.moran.core.DependencyManager;
import com.lidao.moran.dimensions.peach_blossom.PeachBlossomBiomes;
import terrablender.api.TerraBlenderApi;

/**
 * 十轮深度学习后的妖灼华原生物群系优化版本
 */
public class TerrablenderIntegration implements TerraBlenderApi {
    
    @Override
    public void onTerraBlenderInitialized() {
        if (!DependencyManager.isModLoaded(DependencyManager.TERRABLENDER)) {
            MoranMod.LOGGER.error("❌ Terrablender 未加载，跳过生物群系注册");
            return;
        }
        
        MoranMod.LOGGER.info("🌍 TerraBlender 已初始化 - 妖灼华原生物群系优化版本");
        
        // 记录妖灼华原生物群系信息
        PeachBlossomBiomes.logBiomeInfo();
        
        MoranMod.LOGGER.info("✅ 妖灼华原生物群系优化完成！");
        MoranMod.LOGGER.info("🎯 准备下一步：基于优化后的生物群系进行TerraBlender注册");
    }
}