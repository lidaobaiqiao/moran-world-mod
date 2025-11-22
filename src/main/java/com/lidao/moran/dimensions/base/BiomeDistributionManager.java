// src/main/java/com/lidao/moran/dimensions/base/BiomeDistributionManager.java
package com.lidao.moran.dimensions.base;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.noise.DoublePerlinNoiseSampler;
import net.minecraft.util.math.random.Random;

/**
 * 生物群系分布管理器
 * 负责计算生物群系的权重和分布
 */
public class BiomeDistributionManager {

    private final DoublePerlinNoiseSampler temperatureNoise;
    private final DoublePerlinNoiseSampler humidityNoise;
    private final DoublePerlinNoiseSampler continentalNoise;

    public BiomeDistributionManager(Random random) {
        this.temperatureNoise = DoublePerlinNoiseSampler.create(random, 4, 1.0);
        this.humidityNoise = DoublePerlinNoiseSampler.create(random, 4, 1.0);
        this.continentalNoise = DoublePerlinNoiseSampler.create(random, 3, 0.8);
    }

    /**
     * 计算生物群系权重
     */
    public double calculateBiomeWeight(int x, int z, String biomeType) {
        // 基础环境参数
        double temperature = getTemperature(x, z);
        double humidity = getHumidity(x, z);
        double continentalness = getContinentalness(x, z);

        // 根据生物群系类型计算权重
        switch (biomeType) {
            case "peach_forest": // 妖灼华原
                return calculatePeachForestWeight(temperature, humidity, continentalness);

            case "bamboo_forest": // 隐竹之界
                return calculateBambooForestWeight(temperature, humidity, continentalness);

            case "farmland": // 千耕平畴
                return calculateFarmlandWeight(temperature, humidity, continentalness);

            case "hills": // 叠翠微岚
                return calculateHillsWeight(temperature, humidity, continentalness);

            case "river": // 落花寻溪原
                return calculateRiverWeight(temperature, humidity, continentalness);

            case "lake": // 镜湖百池
                return calculateLakeWeight(temperature, humidity, continentalness);

            default:
                return 0.0;
        }
    }

    private double getTemperature(int x, int z) {
        return (temperatureNoise.sample(x * 0.002, 0, z * 0.002) + 1) * 0.5;
    }

    private double getHumidity(int x, int z) {
        return (humidityNoise.sample(x * 0.002, 100, z * 0.002) + 1) * 0.5;
    }

    private double getContinentalness(int x, int z) {
        return (continentalNoise.sample(x * 0.001, 200, z * 0.001) + 1) * 0.5;
    }

    // 各个生物群系的权重计算函数
    private double calculatePeachForestWeight(double temp, double humidity, double continental) {
        // 桃花林喜欢温暖、中等湿度的内陆地区
        double weight = MathHelper.clamp(temp * 0.8 + humidity * 0.6 + continental * 0.7, 0, 1);
        return weight * weight; // 平方使分布更集中
    }

    private double calculateBambooForestWeight(double temp, double humidity, double continental) {
        // 竹林喜欢高湿度、边缘地区
        double weight = MathHelper.clamp(humidity * 0.9 + (1 - continental) * 0.8, 0, 1);
        return weight;
    }

    private double calculateFarmlandWeight(double temp, double humidity, double continental) {
        // 农田喜欢平坦的内陆地区
        double weight = MathHelper.clamp(continental * 0.9 + temp * 0.7, 0, 1);
        return weight;
    }

    private double calculateHillsWeight(double temp, double humidity, double continental) {
        // 丘陵喜欢中等大陆性
        double weight = MathHelper.clamp(Math.abs(continental - 0.5) * 2, 0, 1);
        return weight;
    }

    private double calculateRiverWeight(double temp, double humidity, double continental) {
        // 河流基于低continentalness值
        double weight = MathHelper.clamp((1 - continental) * 0.8 + humidity * 0.5, 0, 1);
        return weight;
    }

    private double calculateLakeWeight(double temp, double humidity, double continental) {
        // 湖泊基于低洼地区
        double weight = MathHelper.clamp((1 - continental) * 0.6 + humidity * 0.8, 0, 1);
        return weight;
    }
}
