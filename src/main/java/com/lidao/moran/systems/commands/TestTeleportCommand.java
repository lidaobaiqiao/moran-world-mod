package com.lidao.moran.systems.commands;

import com.lidao.moran.dimensions.DimensionRegistry;
import com.lidao.moran.systems.teleport.DimensionTeleportManager;
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
        
        // 使用传送管理器进行传送
        boolean success = DimensionTeleportManager.travelToDimension(player, "peach_blossom");
        
        if (success) {
            context.getSource().sendFeedback(() -> Text.literal("§a玩家 " + player.getName().getString() + " 已传送到桃花源"), true);
            return 1;
        } else {
            context.getSource().sendFeedback(() -> Text.literal("§c传送失败"), true);
            return 0;
        }
    }
}