package com.lidao.moran.systems.teleport;

import com.lidao.moran.dimensions.DimensionRegistry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

public class DimensionTeleportManager {

    /**
     * 通用的维度传送方法（现代 API）
     */
    public static boolean travelToDimension(ServerPlayerEntity player, String dimensionId) {
        if (player == null) {
            System.err.println("❌ 传送失败: 玩家为 null");
            return false;
        }

        try {
            // ✅ 使用 RegistryKey 而非 BaseDimension
            var dimensionKey = DimensionRegistry.getDimensionKey(dimensionId);
            if (dimensionKey == null) {
                player.sendMessage(Text.literal("§c维度未注册: " + dimensionId), false);
                return false;
            }

            ServerWorld targetWorld = player.getServer().getWorld(dimensionKey);
            if (targetWorld == null) {
                player.sendMessage(Text.literal("§c维度世界未加载: " + dimensionId), false);
                return false;
            }

            // 与竹筏传送统一使用维度出生点，避免固定 y=100 把玩家送进空中或地下。
            var spawn = targetWorld.getSpawnPos();
            // ✅ 现代传送 API（7 参数）
            player.teleport(
                    targetWorld,
                    spawn.getX() + 0.5,
                    spawn.getY() + 1.0,
                    spawn.getZ() + 0.5,
                    0.0f,         // yaw
                    0.0f          // pitch
            );

            System.out.println("✅ 传送成功: " + player.getEntityName() + " -> " + dimensionId);
            return true;

        } catch (Exception e) {
            System.err.println("❌ 传送异常: " + e.getMessage());
            e.printStackTrace();
            player.sendMessage(Text.literal("§c传送失败: " + e.getMessage()), false);
            return false;
        }
    }

    /**
     * 传送玩家回主世界
     */
    public static boolean returnToOverworld(ServerPlayerEntity player) {
        try {
            ServerWorld overworld = player.getServer().getOverworld();
            if (overworld == null) {
                player.sendMessage(Text.literal("§c主世界未加载"), false);
                return false;
            }

            player.teleport(
                    overworld,
                    overworld.getSpawnPos().getX() + 0.5,
                    overworld.getSpawnPos().getY() + 1,
                    overworld.getSpawnPos().getZ() + 0.5,
                    player.getYaw(),
                    player.getPitch()
            );

            player.sendMessage(Text.literal("§a已返回主世界"), false);
            return true;

        } catch (Exception e) {
            System.err.println("❌ 返回主世界失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
