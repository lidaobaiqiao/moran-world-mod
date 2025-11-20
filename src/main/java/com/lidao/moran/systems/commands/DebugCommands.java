// src/main/java/com/lidao/moran/systems/commands/DebugCommands.java
package com.lidao.moran.systems.commands;

import com.lidao.moran.dimensions.DimensionRegistry;
import com.lidao.moran.systems.teleport.DimensionTeleportManager;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

import static net.minecraft.server.command.ChaseCommand.DIMENSIONS;
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
                                    player.sendMessage(Text.literal("§e=== 墨世界维度列表 ==="), false);

                                    for (String dimId : DimensionRegistry.getDimensionOrder()) {
                                        player.sendMessage(Text.literal("§6- " + dimId), false);
                                    }

                                    player.sendMessage(Text.literal("§e总计: " +
                                            DimensionRegistry.getAllDimensions().size() + " 个维度"), false);
                                }
                                return 1;
                            })
                    )
                    .then(literal("teleport")
                            .then(literal("peach_blossom")
                                    .executes(context -> {
                                        ServerPlayerEntity player = context.getSource().getPlayer();
                                        if (player != null) {
                                            boolean success = DimensionTeleportManager.travelToDimension(player, "peach_blossom");
                                            player.sendMessage(Text.literal(success ?
                                                    "§a传送到桃花源成功！" : "§c传送失败！"), false);
                                        }
                                        return 1;
                                    })
                            )
                    )
                    .then(literal("config")
                            .executes(context -> {
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                if (player != null) {
                                    player.sendMessage(Text.literal("§e=== 模组配置 ==="), false);
                                    player.sendMessage(Text.literal("§6竹筏传送时间: " +
                                            com.lidao.moran.core.config.ModConfig.Teleport.RAFT_STATIONARY_SECONDS + "秒"), false);
                                    player.sendMessage(Text.literal("§6启用桃花源: " +
                                            com.lidao.moran.core.config.ModConfig.Dimensions.ENABLE_PEACH_BLOSSOM), false);
                                }
                                return 1;
                            })
                    )
            );
        });

        System.out.println("✅ 调试命令系统初始化完成");
    }
}
