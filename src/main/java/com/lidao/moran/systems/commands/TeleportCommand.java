package com.lidao.moran.systems.commands;

import com.lidao.moran.dimensions.DimensionRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.literal;

public class TeleportCommand {
    public static void initialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            // 传送到桃花源
            dispatcher.register(literal("peach")
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        if (player != null) {
                            teleportToDimension(player, "peach_blossom");
                        }
                        return 1;
                    })
            );
        });
    }

    private static void teleportToDimension(ServerPlayerEntity player, String dimensionId) {
        try {
            var key = DimensionRegistry.getDimensionKey(dimensionId);
            if (key == null) {
                player.sendMessage(Text.literal("§c维度未注册"), false);
                return;
            }

            var dimension = player.getServer().getWorld(key);
            if (dimension == null) {
                player.sendMessage(Text.literal("§c维度未加载"), false);
                return;
            }

            // ✅ 现代传送 API
            player.teleport(
                    dimension,      // ServerWorld
                    0.5,           // x
                    100.0,         // y
                    0.5,           // z
                    0.0f,          // yaw
                    0.0f           // pitch
            );

            player.sendMessage(Text.literal("§a已传送到桃花源"), false);
        } catch (Exception e) {
            player.sendMessage(Text.literal("§c失败: " + e.getMessage()), false);
        }
    }
}