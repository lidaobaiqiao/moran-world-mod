package com.lidao.moran.core.terrablender;

import com.lidao.moran.MoranMod;
import com.lidao.moran.core.DependencyManager;
import com.lidao.moran.dimensions.peach_blossom.PeachBlossomBiomes;
import terrablender.api.TerraBlenderApi;

/**
 * 第五步：最基础的TerraBlender连接测试
 */
public class TerrablenderIntegration implements TerraBlenderApi {
    
    @Override
    public void onTerraBlenderInitialized() {
        if (!DependencyManager.isModLoaded(DependencyManager.TERRABLENDER)) {
            MoranMod.LOGGER.error("❌ Terrablender 未加载，跳过生物群系注册");
            return;
        }
        
        MoranMod.LOGGER.info("🌍 TerraBlender 已初始化 - 第五步：基础连接测试");
        MoranMod.LOGGER.info("🌸 妖灼华原生物群系Key: " + PeachBlossomBiomes.YAOZHUOHUA.getValue());
        MoranMod.LOGGER.info("🎯 TerraBlender版本连接成功！");
        MoranMod.LOGGER.info("📝 准备在后续步骤中学习正确的API用法");
        
        // 暂时不进行复杂API调用，确保基础连接正常
        MoranMod.LOGGER.info("✅ TerraBlender基础集成完成！");
    }
}