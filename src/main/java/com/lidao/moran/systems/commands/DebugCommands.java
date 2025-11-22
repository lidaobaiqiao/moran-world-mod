// src/main/java/com/lidao/moran/systems/commands/DebugCommands.java
package com.lidao.moran.systems.commands;

import com.lidao.moran.dimensions.DimensionRegistry;
import com.lidao.moran.dimensions.base.BiomeDistributionManager;
import com.lidao.moran.dimensions.base.TerrainGenerator;
import com.lidao.moran.dimensions.peach_blossom.PeachBlossomDimension;
import com.lidao.moran.systems.teleport.DimensionTeleportManager;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

import static net.minecraft.server.command.CommandManager.literal;

public class DebugCommands {

    public static void initialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            // 维度调试命令
            dispatcher.register(literal("mdebug")
                    .then(literal("dimensions")
                            .executes(context -> {
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                if (player != null) {
                                    showDimensionList(player);
                                }
                                return 1;
                            })
                    )
                    .then(literal("teleport")
                            .then(literal("peach_blossom")
                                    .executes(context -> {
                                        ServerPlayerEntity player = context.getSource().getPlayer();
                                        if (player != null) {
                                            teleportToPeachBlossom(player);
                                        }
                                        return 1;
                                    })
                            )
                    )
                    .then(literal("config")
                            .executes(context -> {
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                if (player != null) {
                                    showConfigInfo(player);
                                }
                                return 1;
                            })
                    )
                    .then(literal("terrain")
                            .then(literal("test_height")
                                    .executes(context -> {
                                        ServerPlayerEntity player = context.getSource().getPlayer();
                                        if (player != null) {
                                            testTerrainGeneration(player);
                                        }
                                        return 1;
                                    })
                            )
                    )
            );
        });

        System.out.println("✅ 调试命令系统初始化完成");
    }

    private static void showDimensionList(ServerPlayerEntity player) {
        player.sendMessage(Text.literal("§e=== 墨世界维度列表 ==="), false);

        // 获取所有已注册的维度ID
        List<String> dimensionOrder = new ArrayList<>(DimensionRegistry.getRegisteredDimensionIds());

        for (String dimId : dimensionOrder) {
            player.sendMessage(Text.literal("§6- " + dimId), false);
        }

        player.sendMessage(Text.literal("§e总计: " + dimensionOrder.size() + " 个维度"), false);
    }

    private static void teleportToPeachBlossom(ServerPlayerEntity player) {
        boolean success = DimensionTeleportManager.travelToDimension(player, "peach_blossom");
        player.sendMessage(Text.literal(success ?
                "§a传送到桃花源成功！" : "§c传送失败！"), false);
    }

    private static void showConfigInfo(ServerPlayerEntity player) {
        player.sendMessage(Text.literal("§e=== 模组配置 ==="), false);
        player.sendMessage(Text.literal("§6竹筏传送时间: " +
                com.lidao.moran.core.config.ModConfig.Teleport.RAFT_STATIONARY_SECONDS + "秒"), false);
        player.sendMessage(Text.literal("§6启用桃花源: " +
                com.lidao.moran.core.config.ModConfig.Dimensions.ENABLE_PEACH_BLOSSOM), false);
    }

    private static void testTerrainGeneration(ServerPlayerEntity player) {
        int x = (int) player.getX();
        int z = (int) player.getZ();

        // 获取桃花源维度实例
        PeachBlossomDimension dimension = (PeachBlossomDimension)
                DimensionRegistry.getDimension("peach_blossom");

        if (dimension != null && dimension.getTerrainGenerator() != null) {
            TerrainGenerator terrain = dimension.getTerrainGenerator();
            BiomeDistributionManager biomeManager = dimension.getBiomeManager();

            // 测试各种生物群系的权重
            double peachWeight = biomeManager.calculateBiomeWeight(x, z, "peach_forest");
            double bambooWeight = biomeManager.calculateBiomeWeight(x, z, "bamboo_forest");
            double farmlandWeight = biomeManager.calculateBiomeWeight(x, z, "farmland");
            double hillsWeight = biomeManager.calculateBiomeWeight(x, z, "hills");

            // 计算地形高度
            double baseHeight = terrain.generateTerrainHeight(x, z, peachWeight);
            double finalHeight = terrain.generateWaterFeatures(x, z, baseHeight);

            // 显示详细的地形信息
            player.sendMessage(Text.literal("§e=== 地形生成测试 ==="), false);
            player.sendMessage(Text.literal("§6坐标: " + x + ", " + z), false);
            player.sendMessage(Text.literal("§6桃花林权重: " + String.format("%.2f", peachWeight)), false);
            player.sendMessage(Text.literal("§6竹林权重: " + String.format("%.2f", bambooWeight)), false);
            player.sendMessage(Text.literal("§6农田权重: " + String.format("%.2f", farmlandWeight)), false);
            player.sendMessage(Text.literal("§6丘陵权重: " + String.format("%.2f", hillsWeight)), false);
            player.sendMessage(Text.literal("§6基础高度: " + String.format("%.2f", baseHeight)), false);
            player.sendMessage(Text.literal("§6最终高度: " + String.format("%.2f", finalHeight)), false);

            // 确定主导生物群系
            String dominantBiome = determineDominantBiome(peachWeight, bambooWeight, farmlandWeight, hillsWeight);
            player.sendMessage(Text.literal("§a主导生物群系: " + dominantBiome), false);

        } else {
            player.sendMessage(Text.literal("§c桃花源维度地形系统尚未初始化"), false);
            player.sendMessage(Text.literal("§c请确保桃花源维度已正确注册并初始化"), false);
        }
    }

    /**
     * 根据权重确定主导生物群系
     */
    private static String determineDominantBiome(double peach, double bamboo, double farmland, double hills) {
        double maxWeight = Math.max(Math.max(peach, bamboo), Math.max(farmland, hills));

        if (maxWeight == peach) return "妖灼华原 (桃花林)";
        if (maxWeight == bamboo) return "隐竹之界 (竹林)";
        if (maxWeight == farmland) return "千耕平畴 (农田)";
        if (maxWeight == hills) return "叠翠微岚 (丘陵)";

        return "未知区域";
    }
}




