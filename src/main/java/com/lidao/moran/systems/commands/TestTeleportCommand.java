package com.lidao.moran.systems.commands;

import com.lidao.moran.dimensions.DimensionRegistry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.literal;

/**
 * 测试传送命令 - 直接传送到桃花源维度
 */
public class TestTeleportCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("testpeach")
                .executes(TestTeleportCommand::executeTeleport));
    }
    
    private static int executeTeleport(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFeedback(() -> Text.literal("§c此命令只能由玩家执行"), false);
            return 0;
        }
        
        // 检查维度是否已注册
        if (DimensionRegistry.getDimension("peach_blossom") == null) {
            player.sendMessage(Text.literal("§c桃花源维度尚未注册"), false);
            return 0;
        }
        
        try {
            net.minecraft.server.world.ServerWorld targetWorld = player.getServer()
                    .getWorld(DimensionRegistry.getDimension("peach_blossom").getDimensionKey());
            
            if (targetWorld == null) {
                player.sendMessage(Text.literal("§c桃花源维度尚未加载"), false);
                return 0;
            }
            
            // 执行传送
            net.fabricmc.fabric.api.dimension.v1.FabricDimensions.teleport(
                    player,
                    targetWorld,
                    new net.minecraft.world.TeleportTarget(
                            new net.minecraft.util.math.Vec3d(
                                    targetWorld.getSpawnPos().getX() + 0.5,
                                    targetWorld.getSpawnPos().getY() + 1,
                                    targetWorld.getSpawnPos().getZ() + 0.5
                            ),
                            net.minecraft.util.math.Vec3d.ZERO,
                            player.getYaw(),
                            player.getPitch()
                    )
            );
            
            player.sendMessage(Text.literal("§a已传送到桃花源维度（测试模式）"), false);
            context.getSource().sendFeedback(() -> Text.literal("§a玩家 " + player.getName().getString() + " 已传送到桃花源"), true);
            
            return 1;
            
        } catch (Exception e) {
            player.sendMessage(Text.literal("§c传送失败: " + e.getMessage()), false);
            context.getSource().sendFeedback(() -> Text.literal("§c传送失败: " + e.getMessage()), true);
            return 0;
        }
    }
}