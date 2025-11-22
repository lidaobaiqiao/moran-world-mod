package com.lidao.moran.systems.teleport;

import com.lidao.moran.dimensions.DimensionRegistry;
import com.lidao.moran.dimensions.base.BaseDimension;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;

public class DimensionTeleportManager {

    /**
     * 通用的维度传送方法
     */
    public static boolean travelToDimension(ServerPlayerEntity player, String dimensionId) {
        if (player == null) {
            System.out.println("❌ 传送失败: 玩家为null");
            return false;
        }

        BaseDimension targetDimension = DimensionRegistry.getDimension(dimensionId);
        if (targetDimension == null) {
            System.out.println("❌ 传送失败: 维度未找到 - " + dimensionId);
            player.sendMessage(net.minecraft.text.Text.literal("§c维度未找到: " + dimensionId), false);
            return false;
        }

        ServerWorld targetWorld = player.getServer().getWorld(targetDimension.getDimensionKey());
        if (targetWorld == null) {
            System.out.println("❌ 传送失败: 维度世界未加载 - " + dimensionId);
            player.sendMessage(net.minecraft.text.Text.literal("§c维度世界未加载"), false);
            return false;
        }

        try {
            // 执行传送
            net.fabricmc.fabric.api.dimension.v1.FabricDimensions.teleport(
                    player,
                    targetWorld,
                    createTeleportTarget(player, targetWorld)
            );

            // 调用维度的玩家进入事件
            targetDimension.onPlayerEnter(player);

            System.out.println("✅ 传送成功: " + player.getEntityName() + " -> " + dimensionId);
            return true;

        } catch (Exception e) {
            System.out.println("❌ 传送异常: " + e.getMessage());
            e.printStackTrace();
            player.sendMessage(net.minecraft.text.Text.literal("§c传送失败: " + e.getMessage()), false);
            return false;
        }
    }

    /**
     * 创建传送目标位置
     */
    private static TeleportTarget createTeleportTarget(ServerPlayerEntity player, ServerWorld targetWorld) {
        // 使用维度的出生点，如果没有则使用默认位置
        Vec3d spawnPos = new Vec3d(
                targetWorld.getSpawnPos().getX() + 0.5,
                targetWorld.getSpawnPos().getY() + 1,
                targetWorld.getSpawnPos().getZ() + 0.5
        );

        // 确保位置安全（不在虚空或墙里）
        spawnPos = ensureSafePosition(targetWorld, spawnPos);

        return new TeleportTarget(
                spawnPos,
                Vec3d.ZERO,
                player.getYaw(),
                player.getPitch()
        );
    }

    /**
     * 确保传送位置安全
     */
    private static Vec3d ensureSafePosition(ServerWorld world, Vec3d originalPos) {
        // 简化实现：直接返回原始位置
        // 在实际应用中，这里应该检查位置是否安全，如果不安全则寻找最近的安全位置
        return originalPos;
    }

    /**
     * 传送玩家回主世界
     */
    public static boolean returnToOverworld(ServerPlayerEntity player) {
        ServerWorld overworld = player.getServer().getOverworld();
        if (overworld == null) {
            return false;
        }

        try {
            // 执行传送
            net.fabricmc.fabric.api.dimension.v1.FabricDimensions.teleport(
                    player,
                    overworld,
                    new TeleportTarget(
                            new Vec3d(
                                    overworld.getSpawnPos().getX() + 0.5,
                                    overworld.getSpawnPos().getY() + 1,
                                    overworld.getSpawnPos().getZ() + 0.5
                            ),
                            Vec3d.ZERO,
                            player.getYaw(),
                            player.getPitch()
                    )
            );

            player.sendMessage(net.minecraft.text.Text.literal("§a已返回主世界"), false);
            return true;

        } catch (Exception e) {
            System.out.println("❌ 返回主世界失败: " + e.getMessage());
            return false;
        }
    }
}