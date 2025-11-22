// src/main/java/com/lidao/moran/dimensions/base/TerrainNoiseConfig.java
package com.lidao.moran.dimensions.base;

import net.minecraft.util.math.noise.DoublePerlinNoiseSampler;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;

/**
 * 桃花源维度地形噪声配置
 * 负责管理所有地形生成参数
 */
public class TerrainNoiseConfig {

    // 基础地形参数
    public static class BaseTerrain {
        public static final double BASE_SCALE = 0.05;        // 基础缩放
        public static final double BASE_AMPLITUDE = 80.0;    // 基础振幅
        public static final double ROUGHNESS = 0.5;          // 粗糙度
    }

    // 山丘地形参数
    public static class Hills {
        public static final double SCALE = 0.02;             // 山丘缩放
        public static final double AMPLITUDE = 40.0;         // 山丘振幅
        public static final double FREQUENCY = 1.2;          // 山丘频率
    }

    // 细节地形参数
    public static class Detail {
        public static final double SCALE = 0.1;              // 细节缩放
        public static final double AMPLITUDE = 8.0;          // 细节振幅
        public static final double RIDGE_WEIGHT = 0.3;       // 山脊权重
    }

    // 河流和湖泊参数
    public static class WaterFeatures {
        public static final double RIVER_THRESHOLD = 0.1;    // 河流阈值
        public static final double LAKE_THRESHOLD = 0.05;    // 湖泊阈值
        public static final double RIVER_WIDTH = 0.3;        // 河流宽度
    }

    // 生物群系过渡参数
    public static class BiomeBlend {
        public static final double TRANSITION_SMOOTHNESS = 0.3;  // 过渡平滑度
        public static final double EDGE_SHARPNESS = 0.7;         // 边缘锐度
    }
}
