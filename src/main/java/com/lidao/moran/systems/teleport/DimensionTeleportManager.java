// src/main/java/com/lidao/moran/systems/teleport/DimensionTeleportManager.java
package com.lidao.moran.systems.teleport;

import com.lidao.moran.dimensions.DimensionRegistry;
import com.lidao.moran.dimensions.base.BaseDimension;
import net.fabricmc.fabric.api.dimension.v1.FabricDimensions;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;

public class DimensionTeleportManager {

    public static boolean travelToDimension(ServerPlayerEntity player, String dimensionId) {
        BaseDimension targetDimension = DimensionRegistry.getDimension(dimensionId);

        if (targetDimension == null) {
            player.sendMessage(net.minecraft.text.Text.literal("§c未知的维度: " + dimensionId), false);
            return false;
        }

        ServerWorld targetWorld = player.getServer().getWorld(targetDimension.getDimensionKey());
        if (targetWorld == null) {
            player.sendMessage(net.minecraft.text.Text.literal("§c维度尚未加载: " + dimensionId), false);
            return false;
        }

        try {
            // 执行传送
            targetDimension.onPlayerEnter(player);

            // 使用安全的重生点
            Vec3d spawnPos = new Vec3d(
                    targetWorld.getSpawnPos().getX() + 0.5,
                    targetWorld.getSpawnPos().getY() + 1,
                    targetWorld.getSpawnPos().getZ() + 0.5
            );

            FabricDimensions.teleport(
                    player,
                    targetWorld,
                    new TeleportTarget(
                            spawnPos,
                            Vec3d.ZERO,
                            player.getYaw(),
                            player.getPitch()
                    )
            );

            System.out.println("🎯 玩家 " + player.getEntityName() + " 传送到维度: " + dimensionId);
            return true;

        } catch (Exception e) {
            System.out.println("❌ 传送失败: " + e.getMessage());
            e.printStackTrace();
            player.sendMessage(net.minecraft.text.Text.literal("§c传送失败: " + e.getMessage()), false);
            return false;
        }
    }
}