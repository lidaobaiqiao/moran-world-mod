package com.lidao.moran.core.terrablender;

import com.lidao.moran.MoranMod;
import com.lidao.moran.core.DependencyManager;
import com.lidao.moran.dimensions.peach_blossom.PeachBlossomBiomes;
import terrablender.api.TerraBlenderApi;

/**
 * 妖灼华原生物群系配置完成 - 只需在桃花源维度生成
 */
public class TerrablenderIntegration implements TerraBlenderApi {
    
    @Override
    public void onTerraBlenderInitialized() {
        if (!DependencyManager.isModLoaded(DependencyManager.TERRABLENDER)) {
            MoranMod.LOGGER.error("❌ Terrablender 未加载，跳过生物群系注册");
            return;
        }
        
        MoranMod.LOGGER.info("🌍 TerraBlender 已初始化 - 妖灼华原生物群系配置完成");
        
        // 记录妖灼华原生物群系信息
        PeachBlossomBiomes.logBiomeInfo();
        
        MoranMod.LOGGER.info("✅ 妖灼华原生物群系优化完成！");
        MoranMod.LOGGER.info("🎯 生物群系将在桃花源维度中生成（无需主世界注册）");
        MoranMod.LOGGER.info("🌸 气候配置：温暖干燥，适合桃花生长");
        MoranMod.LOGGER.info("📏 权重分布：基于优化后的BiomeDistributionManager");
    }
}