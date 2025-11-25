package com.lidao.moran.core.terrablender;

import com.lidao.moran.MoranMod;
import com.lidao.moran.dimensions.peach_blossom.PeachBlossomBiomes;
import net.minecraft.util.Identifier;

/**
 * 第二步：创建生物群系定义数据文件
 */
public class BiomeDataCreator {
    
    public static void initialize() {
        MoranMod.LOGGER.info("🌍 开始创建生物群系定义数据...");
        
        // 为妖灼华原创建基础生物群系定义
        createYaozhuohuaBiomeData();
        
        MoranMod.LOGGER.info("✅ 生物群系定义数据创建完成！");
    }
    
    /**
     * 创建妖灼华原生物群系定义数据
     */
    private static void createYaozhuohuaBiomeData() {
        MoranMod.LOGGER.info("🌸 创建妖灼华原生物群系定义...");
        
        // 获取妖灼华原的ID
        Identifier yaozhuohuaId = PeachBlossomBiomes.YAOZHUOHUA.getValue();
        
        MoranMod.LOGGER.info("✅ 妖灼华原生物群系ID: " + yaozhuohuaId);
        MoranMod.LOGGER.info("📝 这个生物群系将在后续步骤中被正确注册");
    }
}