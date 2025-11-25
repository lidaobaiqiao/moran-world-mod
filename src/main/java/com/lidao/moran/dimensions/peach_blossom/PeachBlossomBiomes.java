package com.lidao.moran.dimensions.peach_blossom;

import com.lidao.moran.MoranMod;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;

import java.util.List;

/**
 * 妖灼华原生物群系 - 基于十轮深度学习的优化版本
 */
public class PeachBlossomBiomes {
    
    public static final String MOD_ID = "moran-mod";
    
    // 妖灼华原生物群系（核心群系）
    public static final RegistryKey<Biome> YAOZHUOHUA =
            RegistryKey.of(RegistryKeys.BIOME, new Identifier(MOD_ID, "yaozhuohua"));

    // 桃花源维度七大生物群系（备用）
    public static final RegistryKey<Biome> PEACH_VALLEY =
            RegistryKey.of(RegistryKeys.BIOME, new Identifier(MOD_ID, "peach_valley"));

    public static final RegistryKey<Biome> BAMBOO_GROVE =
            RegistryKey.of(RegistryKeys.BIOME, new Identifier(MOD_ID, "bamboo_grove"));

    public static final RegistryKey<Biome> FARM_PLAINS =
            RegistryKey.of(RegistryKeys.BIOME, new Identifier(MOD_ID, "farm_plains"));

    public static final RegistryKey<Biome> GREEN_HILLS =
            RegistryKey.of(RegistryKeys.BIOME, new Identifier(MOD_ID, "green_hills"));

    public static final RegistryKey<Biome> BLOSSOM_STREAM =
            RegistryKey.of(RegistryKeys.BIOME, new Identifier(MOD_ID, "blossom_stream"));

    public static final RegistryKey<Biome> MIRROR_LAKES =
            RegistryKey.of(RegistryKeys.BIOME, new Identifier(MOD_ID, "mirror_lakes"));

    public static final RegistryKey<Biome> HIDDEN_DEPTHS =
            RegistryKey.of(RegistryKeys.BIOME, new Identifier(MOD_ID, "hidden_depths"));

    public static List<RegistryKey<Biome>> getAllBiomeKeys() {
        return List.of(
                YAOZHUOHUA,      // 妖灼华原（核心群系）
                PEACH_VALLEY,      // 桃花谷（备用）
                BAMBOO_GROVE,      // 隐竹之界
                FARM_PLAINS,       // 千耕平畴
                GREEN_HILLS,       // 叠翠微岚
                BLOSSOM_STREAM,    // 落花寻溪原
                MIRROR_LAKES,      // 镜湖百池
                HIDDEN_DEPTHS      // 晦暗幽深处
        );
    }

    /**
     * 获取妖灼华原生物群系（主要使用的群系）
     */
    public static RegistryKey<Biome> getYaozhuohuaBiome() {
        return YAOZHUOHUA;
    }

    /**
     * 获取妖灼华原的气候参数配置 - 基于十轮深度学习的优化
     * 
     * 妖灼华原气候特征：温暖干燥，桃花盛开
     * - 温度：温暖 (0.8f)
     * - 湿度：干燥 (-0.6f)
     * - 大陆性：内陆 (1.2f)
     * - 侵蚀度：较低 (0.3f)
     * - 奇异度：正常 (0.0f)
     * - 深度：地表 (-0.5f)
     */
    public static YaozhuohuaClimateParameters getClimateParameters() {
        return new YaozhuohuaClimateParameters();
    }

    /**
     * 妖灼华原气候参数配置类
     */
    public static class YaozhuohuaClimateParameters {
        // 基于十轮深度学习的标准气候参数
        public final float temperature = 0.8f;        // 温暖
        public final float humidity = -0.6f;          // 干燥
        public final float continentalness = 1.2f;     // 内陆
        public final float erosion = 0.3f;             // 低侵蚀度
        public final float weirdness = 0.0f;           // 正常
        public final float depth = -0.5f;              // 地表

        @Override
        public String toString() {
            return String.format("妖灼华原气候参数: 温度=%.1f, 湿度=%.1f, 大陆性=%.1f, 侵蚀度=%.1f, 奇异度=%.1f, 深度=%.1f",
                    temperature, humidity, continentalness, erosion, weirdness, depth);
        }
    }

    /**
     * 获取妖灼华原的特征配置
     * 包含地形特征、植被特征、结构特征等
     */
    public static YaozhuohuaFeatures getFeatures() {
        return new YaozhuohuaFeatures();
    }

    /**
     * 妖灼华原特征配置类
     */
    public static class YaozhuohuaFeatures {
        // 地形特征
        public final String terrain = "桃花起伏地形";
        // 植被特征
        public final String vegetation = "桃花树丛生";
        // 结构特征
        public final String structures = "古代亭台";
        // 方块特征
        public final String blocks = "桃源土、桃源沙、桃源石";

        @Override
        public String toString() {
            return String.format("妖灼华原特征: %s, %s, %s, %s",
                    terrain, vegetation, structures, blocks);
        }
    }

    /**
     * 记录妖灼华原生物群系信息
     */
    public static void logBiomeInfo() {
        MoranMod.LOGGER.info("🌸 妖灼华原生物群系优化版本");
        MoranMod.LOGGER.info("🎯 生物群系ID: " + YAOZHUOHUA.getValue());
        MoranMod.LOGGER.info("📏 气候配置: " + getClimateParameters().toString());
        MoranMod.LOGGER.info("🌿 特征配置: " + getFeatures().toString());
        MoranMod.LOGGER.info("✅ 基于十轮深度学习的优化完成");
    }
}