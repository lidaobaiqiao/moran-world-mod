// src/main/java/com/lidao/moran/dimensions/base/TerrainGenerator.java
package com.lidao.moran.dimensions.base;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.noise.DoublePerlinNoiseSampler;
import net.minecraft.util.math.random.Random;

/**
 * 桃花源维度地形生成器
 * 使用多噪声层叠加技术生成复杂地形
 */
public class TerrainGenerator {

    private final DoublePerlinNoiseSampler baseNoise;
    private final DoublePerlinNoiseSampler hillNoise;
    private final DoublePerlinNoiseSampler detailNoise;
    private final DoublePerlinNoiseSampler ridgeNoise;

    public TerrainGenerator(Random random) {
        this.baseNoise = DoublePerlinNoiseSampler.create(random, 8, 1.0);
        this.hillNoise = DoublePerlinNoiseSampler.create(random, 6, 0.8);
        this.detailNoise = DoublePerlinNoiseSampler.create(random, 4, 0.5);
        this.ridgeNoise = DoublePerlinNoiseSampler.create(random, 3, 0.3);
    }

    /**
     * 生成地形高度
     */
    public double generateTerrainHeight(int x, int z, double biomeWeight) {
        // 基础地形
        double baseHeight = baseNoise.sample(
                x * TerrainNoiseConfig.BaseTerrain.BASE_SCALE,
                0,
                z * TerrainNoiseConfig.BaseTerrain.BASE_SCALE
        ) * TerrainNoiseConfig.BaseTerrain.BASE_AMPLITUDE;

        // 山丘地形
        double hillHeight = hillNoise.sample(
                x * TerrainNoiseConfig.Hills.SCALE,
                0,
                z * TerrainNoiseConfig.Hills.SCALE
        ) * TerrainNoiseConfig.Hills.AMPLITUDE;

        // 细节地形
        double detailHeight = detailNoise.sample(
                x * TerrainNoiseConfig.Detail.SCALE,
                0,
                z * TerrainNoiseConfig.Detail.SCALE
        ) * TerrainNoiseConfig.Detail.AMPLITUDE;

        // 山脊特征
        double ridgeHeight = Math.abs(ridgeNoise.sample(
                x * 0.015,
                0,
                z * 0.015
        )) * TerrainNoiseConfig.Detail.RIDGE_WEIGHT * 20;

        // 噪声层叠加
        double height = baseHeight + hillHeight + detailHeight + ridgeHeight;

        // 应用生物群系权重
        height = applyBiomeInfluence(height, biomeWeight);

        // 平滑处理
        return smoothTerrain(height);
    }

    /**
     * 应用生物群系影响
     */
    private double applyBiomeInfluence(double height, double biomeWeight) {
        // 根据生物群系权重调整地形特征
        double adjustedHeight = height * (0.8 + biomeWeight * 0.4);

        // 确保高度在合理范围内
        return MathHelper.clamp(adjustedHeight, -64, 320);
    }

    /**
     * 地形平滑处理
     */
    private double smoothTerrain(double height) {
        // 使用平滑函数减少尖锐变化
        return height * (1 - TerrainNoiseConfig.BaseTerrain.ROUGHNESS) +
                Math.sin(height * 0.1) * TerrainNoiseConfig.BaseTerrain.ROUGHNESS * 10;
    }

    /**
     * 生成河流和湖泊地形
     */
    public double generateWaterFeatures(int x, int z, double baseHeight) {
        // 河流噪声
        double riverNoise = baseNoise.sample(
                x * 0.01,
                100,
                z * 0.01
        );

        // 如果处于河流区域，降低地形高度
        if (Math.abs(riverNoise) < TerrainNoiseConfig.WaterFeatures.RIVER_THRESHOLD) {
            double riverDepth = (TerrainNoiseConfig.WaterFeatures.RIVER_THRESHOLD - Math.abs(riverNoise)) * 20;
            return baseHeight - riverDepth;
        }

        // 湖泊生成
        double lakeNoise = hillNoise.sample(
                x * 0.005,
                200,
                z * 0.005
        );

        if (Math.abs(lakeNoise) < TerrainNoiseConfig.WaterFeatures.LAKE_THRESHOLD) {
            double lakeDepth = (TerrainNoiseConfig.WaterFeatures.LAKE_THRESHOLD - Math.abs(lakeNoise)) * 30;
            return baseHeight - lakeDepth;
        }

        return baseHeight;
    }
}
