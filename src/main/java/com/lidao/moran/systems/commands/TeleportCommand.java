package com.lidao.moran.systems.commands;

import com.lidao.moran.systems.teleport.DimensionTeleportManager;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.network.ServerPlayerEntity;

import static net.minecraft.server.command.CommandManager.literal;

public class TeleportCommand {
    public static void initialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("peach")
                    // 传送到桃花源
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        if (player != null) {
                            DimensionTeleportManager.travelToDimension(player, "peach_blossom");
                        }
                        return 1;
                    })
                    // 返回主世界
                    .then(literal("exit")
                            .executes(context -> {
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                if (player != null) {
                                    DimensionTeleportManager.returnToOverworld(player);
                                }
                                return 1;
                            }))
            );
        });
    }
}
