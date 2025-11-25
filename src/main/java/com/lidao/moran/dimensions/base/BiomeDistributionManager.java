// src/main/java/com/lidao/moran/dimensions/base/BiomeDistributionManager.java
package com.lidao.moran.dimensions.base;

import com.lidao.moran.dimensions.peach_blossom.PeachBlossomBiomes;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.noise.DoublePerlinNoiseSampler;
import net.minecraft.util.math.random.Random;

/**
 * 生物群系分布管理器 - 基于优化后的妖灼华原生物群系
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
     * 计算生物群系权重 - 基于优化后的妖灼华原气候参数
     */
    public double calculateBiomeWeight(int x, int z, String biomeType) {
        // 获取当前位置的环境参数
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

    /**
     * 获取妖灼华原的目标气候参数 - 基于优化配置
     */
    public PeachBlossomBiomes.YaozhuohuaClimateParameters getTargetClimate() {
        return PeachBlossomBiomes.getClimateParameters();
    }

    /**
     * 计算妖灼华原的生成适应性 - 基于优化后的气候参数
     */
    public double calculatePeachForestSuitability(double temp, double humidity, double continental) {
        PeachBlossomBiomes.YaozhuohuaClimateParameters target = getTargetClimate();
        
        // 计算与目标气候的差异
        double tempDiff = Math.abs(temp - target.temperature);
        double humidityDiff = Math.abs(humidity - target.humidity);
        double continentalDiff = Math.abs(continental - target.continentalness);
        
        // 总体适应性（差异越小适应性越高）
        double suitability = 1.0 - (tempDiff + humidityDiff + continentalDiff) / 3.0;
        
        return MathHelper.clamp(suitability, 0.0, 1.0);
    }

    private double getTemperature(int x, int z) {
        // 将噪声值映射到[-1, 1]范围
        return MathHelper.clamp((temperatureNoise.sample(x * 0.002, 0, z * 0.002) + 1) * 0.5 * 2 - 1, -1, 1);
    }

    private double getHumidity(int x, int z) {
        return MathHelper.clamp((humidityNoise.sample(x * 0.002, 100, z * 0.002) + 1) * 0.5 * 2 - 1, -1, 1);
    }

    private double getContinentalness(int x, int z) {
        return MathHelper.clamp((continentalNoise.sample(x * 0.001, 200, z * 0.001) + 1) * 0.5 * 2 - 1, -1, 1);
    }

    // 各个生物群系的权重计算函数 - 基于优化后的气候参数
    private double calculatePeachForestWeight(double temp, double humidity, double continental) {
        // 使用优化后的妖灼华原气候参数
        double suitability = calculatePeachForestSuitability(temp, humidity, continental);
        
        // 妖灼华原偏好：温暖干燥的内陆地区
        double tempPref = temp > 0 ? MathHelper.clamp(temp, 0, 1) : 0;
        double humidityPref = humidity < 0 ? MathHelper.clamp(-humidity, 0, 1) : 0;
        double continentalPref = continental > 0.5 ? MathHelper.clamp((continental - 0.5) * 2, 0, 1) : 0;
        
        // 综合权重（适应性 + 环境偏好）
        double weight = suitability * 0.7 + (tempPref + humidityPref + continentalPref) / 3.0 * 0.3;
        
        return MathHelper.clamp(weight * weight, 0, 1); // 平方使分布更集中
    }

    private double calculateBambooForestWeight(double temp, double humidity, double continental) {
        // 竹林喜欢高湿度、边缘地区
        double weight = MathHelper.clamp(humidity * 0.9 + (1 - Math.abs(continental)) * 0.8, 0, 1);
        return weight;
    }

    private double calculateFarmlandWeight(double temp, double humidity, double continental) {
        // 农田喜欢平坦的内陆地区
        double weight = MathHelper.clamp(Math.abs(continental) * 0.9 + temp * 0.7, 0, 1);
        return weight;
    }

    private double calculateHillsWeight(double temp, double humidity, double continental) {
        // 丘陵喜欢中等大陆性
        double weight = MathHelper.clamp(1 - Math.abs(continental), 0, 1);
        return weight;
    }

    private double calculateRiverWeight(double temp, double humidity, double continental) {
        // 河流基于低continentalness值
        double weight = MathHelper.clamp((1 - Math.abs(continental)) * 0.8 + humidity * 0.5, 0, 1);
        return weight;
    }

    private double calculateLakeWeight(double temp, double humidity, double continental) {
        // 湖泊基于低洼地区
        double weight = MathHelper.clamp((1 - Math.abs(continental)) * 0.6 + humidity * 0.8, 0, 1);
        return weight;
    }
}