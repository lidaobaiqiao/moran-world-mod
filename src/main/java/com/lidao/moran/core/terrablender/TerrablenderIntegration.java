package com.lidao.moran.core.terrablender;

import com.lidao.moran.MoranMod;
import com.lidao.moran.core.DependencyManager;
import com.lidao.moran.dimensions.peach_blossom.PeachBlossomBiomes;
import terrablender.api.TerraBlenderApi;

/**
 * Terrablender 集成类
 * 第一步：只确认TerraBlender可用，不注册具体生物群系
 */
public class TerrablenderIntegration implements TerraBlenderApi {
    
    @Override
    public void onTerraBlenderInitialized() {
        if (!DependencyManager.isModLoaded(DependencyManager.TERRABLENDER)) {
            MoranMod.LOGGER.error("❌ Terrablender 未加载，跳过生物群系注册");
            return;
        }
        
        MoranMod.LOGGER.info("🌍 TerraBlender 已初始化 - 第一步测试");
        MoranMod.LOGGER.info("🌸 妖灼华原生物群系Key: " + PeachBlossomBiomes.YAOZHUOHUA.getValue());
        MoranMod.LOGGER.info("✅ TerraBlender 集成基础测试完成！");
    }
}